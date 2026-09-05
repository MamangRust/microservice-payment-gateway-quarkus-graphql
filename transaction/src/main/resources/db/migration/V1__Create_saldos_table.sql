-- Create saldos table
CREATE TABLE IF NOT EXISTS "saldos" (
    "saldo_id" BIGSERIAL PRIMARY KEY,
    "card_number" VARCHAR(16) NOT NULL,
    "total_balance" INT NOT NULL DEFAULT 0,
    "withdraw_amount" INT DEFAULT 0,
    "withdraw_time" TIMESTAMP DEFAULT NULL,
    "created_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "deleted_at" TIMESTAMP DEFAULT NULL
);

-- Create sequences explicitly for Hibernate saldo_id if needed
CREATE SEQUENCE IF NOT EXISTS saldos_seq START 1;

CREATE INDEX IF NOT EXISTS "idx_saldos_card_number" ON "saldos" ("card_number");
