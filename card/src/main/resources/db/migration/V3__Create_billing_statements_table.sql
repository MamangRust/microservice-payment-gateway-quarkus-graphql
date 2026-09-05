CREATE TABLE IF NOT EXISTS "billing_statements" (
    "statement_id" BIGSERIAL PRIMARY KEY,
    "card_number" VARCHAR(16) NOT NULL REFERENCES "cards"("card_number") ON DELETE CASCADE,
    "billing_cycle_day" INT NOT NULL,
    "opening_balance" NUMERIC(19,2) DEFAULT 0,
    "closing_balance" NUMERIC(19,2) DEFAULT 0,
    "minimum_payment" NUMERIC(19,2) DEFAULT 0,
    "due_date" DATE NOT NULL,
    "fees" NUMERIC(19,2) DEFAULT 0,
    "interest" NUMERIC(19,2) DEFAULT 0,
    "statement_date" DATE NOT NULL,
    "status" VARCHAR(20) DEFAULT 'OPEN',
    "created_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "deleted_at" TIMESTAMP DEFAULT NULL
);

CREATE INDEX IF NOT EXISTS "idx_billing_card_number" ON "billing_statements" ("card_number");
CREATE INDEX IF NOT EXISTS "idx_billing_statement_date" ON "billing_statements" ("statement_date");
CREATE INDEX IF NOT EXISTS "idx_billing_status" ON "billing_statements" ("status");
