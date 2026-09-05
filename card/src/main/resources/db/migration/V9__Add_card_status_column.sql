-- Keep the cards table aligned with com.sanedge.card.entity.Card.CardStatus.
ALTER TABLE "cards"
    ADD COLUMN IF NOT EXISTS "status" VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';
