CREATE TABLE IF NOT EXISTS "card_auth_transactions" (
    "auth_txn_id" BIGSERIAL PRIMARY KEY,
    "card_number" VARCHAR(16) NOT NULL REFERENCES "cards"("card_number") ON DELETE CASCADE,
    "merchant_id" INT NOT NULL,
    "amount" NUMERIC(19,2) NOT NULL,
    "currency" VARCHAR(3) DEFAULT 'IDR',
    "pos_entry_mode" VARCHAR(3),
    "mcc" VARCHAR(4),
    "idempotency_key" VARCHAR(64) UNIQUE,
    "risk_score" INT DEFAULT 0,
    "status" VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    "authorized_at" TIMESTAMP,
    "reversed_at" TIMESTAMP,
    "created_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "deleted_at" TIMESTAMP DEFAULT NULL
);

CREATE INDEX IF NOT EXISTS "idx_auth_txn_card_number" ON "card_auth_transactions" ("card_number");
CREATE INDEX IF NOT EXISTS "idx_auth_txn_idempotency" ON "card_auth_transactions" ("idempotency_key");
CREATE INDEX IF NOT EXISTS "idx_auth_txn_status" ON "card_auth_transactions" ("status");
