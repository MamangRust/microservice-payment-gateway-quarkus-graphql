-- Create transfers table
CREATE TABLE IF NOT EXISTS "transfers" (
    "transfer_id" BIGSERIAL PRIMARY KEY,
    "transfer_no" UUID NOT NULL DEFAULT gen_random_uuid(),
    "transfer_from" VARCHAR(16) NOT NULL,
    "transfer_to" VARCHAR(16) NOT NULL,
    "transfer_amount" INT NOT NULL,
    "transfer_time" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "status" VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    "created_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "deleted_at" TIMESTAMP DEFAULT NULL
);

-- Create sequences explicitly for Hibernate transfer_id if needed
CREATE SEQUENCE IF NOT EXISTS transfers_seq START 1;

CREATE INDEX IF NOT EXISTS "idx_transfers_transfer_no" ON "transfers" ("transfer_no");
CREATE INDEX IF NOT EXISTS "idx_transfers_transfer_from" ON "transfers" ("transfer_from");
CREATE INDEX IF NOT EXISTS "idx_transfers_transfer_to" ON "transfers" ("transfer_to");
CREATE INDEX IF NOT EXISTS "idx_transfers_status" ON "transfers" ("status");
CREATE INDEX IF NOT EXISTS "idx_transfers_transfer_time" ON "transfers" ("transfer_time");
