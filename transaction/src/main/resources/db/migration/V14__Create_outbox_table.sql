-- Transactional outbox for reliable Kafka event publishing (F3).
-- Producers write the business entity + outbox row in the SAME DB transaction;
-- OutboxPublisher claims PENDING rows (claim token + lease) and relays them to
-- Kafka, marking them SENT or retrying with exponential backoff.
CREATE TABLE IF NOT EXISTS "outbox" (
    "id" BIGSERIAL PRIMARY KEY,
    "domain" VARCHAR(50) NOT NULL,
    "event_id" VARCHAR(100) NOT NULL UNIQUE,
    "topic" VARCHAR(200) NOT NULL,
    "event_key" VARCHAR(200),
    "payload" TEXT NOT NULL,
    "status" VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    "attempts" INT NOT NULL DEFAULT 0,
    "next_attempt_at" TIMESTAMP NULL,
    "claimed_at" TIMESTAMP NULL,
    "claim_token" VARCHAR(100) NULL,
    "last_error" TEXT NULL,
    "created_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "deleted_at" TIMESTAMP NULL
);

CREATE INDEX IF NOT EXISTS "idx_outbox_due"
    ON "outbox" ("status", "domain", "next_attempt_at", "claimed_at");

CREATE INDEX IF NOT EXISTS "idx_outbox_event_id"
    ON "outbox" ("event_id");
