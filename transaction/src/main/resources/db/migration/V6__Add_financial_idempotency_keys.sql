-- Phase 1: optional idempotency keys for backward-compatible financial commands.
-- PostgreSQL allows multiple NULL values in a UNIQUE constraint, so legacy clients
-- that do not send a key remain valid while keyed requests are deduplicated.
ALTER TABLE "topups"
    ADD COLUMN IF NOT EXISTS "idempotency_key" VARCHAR(64);

ALTER TABLE "transactions"
    ADD COLUMN IF NOT EXISTS "idempotency_key" VARCHAR(64);

ALTER TABLE "transfers"
    ADD COLUMN IF NOT EXISTS "idempotency_key" VARCHAR(64);

ALTER TABLE "withdraws"
    ADD COLUMN IF NOT EXISTS "idempotency_key" VARCHAR(64);

CREATE UNIQUE INDEX IF NOT EXISTS "idx_topups_idempotency_key"
    ON "topups" ("idempotency_key")
    WHERE "idempotency_key" IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS "idx_transactions_idempotency_key"
    ON "transactions" ("idempotency_key")
    WHERE "idempotency_key" IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS "idx_transfers_idempotency_key"
    ON "transfers" ("idempotency_key")
    WHERE "idempotency_key" IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS "idx_withdraws_idempotency_key"
    ON "withdraws" ("idempotency_key")
    WHERE "idempotency_key" IS NOT NULL;
