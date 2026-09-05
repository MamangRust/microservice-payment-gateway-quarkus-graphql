-- Phase 3: compensation must reverse only confirmed saldo effects.
ALTER TABLE "topups"
    ADD COLUMN IF NOT EXISTS "compensation_leg_a_card" VARCHAR(32),
    ADD COLUMN IF NOT EXISTS "compensation_leg_a_delta" INTEGER,
    ADD COLUMN IF NOT EXISTS "compensation_leg_a_applied" BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS "compensation_leg_b_card" VARCHAR(32),
    ADD COLUMN IF NOT EXISTS "compensation_leg_b_delta" INTEGER,
    ADD COLUMN IF NOT EXISTS "compensation_leg_b_applied" BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE "transactions"
    ADD COLUMN IF NOT EXISTS "compensation_leg_a_card" VARCHAR(32),
    ADD COLUMN IF NOT EXISTS "compensation_leg_a_delta" INTEGER,
    ADD COLUMN IF NOT EXISTS "compensation_leg_a_applied" BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS "compensation_leg_b_card" VARCHAR(32),
    ADD COLUMN IF NOT EXISTS "compensation_leg_b_delta" INTEGER,
    ADD COLUMN IF NOT EXISTS "compensation_leg_b_applied" BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE "transfers"
    ADD COLUMN IF NOT EXISTS "compensation_leg_a_card" VARCHAR(32),
    ADD COLUMN IF NOT EXISTS "compensation_leg_a_delta" INTEGER,
    ADD COLUMN IF NOT EXISTS "compensation_leg_a_applied" BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS "compensation_leg_b_card" VARCHAR(32),
    ADD COLUMN IF NOT EXISTS "compensation_leg_b_delta" INTEGER,
    ADD COLUMN IF NOT EXISTS "compensation_leg_b_applied" BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE "withdraws"
    ADD COLUMN IF NOT EXISTS "compensation_leg_a_card" VARCHAR(32),
    ADD COLUMN IF NOT EXISTS "compensation_leg_a_delta" INTEGER,
    ADD COLUMN IF NOT EXISTS "compensation_leg_a_applied" BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS "compensation_leg_b_card" VARCHAR(32),
    ADD COLUMN IF NOT EXISTS "compensation_leg_b_delta" INTEGER,
    ADD COLUMN IF NOT EXISTS "compensation_leg_b_applied" BOOLEAN NOT NULL DEFAULT FALSE;
