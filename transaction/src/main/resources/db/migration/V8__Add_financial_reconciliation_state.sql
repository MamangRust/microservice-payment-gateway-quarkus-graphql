-- Phase 2: durable reconciliation state for failures after a local financial side effect.
-- This is a saga marker, not a distributed transaction or automatic rollback claim.
-- The active-card uniqueness index is intentionally partial so soft-deleted historical
-- rows do not block a new saldo. Existing duplicate active cards must be reconciled
-- before applying this migration in a non-empty legacy database.
ALTER TABLE "saldos"
    ADD CONSTRAINT "chk_saldos_non_negative_balance" CHECK ("total_balance" >= 0);
CREATE UNIQUE INDEX IF NOT EXISTS "idx_saldos_active_card_number"
    ON "saldos" ("card_number")
    WHERE "deleted_at" IS NULL;

ALTER TABLE "topups"
    ADD COLUMN IF NOT EXISTS "compensation_required_at" TIMESTAMP,
    ADD COLUMN IF NOT EXISTS "compensation_attempts" INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS "last_failure_reason" VARCHAR(500);

ALTER TABLE "transactions"
    ADD COLUMN IF NOT EXISTS "compensation_required_at" TIMESTAMP,
    ADD COLUMN IF NOT EXISTS "compensation_attempts" INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS "last_failure_reason" VARCHAR(500);

ALTER TABLE "transfers"
    ADD COLUMN IF NOT EXISTS "compensation_required_at" TIMESTAMP,
    ADD COLUMN IF NOT EXISTS "compensation_attempts" INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS "last_failure_reason" VARCHAR(500);

ALTER TABLE "withdraws"
    ADD COLUMN IF NOT EXISTS "compensation_required_at" TIMESTAMP,
    ADD COLUMN IF NOT EXISTS "compensation_attempts" INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS "last_failure_reason" VARCHAR(500);

CREATE INDEX IF NOT EXISTS "idx_topups_compensation_required"
    ON "topups" ("status", "compensation_required_at")
    WHERE "compensation_required_at" IS NOT NULL;
CREATE INDEX IF NOT EXISTS "idx_transactions_compensation_required"
    ON "transactions" ("status", "compensation_required_at")
    WHERE "compensation_required_at" IS NOT NULL;
CREATE INDEX IF NOT EXISTS "idx_transfers_compensation_required"
    ON "transfers" ("status", "compensation_required_at")
    WHERE "compensation_required_at" IS NOT NULL;
CREATE INDEX IF NOT EXISTS "idx_withdraws_compensation_required"
    ON "withdraws" ("status", "compensation_required_at")
    WHERE "compensation_required_at" IS NOT NULL;
