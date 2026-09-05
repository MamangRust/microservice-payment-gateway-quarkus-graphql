CREATE TABLE IF NOT EXISTS "card_payments" (
    "payment_id" BIGSERIAL PRIMARY KEY,
    "card_number" VARCHAR(16) NOT NULL REFERENCES "cards"("card_number") ON DELETE CASCADE,
    "statement_id" BIGINT REFERENCES "billing_statements"("statement_id"),
    "amount" NUMERIC(19,2) NOT NULL,
    "payment_channel" VARCHAR(20) NOT NULL,
    "reference_id" VARCHAR(64),
    "status" VARCHAR(20) DEFAULT 'PENDING',
    "paid_at" TIMESTAMP,
    "created_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "deleted_at" TIMESTAMP DEFAULT NULL
);

CREATE INDEX IF NOT EXISTS "idx_payment_card_number" ON "card_payments" ("card_number");
CREATE INDEX IF NOT EXISTS "idx_payment_reference" ON "card_payments" ("reference_id");
CREATE INDEX IF NOT EXISTS "idx_payment_statement" ON "card_payments" ("statement_id");
