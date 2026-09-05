-- Create transactions table
CREATE TABLE IF NOT EXISTS "transactions" (
    "transaction_id" BIGSERIAL PRIMARY KEY,
    "transaction_no" UUID NOT NULL DEFAULT gen_random_uuid(),
    "card_number" VARCHAR(16) NOT NULL,
    "amount" INT NOT NULL,
    "payment_method" VARCHAR(50) NOT NULL,
    "merchant_id" INT NOT NULL,
    "transaction_time" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "status" VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    "created_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "deleted_at" TIMESTAMP DEFAULT NULL
);

-- Create sequences explicitly for Hibernate transaction_id if needed
CREATE SEQUENCE IF NOT EXISTS transactions_seq START 1;

CREATE INDEX IF NOT EXISTS "idx_transactions_transaction_no" ON "transactions" ("transaction_no");
CREATE INDEX IF NOT EXISTS "idx_transactions_card_number" ON "transactions" ("card_number");
CREATE INDEX IF NOT EXISTS "idx_transactions_merchant_id" ON "transactions" ("merchant_id");
CREATE INDEX IF NOT EXISTS "idx_transactions_status" ON "transactions" ("status");
CREATE INDEX IF NOT EXISTS "idx_transactions_transaction_time" ON "transactions" ("transaction_time");
