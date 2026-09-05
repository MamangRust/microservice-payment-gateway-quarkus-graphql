-- Enable pgcrypto extension for gen_random_uuid()
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Create merchants table
CREATE TABLE IF NOT EXISTS "merchants" (
    "merchant_id" BIGSERIAL PRIMARY KEY,
    "merchant_no" UUID NOT NULL DEFAULT gen_random_uuid(),
    "name" VARCHAR(255) NOT NULL,
    "api_key" VARCHAR(255) UNIQUE NOT NULL,
    "user_id" BIGINT NOT NULL REFERENCES payment_identity.users ("id") ON DELETE CASCADE,
    "status" VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    "created_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "deleted_at" TIMESTAMP DEFAULT NULL
);

-- Create sequences explicitly for Hibernate merchant_id if needed
CREATE SEQUENCE IF NOT EXISTS merchants_seq START 1;

CREATE INDEX IF NOT EXISTS "idx_merchants_api_key" ON "merchants" ("api_key");
CREATE INDEX IF NOT EXISTS "idx_merchants_user_id" ON "merchants" ("user_id");
CREATE INDEX IF NOT EXISTS "idx_merchants_status" ON "merchants" ("status");
CREATE INDEX IF NOT EXISTS "idx_merchants_name" ON "merchants" ("name");
CREATE INDEX IF NOT EXISTS "idx_merchants_user_id_status" ON "merchants" ("user_id", "status");
