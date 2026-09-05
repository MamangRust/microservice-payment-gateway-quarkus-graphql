package com.sanedge.email;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.reactive.ReactiveMailer;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.kafka.client.common.TopicPartition;
import io.vertx.kafka.client.consumer.KafkaConsumer;
import io.vertx.kafka.client.consumer.KafkaConsumerRecord;
import io.vertx.kafka.client.consumer.OffsetAndMetadata;
import io.vertx.kafka.client.producer.KafkaHeader;
import io.vertx.kafka.client.producer.KafkaProducer;
import io.vertx.kafka.client.producer.KafkaProducerRecord;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

/**
 * Email worker (single Kafka consumer). Notifications published by the domain
 * services are consumed here and sent through SMTP.
 *
 * <p>Delivery guarantees (Phase 4 unified retry/DLQ model):
 * <ul>
 *   <li><b>Retry</b> — a failed SMTP send is re-published to
 *       {@code email-service-topic-email-retry} with a backoff delay and an
 *       attempt counter; after {@code kafka.email.max-retries} the record is
 *       moved to the shared {@code email-service-topic-email-dlq} instead of
 *       being lost.</li>
 *   <li><b>Idempotency</b> — {@link EmailIdempotencyGuard} claims
 *       {@code email:idempotency:<event_id>} atomically with a lease before the
 *       send and flips it to {@code SENT} after a successful delivery.</li>
 *   <li><b>Commit</b> — the source offset is committed only after a terminal
 *       outcome (sent, retry published, or DLQ published).</li>
 * </ul>
 */
