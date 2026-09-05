-- Create topups table
CREATE TABLE IF NOT EXISTS "topups" (
    "topup_id" BIGSERIAL PRIMARY KEY,
    "topup_no" UUID NOT NULL DEFAULT gen_random_uuid(),
    "card_number" VARCHAR(16) NOT NULL,
    "topup_amount" INT NOT NULL,
    "topup_method" VARCHAR(50) NOT NULL,
    "topup_time" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "status" VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    "created_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "deleted_at" TIMESTAMP DEFAULT NULL
);

-- Create sequences explicitly for Hibernate topup_id if needed
CREATE SEQUENCE IF NOT EXISTS topups_seq START 1;

CREATE INDEX IF NOT EXISTS "idx_topups_topup_no" ON "topups" ("topup_no");
CREATE INDEX IF NOT EXISTS "idx_topups_card_number" ON "topups" ("card_number");
CREATE INDEX IF NOT EXISTS "idx_topups_status" ON "topups" ("status");
CREATE INDEX IF NOT EXISTS "idx_topups_topup_time" ON "topups" ("topup_time");
