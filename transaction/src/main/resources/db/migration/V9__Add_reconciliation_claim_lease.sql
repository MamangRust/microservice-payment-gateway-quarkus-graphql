-- Phase 3: durable reconciliation claim protocol.
-- Claiming is an ownership/lease mechanism only; it does not perform a financial rollback.
-- A worker must claim before invoking an explicitly defined compensation adapter.

ALTER TABLE "topups"
    ADD COLUMN IF NOT EXISTS "compensation_claimed_at" TIMESTAMP,
    ADD COLUMN IF NOT EXISTS "compensation_claimed_by" VARCHAR(100),
    ADD COLUMN IF NOT EXISTS "compensation_claim_token" VARCHAR(100),
    ADD COLUMN IF NOT EXISTS "compensation_lease_until" TIMESTAMP,
    ADD COLUMN IF NOT EXISTS "compensation_next_attempt_at" TIMESTAMP;

ALTER TABLE "transactions"
    ADD COLUMN IF NOT EXISTS "compensation_claimed_at" TIMESTAMP,
    ADD COLUMN IF NOT EXISTS "compensation_claimed_by" VARCHAR(100),
    ADD COLUMN IF NOT EXISTS "compensation_claim_token" VARCHAR(100),
    ADD COLUMN IF NOT EXISTS "compensation_lease_until" TIMESTAMP,
    ADD COLUMN IF NOT EXISTS "compensation_next_attempt_at" TIMESTAMP;

ALTER TABLE "transfers"
    ADD COLUMN IF NOT EXISTS "compensation_claimed_at" TIMESTAMP,
    ADD COLUMN IF NOT EXISTS "compensation_claimed_by" VARCHAR(100),
    ADD COLUMN IF NOT EXISTS "compensation_claim_token" VARCHAR(100),
    ADD COLUMN IF NOT EXISTS "compensation_lease_until" TIMESTAMP,
    ADD COLUMN IF NOT EXISTS "compensation_next_attempt_at" TIMESTAMP;

ALTER TABLE "withdraws"
    ADD COLUMN IF NOT EXISTS "compensation_claimed_at" TIMESTAMP,
    ADD COLUMN IF NOT EXISTS "compensation_claimed_by" VARCHAR(100),
    ADD COLUMN IF NOT EXISTS "compensation_claim_token" VARCHAR(100),
    ADD COLUMN IF NOT EXISTS "compensation_lease_until" TIMESTAMP,
    ADD COLUMN IF NOT EXISTS "compensation_next_attempt_at" TIMESTAMP;

CREATE INDEX IF NOT EXISTS "idx_topups_reconciliation_claim"
    ON "topups" ("status", "compensation_next_attempt_at", "compensation_claimed_at")
    WHERE "status" = 'COMPENSATION_REQUIRED';
CREATE INDEX IF NOT EXISTS "idx_transactions_reconciliation_claim"
    ON "transactions" ("status", "compensation_next_attempt_at", "compensation_claimed_at")
    WHERE "status" = 'COMPENSATION_REQUIRED';
CREATE INDEX IF NOT EXISTS "idx_transfers_reconciliation_claim"
    ON "transfers" ("status", "compensation_next_attempt_at", "compensation_claimed_at")
    WHERE "status" = 'COMPENSATION_REQUIRED';
CREATE INDEX IF NOT EXISTS "idx_withdraws_reconciliation_claim"
    ON "withdraws" ("status", "compensation_next_attempt_at", "compensation_claimed_at")
    WHERE "status" = 'COMPENSATION_REQUIRED';