@ApplicationScoped
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    public static final String RETRY_TOPIC = "email-service-topic-email-retry";
    public static final String DLQ_TOPIC = "email-service-topic-email-dlq";

    private static final List<String> INTERNAL_FIELDS = Arrays.asList(
            "_srcTopic", "_srcPartition", "_srcOffset", "_attempt", "_retryAt", "_reason");

    @Inject
    Vertx vertx;

    @Inject
    ReactiveMailer mailer;

    @Inject
    EmailIdempotencyGuard idempotencyGuard;

    private final LongCounter sentCounter;
    private final LongCounter failedCounter;
    private final LongCounter retriedCounter;
    private final LongCounter dlqCounter;
    private final LongCounter duplicateCounter;
    private final LongCounter invalidCounter;
    private final DoubleHistogram processingDuration;
    private final Tracer tracer;
    private final TextMapPropagator propagator;

    private static final TextMapGetter<Map<String, String>> MAP_GETTER = new TextMapGetter<>() {
        @Override
        public Iterable<String> keys(Map<String, String> carrier) {
            return carrier.keySet();
        }

        @Override
        public String get(Map<String, String> carrier, String key) {
            return carrier != null ? carrier.get(key) : null;
        }
    };

    @Inject
    public EmailService(OpenTelemetry openTelemetry) {
        io.opentelemetry.api.metrics.Meter meter = openTelemetry.getMeter("email-service");
        this.sentCounter = meter.counterBuilder("email_sent_total")
                .setDescription("Total emails successfully sent via SMTP")
                .build();
        this.failedCounter = meter.counterBuilder("email_failed_total")
                .setDescription("Total email send attempts that failed")
                .build();
        this.retriedCounter = meter.counterBuilder("email_retried_total")
                .setDescription("Total emails routed to the retry topic")
                .build();
        this.dlqCounter = meter.counterBuilder("email_dlq_total")
                .setDescription("Total emails moved to the shared dead-letter topic")
                .build();
        this.duplicateCounter = meter.counterBuilder("email_duplicate_total")
                .setDescription("Total duplicate email events skipped")
                .build();
        this.invalidCounter = meter.counterBuilder("email_invalid_event_total")
                .setDescription("Total events rejected for an invalid envelope")
                .build();
        this.processingDuration = meter.histogramBuilder("email_processing_duration_seconds")
                .setDescription("Email record processing duration in seconds")
                .setUnit("s")
                .build();
        // Phase 6: expose per-partition consumer lag. The callback runs on each
        // scrape; failures degrade gracefully (empty series = consumer not assigned).
        meter.gaugeBuilder("kafka_consumer_lag")
                .setDescription("Kafka consumer lag per partition for the email consumer group")
                .buildWithCallback(measurement -> partitionLag.forEach((partition, value) -> measurement.record(
                        value == null ? 0.0 : value.doubleValue(),
                        io.opentelemetry.api.common.Attributes.of(
                                io.opentelemetry.api.common.AttributeKey.stringKey("group"), "email-service-group",
                                io.opentelemetry.api.common.AttributeKey.stringKey("partition"), partition))));
        this.tracer = openTelemetry.getTracer("email-service", "1.0.0");
        this.propagator = openTelemetry.getPropagators().getTextMapPropagator();
    }

    @ConfigProperty(name = "kafka.email.max-retries", defaultValue = "3")
    int maxAttempts;

    @ConfigProperty(name = "kafka.email.retry-backoff-ms", defaultValue = "250")
    long baseBackoffMs;

    @ConfigProperty(name = "kafka.email.lag.poll-ms", defaultValue = "15000")
    long lagPollMs;

    private KafkaConsumer<String, JsonObject> consumer;
    KafkaProducer<String, String> producer;
    private final Map<TopicPartition, CompletableFuture<Void>> partitionTails = new ConcurrentHashMap<>();

    // Phase 6: consumer-lag gauge for the email consumer group (partitionLag is
    // refreshed periodically and exposed as kafka_consumer_lag{group,partition}).
    private final Map<TopicPartition, Long> latestCommittedPosition = new ConcurrentHashMap<>();
    private final Map<String, Long> partitionLag = new ConcurrentHashMap<>();
    private volatile long lagTimerId = -1;

    void onStart(@Observes StartupEvent ev) {
        log.info("Starting Email Service with retry-topic + shared DLQ handling");

        producer = KafkaProducer.create(vertx, producerConfig());

        consumer = KafkaConsumer.create(vertx, consumerConfig());
        consumer.handler(this::handleRecord);

        List<String> topics = Arrays.asList(
                "email-service-topic-auth-register",
                "email-service-topic-auth-forgot-password",
                "email-service-topic-auth-verify-code-success",
                "email-service-topic-saldo-create",
                "email-service-topic-topup-create",
                "email-service-topic-transaction-create",
                "email-service-topic-transfer-create",
                "email-service-topic-withdraw-create",
                "email-service-topic-merchant-create",
                "email-service-topic-merchant-update-status",
                "email-service-topic-merchant-document-create",
                "email-service-topic-merchant-document-update-status");

        consumer.subscribe(new HashSet<>(topics))
                .onSuccess(v -> log.info("Email consumer subscribed to {} topics", topics.size()))
                .onFailure(err -> log.error("Email consumer subscription failed", err));

        lagTimerId = vertx.setPeriodic(Math.max(1000, lagPollMs), ignored -> refreshLag());
    }

    /**
     * Refreshes {@link #partitionLag} for the current assignment: end offset minus
     * the last committed position per partition. Non-blocking; failures only log.
     */
    private void refreshLag() {
        if (consumer == null) {
            return;
        }
        consumer.assignment()
                .onSuccess(partitions -> {
                    if (partitions == null || partitions.isEmpty()) {
                        return;
                    }
                    consumer.endOffsets(partitions)
                            .onSuccess(endOffsets -> {
                                Map<String, Long> lag = new HashMap<>();
                                for (TopicPartition partition : partitions) {
                                    Long end = endOffsets.get(partition);
                                    if (end == null) {
                                        continue;
                                    }
                                    Long position = latestCommittedPosition.get(partition);
                                    long committed = position == null ? 0L : position;
                                    lag.put(partition.getTopic() + "-" + partition.getPartition(),
                                            Math.max(0L, end - committed));
                                }
                                partitionLag.clear();
                                partitionLag.putAll(lag);
                            })
                            .onFailure(err -> log.warn("Failed to read Kafka end offsets for lag metric", err));
                })
                .onFailure(err -> log.warn("Failed to read Kafka assignment for lag metric", err));
    }

    /**
     * Shared by {@link RetryProcessor} so both consumers use the same group
     * settings (manual commits, earliest reset, SASL/TLS from env).
     */
    Map<String, String> consumerConfig() {
        String bootstrap = System.getenv().getOrDefault("KAFKA_BROKERS", "localhost:9092");
        return KafkaSecurityConfig.consumer(bootstrap, "email-service-group");
    }

    private Map<String, String> producerConfig() {
        String bootstrap = System.getenv().getOrDefault("KAFKA_BROKERS", "localhost:9092");
        String acks = System.getenv().getOrDefault("KAFKA_ACKS", KafkaSecurityConfig.DEFAULT_ACKS);
        boolean idempotence = Boolean.parseBoolean(
                System.getenv().getOrDefault("KAFKA_IDEMPOTENCE", "true"));
        return KafkaSecurityConfig.producer(bootstrap, acks, idempotence);
    }

    private void handleRecord(KafkaConsumerRecord<String, JsonObject> record) {
        TopicPartition partition = new TopicPartition(record.topic(), record.partition());
        CompletableFuture<Void> previous = partitionTails.getOrDefault(partition,
                CompletableFuture.completedFuture(null));
        Span span = startConsumeSpan(record.headers(), record.topic(), record.partition(), record.offset());
        CompletableFuture<Void> current;
        try (Scope scope = span.makeCurrent()) {
            current = previous
                    .thenCompose(ignored -> processAndCommit(record)
                            // A temporary SMTP/retry/DLQ publish failure must not advance
                            // the source offset. Retry until a terminal outcome is persisted.
                            .onFailure().retry()
                            .withBackOff(Duration.ofMillis(250), Duration.ofSeconds(5))
                            .indefinitely()
                            .subscribeAsCompletionStage())
                    .whenComplete((ignored, error) -> {
                        if (error != null) {
                            span.setStatus(StatusCode.ERROR, error.getMessage());
                            log.error("❌ Error processing record | topic={} partition={} offset={} error={}",
                                    record.topic(), record.partition(), record.offset(), error.getMessage());
                        }
                        span.end();
                    });
        } catch (Exception error) {
            span.recordException(error);
            span.end();
            log.error("❌ Unhandled error in consumer handler", error);
            return;
        }
        partitionTails.put(partition, current);
    }

    /**
     * Starts a {@code kafka.consume} span parented to the W3C trace context that
     * the producer injected into the record headers. Package-private so
     * {@link RetryProcessor} can reuse it for retry-topic records.
     */
    Span startConsumeSpan(java.util.List<KafkaHeader> headers, String topic, int partition, long offset) {
        Map<String, String> carrier = new HashMap<>();
        if (headers != null) {
            for (KafkaHeader header : headers) {
                if (header.value() != null) {
                    carrier.put(header.key(), header.value().toString());
                }
            }
        }
        Context parent = propagator.extract(Context.current(), carrier, MAP_GETTER);
        return tracer.spanBuilder("kafka.consume")
                .setSpanKind(SpanKind.CONSUMER)
                .setParent(parent)
                .setAttribute("messaging.system", "kafka")
                .setAttribute("messaging.source", topic)
                .setAttribute("messaging.destination.partition", partition)
                .setAttribute("messaging.kafka.offset", offset)
                .setAttribute("messaging.operation", "process")
                .startSpan();
    }

    private void injectTraceparent(KafkaProducerRecord<String, String> record) {
        try {
            Map<String, String> carrier = new HashMap<>();
            propagator.inject(Context.current(), carrier, Map::put);
            String traceparent = carrier.get("traceparent");
            if (traceparent != null) {
                record.addHeader("traceparent", traceparent);
            }
        } catch (Exception e) {
            log.debug("Failed to inject traceparent header: {}", e.getMessage());
        }
    }

    private Uni<Void> processAndCommit(KafkaConsumerRecord<String, JsonObject> record) {
        long startNanos = System.nanoTime();
        return processRecord(record.topic(), record.partition(), record.offset(),
                record.key(), record.value(), 0)
                .chain(ignored -> commit(record))
                .invoke(() -> processingDuration.record((System.nanoTime() - startNanos) / 1_000_000_000.0));
    }

    /**
     * Process one logical notification. Package-private so {@link RetryProcessor}
     * can re-enter with the original source coordinates and the attempt counter.
     */
    Uni<Void> processRecord(String srcTopic, int srcPartition, long srcOffset, String key,
            JsonObject payload, int attempt) {
        if (payload == null || !hasValidEnvelope(payload)) {
            log.warn("⚠️ Invalid email envelope, routing to DLQ | topic={} partition={} offset={}",
                    srcTopic, srcPartition, srcOffset);
            invalidCounter.add(1);
            return routeToDeadLetter(payload == null ? new JsonObject() : payload, key,
                    srcTopic, srcPartition, srcOffset, 0, "invalid_event_envelope");
        }

        String eventId = payload.getString("event_id");
        return idempotencyGuard.claim(eventId)
                .chain(result -> {
                    switch (result) {
                        case DUPLICATE:
                            log.info("⏭️ Duplicate email skipped (event_id={})", eventId);
                            duplicateCounter.add(1);
                            return Uni.createFrom().voidItem();
                        case BUSY:
                            // Another consumer holds the lease — do not commit; the
                            // per-partition retry re-claims once the lease expires.
                            return Uni.createFrom().failure(new IllegalStateException(
                                    "Email idempotency claim busy, retrying event_id=" + eventId));
                        default: // CLAIMED
                            return sendEmail(payload)
                                    .chain(() -> idempotencyGuard.markSent(eventId))
                                    .onFailure().recoverWithUni(error -> idempotencyGuard.release(eventId)
                                            .chain(ignored -> routeToRetryOrDlq(payload, key,
                                                    srcTopic, srcPartition, srcOffset, attempt, error)));
                    }
                });
    }

    private Uni<Void> sendEmail(JsonObject payload) {
        String email = payload.getString("email");
        String subject = payload.getString("subject");
        String body = payload.getString("body");

        return mailer.send(Mail.withHtml(email, subject, body))
                .invoke(() -> {
                    sentCounter.add(1);
                    log.info("✅ Email successfully sent to {}", email);
                });
    }

    private Uni<Void> routeToRetryOrDlq(JsonObject payload, String key, String srcTopic,
            int srcPartition, long srcOffset, int attempt, Throwable error) {
        failedCounter.add(1);
        int nextAttempt = attempt + 1;
        if (nextAttempt >= maxAttempts) {
            return routeToDeadLetter(payload, key, srcTopic, srcPartition, srcOffset, nextAttempt,
                    "max_retries_exceeded");
        }
        retriedCounter.add(1);

        JsonObject retry = payload.copy()
                .put("_srcTopic", srcTopic)
                .put("_srcPartition", srcPartition)
                .put("_srcOffset", srcOffset)
                .put("_attempt", nextAttempt)
                .put("_retryAt", System.currentTimeMillis() + backoffMs(nextAttempt))
                .put("_reason", error.getMessage());

        log.warn("🔁 Email send failed; scheduled retry {} of {} | to={} reason={}",
                nextAttempt, maxAttempts, payload.getString("email"), error.getMessage());
        return sendToTopic(RETRY_TOPIC, key, retry);
    }

    private Uni<Void> routeToDeadLetter(JsonObject payload, String key, String srcTopic,
            int srcPartition, long srcOffset, int attempts, String reason) {
        JsonObject dlq = payload.copy();
        for (String field : INTERNAL_FIELDS) {
            dlq.remove(field);
        }
        dlq.put("_reason", reason)
                .put("_srcTopic", srcTopic)
                .put("_srcPartition", srcPartition)
                .put("_srcOffset", srcOffset)
                .put("_attempts", attempts);

        dlqCounter.add(1);
        log.error("☠️ Email moved to DLQ after {} attempts | to={} topic={} reason={}",
                attempts, payload.getString("email"), srcTopic, reason);
        return sendToTopic(DLQ_TOPIC, key, dlq);
    }

    private Uni<Void> sendToTopic(String topic, String key, JsonObject payload) {
        return Uni.createFrom().emitter(emitter -> {
            KafkaProducerRecord<String, String> record = KafkaProducerRecord.create(topic, key, payload.encode());
            injectTraceparent(record);
            producer.send(record)
                    .onSuccess(metadata -> {
                        log.debug("📤 Sent to topic={} partition={} offset={}",
                                topic, metadata.getPartition(), metadata.getOffset());
                        emitter.complete(null);
                    })
                    .onFailure(err -> {
                        log.error("❌ Failed to publish to topic={}: {}", topic, err.getMessage());
                        emitter.fail(err);
                    });
        });
    }

    private boolean hasValidEnvelope(JsonObject payload) {
        // Phase 2 (event contract): envelope validation, consistent with the
        // Ecommerce consumer (event_id, schema_version=1, event_type).
        return payload != null
                && payload.getString("event_id") != null
                && payload.getInteger("schema_version", 0) == 1
                && payload.getString("event_type") != null
                && payload.getString("email") != null
                && payload.getString("subject") != null
                && payload.getString("body") != null;
    }

    private long backoffMs(int attempt) {
        return Math.min((long) attempt * baseBackoffMs, 300_000L);
    }

    private Uni<Void> commit(KafkaConsumerRecord<String, JsonObject> record) {
        TopicPartition partition = new TopicPartition(record.topic(), record.partition());
        Map<TopicPartition, OffsetAndMetadata> offsets = Map.of(partition,
                new OffsetAndMetadata(record.offset() + 1, null));
        return Uni.createFrom().completionStage(consumer.commit(offsets).toCompletionStage())
                .replaceWithVoid()
                .invoke(() -> {
                    latestCommittedPosition.put(partition, record.offset() + 1);
                    log.debug("Committed email offset topic={}, partition={}, offset={}",
                            record.topic(), record.partition(), record.offset());
                });
    }

    void onStop(@Observes ShutdownEvent ev) {
        partitionTails.clear();
        latestCommittedPosition.clear();
        partitionLag.clear();
        if (lagTimerId >= 0) {
            vertx.cancelTimer(lagTimerId);
        }
        if (consumer != null) {
            consumer.close()
                    .onSuccess(v -> log.info("Email consumer closed"))
                    .onFailure(err -> log.error("Failed to close email consumer", err));
        }
        if (producer != null) {
            producer.close()
                    .onSuccess(v -> log.info("Email producer closed"))
                    .onFailure(err -> log.error("Failed to close email producer", err));
        }
    }
}
