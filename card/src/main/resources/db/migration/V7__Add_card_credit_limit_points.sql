-- Add credit_limit and points columns to cards table
-- (entity Card has BigDecimal creditLimit/points but V5 did not define them)
ALTER TABLE cards ADD COLUMN IF NOT EXISTS credit_limit NUMERIC(19, 2) DEFAULT 0;
ALTER TABLE cards ADD COLUMN IF NOT EXISTS points NUMERIC(19, 2) DEFAULT 0;
