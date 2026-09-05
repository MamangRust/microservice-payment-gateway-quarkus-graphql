CREATE TABLE IF NOT EXISTS "card_rewards" (
    "reward_id" BIGSERIAL PRIMARY KEY,
    "card_number" VARCHAR(16) NOT NULL REFERENCES "cards"("card_number") ON DELETE CASCADE,
    "auth_txn_id" BIGINT REFERENCES "card_auth_transactions"("auth_txn_id"),
    "amount" NUMERIC(19,2) NOT NULL,
    "mcc" VARCHAR(4),
    "points_earned" NUMERIC(19,2) NOT NULL DEFAULT 0,
    "expires_at" TIMESTAMP,
    "redeemed" BOOLEAN DEFAULT FALSE,
    "created_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "deleted_at" TIMESTAMP DEFAULT NULL
);

CREATE INDEX IF NOT EXISTS "idx_rewards_card_number" ON "card_rewards" ("card_number");
CREATE INDEX IF NOT EXISTS "idx_rewards_auth_txn" ON "card_rewards" ("auth_txn_id");
