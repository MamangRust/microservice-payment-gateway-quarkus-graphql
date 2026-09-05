package com.sanedge.card.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sanedge.card.entity.CardEventLog;
import com.sanedge.card.repository.CardEventLogRepository;

import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.kafka.client.consumer.KafkaConsumerRecord;

@ExtendWith(MockitoExtension.class)
class CardEventLogConsumerTest {

    @Mock
    CardEventLogRepository eventLogRepo;

    @Mock
    Vertx vertx;

    @InjectMocks
    CardEventLogConsumer consumer;

    @BeforeEach
    void setUp() {
        // Lenient: only handleRecord tests actually reach the repository.
        lenient().when(eventLogRepo.appendIfAbsent(anyString(), anyString(), any(), any(), anyString()))
                .thenReturn(Uni.createFrom().item(new CardEventLog()));
    }

    @Test
    void resolveReferenceId_usesPaymentId_forPaymentPosted() {
        JsonObject payload = new JsonObject().put("paymentId", 42L).put("cardNumber", "4111111111111111");
        assertThat(consumer.resolveReferenceId("card.payment.posted", payload)).isEqualTo("42");
    }

    @Test
    void resolveReferenceId_usesAuthTxnId_forFraudAlert() {
        JsonObject payload = new JsonObject().put("authTxnId", 7L).put("cardNumber", "4111111111111111");
        assertThat(consumer.resolveReferenceId("card.fraud.alert", payload)).isEqualTo("7");
    }

    @Test
    void resolveReferenceId_isNull_forStatementGenerated() {
        JsonObject payload = new JsonObject().put("billingCycleDay", 10).put("statementsProcessed", 3);
        assertThat(consumer.resolveReferenceId("card.statement.generated", payload)).isNull();
    }

    @Test
    void resolveReferenceId_isNull_whenIdFieldMissing() {
        JsonObject payload = new JsonObject().put("cardNumber", "4111111111111111");
        assertThat(consumer.resolveReferenceId("card.payment.posted", payload)).isNull();
    }

    @Test
    void resolveCardNumber_readsCamelCaseField() {
        JsonObject payload = new JsonObject().put("cardNumber", "4111111111111111");
        assertThat(consumer.resolveCardNumber(payload)).isEqualTo("4111111111111111");
    }

    @Test
    void handleRecord_persistsPaymentPostedEvent() {
        KafkaConsumerRecord<String, String> record = mock(KafkaConsumerRecord.class);
        when(record.topic()).thenReturn("card.payment.posted");
        when(record.value()).thenReturn(
                "{\"paymentId\":42,\"cardNumber\":\"4111111111111111\",\"amount\":\"10000\",\"status\":\"SUCCESS\"}");

        consumer.handleRecord(record);

        verify(eventLogRepo).appendIfAbsent(
                eq("card.payment.posted"),
                eq("card.payment.posted"),
                eq("4111111111111111"),
                eq("42"),
                anyString());
    }

    @Test
    void handleRecord_doesNotCrash_onMalformedPayload() {
        KafkaConsumerRecord<String, String> record = mock(KafkaConsumerRecord.class);
        when(record.topic()).thenReturn("card.fraud.alert");
        when(record.value()).thenReturn("not-json");

        assertThatCode(() -> consumer.handleRecord(record)).doesNotThrowAnyException();
    }
}
