-- Transactional outbox for reliable Kafka event publishing (F3).
-- Merchant service owns this schema; only domain='merchant' rows are written here.
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
