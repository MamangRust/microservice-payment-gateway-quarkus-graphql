-- Phase 1 follow-up: persist a canonical business-request fingerprint for idempotency replay checks.
ALTER TABLE "topups"
    ADD COLUMN IF NOT EXISTS "request_fingerprint" VARCHAR(64);

ALTER TABLE "transactions"
    ADD COLUMN IF NOT EXISTS "request_fingerprint" VARCHAR(64);

ALTER TABLE "transfers"
    ADD COLUMN IF NOT EXISTS "request_fingerprint" VARCHAR(64);

ALTER TABLE "withdraws"
    ADD COLUMN IF NOT EXISTS "request_fingerprint" VARCHAR(64);
