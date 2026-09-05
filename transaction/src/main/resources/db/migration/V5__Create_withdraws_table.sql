-- Create withdraws table
CREATE TABLE IF NOT EXISTS "withdraws" (
    "withdraw_id" BIGSERIAL PRIMARY KEY,
    "withdraw_no" UUID NOT NULL DEFAULT gen_random_uuid(),
    "card_number" VARCHAR(16) NOT NULL,
    "withdraw_amount" INT NOT NULL,
    "withdraw_time" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "status" VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    "created_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "deleted_at" TIMESTAMP DEFAULT NULL
);

-- Create sequences explicitly for Hibernate withdraw_id if needed
CREATE SEQUENCE IF NOT EXISTS withdraws_seq START 1;

CREATE INDEX IF NOT EXISTS "idx_withdraws_withdraw_no" ON "withdraws" ("withdraw_no");
CREATE INDEX IF NOT EXISTS "idx_withdraws_card_number" ON "withdraws" ("card_number");
CREATE INDEX IF NOT EXISTS "idx_withdraws_status" ON "withdraws" ("status");
CREATE INDEX IF NOT EXISTS "idx_withdraws_withdraw_time" ON "withdraws" ("withdraw_time");
