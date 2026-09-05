-- Phase 4: durable event audit sink for card domain events that were
-- previously publish-only (card.payment.posted, card.statement.generated,
-- card.fraud.alert). Consumed by card/kafka/CardEventLogConsumer.java
-- (consumer group card-event-log-group).
--
-- payload stores the raw JSON text of the Kafka record. A JSONB upgrade is
-- possible later via ALTER ... USING payload::jsonb if field-level queries
-- are required.

CREATE TABLE IF NOT EXISTS "card_event_logs" (
    "event_id"     BIGSERIAL PRIMARY KEY,
    "topic"        VARCHAR(80)  NOT NULL,
    "event_type"   VARCHAR(80)  NOT NULL,
    "card_number"  VARCHAR(16),
    "reference_id" VARCHAR(64),
    "payload"      VARCHAR(10000) NOT NULL,
    "created_at"   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updated_at"   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted_at"   TIMESTAMP    DEFAULT NULL
);

CREATE INDEX IF NOT EXISTS "idx_card_event_logs_topic_received"
    ON "card_event_logs" ("topic", "created_at");
CREATE INDEX IF NOT EXISTS "idx_card_event_logs_card_number"
    ON "card_event_logs" ("card_number");

-- At-least-once redelivery dedup: reference_id is a stable per-entity id for
-- the topic (card.payment.posted -> paymentId, card.fraud.alert -> authTxnId).
-- Excluded: card.statement.generated (reference would be billingCycleDay which
-- repeats every month); a full uniqueness would wrongly drop valid events.
CREATE UNIQUE INDEX IF NOT EXISTS "uq_card_event_logs_topic_reference"
    ON "card_event_logs" ("topic", "reference_id")
    WHERE "reference_id" IS NOT NULL AND "topic" <> 'card.statement.generated';
