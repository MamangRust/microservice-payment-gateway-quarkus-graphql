-- Create cards table
CREATE TABLE IF NOT EXISTS "cards" (
    "card_id" BIGSERIAL PRIMARY KEY,
    "user_id" BIGINT NOT NULL REFERENCES payment_identity.users ("id") ON DELETE CASCADE,
    "card_number" VARCHAR(16) UNIQUE NOT NULL,
    "card_type" VARCHAR(50) NOT NULL,
    "expire_date" DATE NOT NULL,
    "cvv" VARCHAR(3) NOT NULL,
    "card_provider" VARCHAR(50) NOT NULL,
    "created_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "deleted_at" TIMESTAMP DEFAULT NULL
);

-- Create sequences explicitly for Hibernate card_id if needed
CREATE SEQUENCE IF NOT EXISTS cards_seq START 1;

CREATE INDEX IF NOT EXISTS "idx_cards_card_number" ON "cards" ("card_number");
CREATE INDEX IF NOT EXISTS "idx_cards_user_id" ON "cards" ("user_id");
CREATE INDEX IF NOT EXISTS "idx_cards_card_type" ON "cards" ("card_type");
CREATE INDEX IF NOT EXISTS "idx_cards_expire_date" ON "cards" ("expire_date");
CREATE INDEX IF NOT EXISTS "idx_cards_user_id_card_type" ON "cards" ("user_id", "card_type");
