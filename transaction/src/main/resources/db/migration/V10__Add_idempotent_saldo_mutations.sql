-- Phase 3: every saldo delta mutation has a durable operation key.
-- Replaying the same operation key returns the original outcome without applying
-- the delta a second time.
CREATE TABLE IF NOT EXISTS "saldo_mutation_operations" (
    "operation_key" VARCHAR(160) PRIMARY KEY,
    "card_number" VARCHAR(32) NOT NULL,
    "requested_delta" INTEGER NOT NULL,
    "minimum_balance" INTEGER NOT NULL DEFAULT 0,
    "result_status" VARCHAR(16) NOT NULL,
    "result_balance" INTEGER,
    "failure_reason" VARCHAR(500),
    "created_at" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS "idx_saldo_mutation_operations_card"
    ON "saldo_mutation_operations" ("card_number", "created_at");
CREATE INDEX IF NOT EXISTS "idx_saldo_mutation_operations_status"
    ON "saldo_mutation_operations" ("result_status", "updated_at");
