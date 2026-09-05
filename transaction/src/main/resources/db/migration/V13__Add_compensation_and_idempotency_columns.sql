-- Add idempotency + compensation columns to financial movement tables.
-- The entities (Topup/Transaction/Transfer/Withdraw) define these fields but
-- the original CREATE TABLE migrations (V13-V18) omitted them.
-- Columns intentionally nullable to satisfy Hibernate schema validation
-- (entities use plain public fields => nullable by default).

-- topups
ALTER TABLE topups ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(255);
ALTER TABLE topups ADD COLUMN IF NOT EXISTS request_fingerprint VARCHAR(255);
ALTER TABLE topups ADD COLUMN IF NOT EXISTS compensation_required_at TIMESTAMP;
ALTER TABLE topups ADD COLUMN IF NOT EXISTS compensation_attempts INTEGER DEFAULT 0;
ALTER TABLE topups ADD COLUMN IF NOT EXISTS last_failure_reason VARCHAR(500);
ALTER TABLE topups ADD COLUMN IF NOT EXISTS compensation_claimed_at TIMESTAMP;
ALTER TABLE topups ADD COLUMN IF NOT EXISTS compensation_claimed_by VARCHAR(100);
ALTER TABLE topups ADD COLUMN IF NOT EXISTS compensation_claim_token VARCHAR(255);
ALTER TABLE topups ADD COLUMN IF NOT EXISTS compensation_lease_until TIMESTAMP;
ALTER TABLE topups ADD COLUMN IF NOT EXISTS compensation_next_attempt_at TIMESTAMP;
ALTER TABLE topups ADD COLUMN IF NOT EXISTS compensation_leg_a_card VARCHAR(50);
ALTER TABLE topups ADD COLUMN IF NOT EXISTS compensation_leg_a_delta INTEGER;
ALTER TABLE topups ADD COLUMN IF NOT EXISTS compensation_leg_a_applied BOOLEAN DEFAULT FALSE;
ALTER TABLE topups ADD COLUMN IF NOT EXISTS compensation_leg_b_card VARCHAR(50);
ALTER TABLE topups ADD COLUMN IF NOT EXISTS compensation_leg_b_delta INTEGER;
ALTER TABLE topups ADD COLUMN IF NOT EXISTS compensation_leg_b_applied BOOLEAN DEFAULT FALSE;

-- transactions
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(255);
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS request_fingerprint VARCHAR(255);
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS compensation_required_at TIMESTAMP;
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS compensation_attempts INTEGER DEFAULT 0;
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS last_failure_reason VARCHAR(500);
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS compensation_claimed_at TIMESTAMP;
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS compensation_claimed_by VARCHAR(100);
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS compensation_claim_token VARCHAR(255);
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS compensation_lease_until TIMESTAMP;
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS compensation_next_attempt_at TIMESTAMP;
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS compensation_leg_a_card VARCHAR(50);
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS compensation_leg_a_delta INTEGER;
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS compensation_leg_a_applied BOOLEAN DEFAULT FALSE;
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS compensation_leg_b_card VARCHAR(50);
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS compensation_leg_b_delta INTEGER;
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS compensation_leg_b_applied BOOLEAN DEFAULT FALSE;

-- transfers
ALTER TABLE transfers ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(255);
ALTER TABLE transfers ADD COLUMN IF NOT EXISTS request_fingerprint VARCHAR(255);
ALTER TABLE transfers ADD COLUMN IF NOT EXISTS compensation_required_at TIMESTAMP;
ALTER TABLE transfers ADD COLUMN IF NOT EXISTS compensation_attempts INTEGER DEFAULT 0;
ALTER TABLE transfers ADD COLUMN IF NOT EXISTS last_failure_reason VARCHAR(500);
ALTER TABLE transfers ADD COLUMN IF NOT EXISTS compensation_claimed_at TIMESTAMP;
ALTER TABLE transfers ADD COLUMN IF NOT EXISTS compensation_claimed_by VARCHAR(100);
ALTER TABLE transfers ADD COLUMN IF NOT EXISTS compensation_claim_token VARCHAR(255);
ALTER TABLE transfers ADD COLUMN IF NOT EXISTS compensation_lease_until TIMESTAMP;
ALTER TABLE transfers ADD COLUMN IF NOT EXISTS compensation_next_attempt_at TIMESTAMP;
ALTER TABLE transfers ADD COLUMN IF NOT EXISTS compensation_leg_a_card VARCHAR(50);
ALTER TABLE transfers ADD COLUMN IF NOT EXISTS compensation_leg_a_delta INTEGER;
ALTER TABLE transfers ADD COLUMN IF NOT EXISTS compensation_leg_a_applied BOOLEAN DEFAULT FALSE;
ALTER TABLE transfers ADD COLUMN IF NOT EXISTS compensation_leg_b_card VARCHAR(50);
ALTER TABLE transfers ADD COLUMN IF NOT EXISTS compensation_leg_b_delta INTEGER;
ALTER TABLE transfers ADD COLUMN IF NOT EXISTS compensation_leg_b_applied BOOLEAN DEFAULT FALSE;

-- withdraws
ALTER TABLE withdraws ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(255);
ALTER TABLE withdraws ADD COLUMN IF NOT EXISTS request_fingerprint VARCHAR(255);
ALTER TABLE withdraws ADD COLUMN IF NOT EXISTS compensation_required_at TIMESTAMP;
ALTER TABLE withdraws ADD COLUMN IF NOT EXISTS compensation_attempts INTEGER DEFAULT 0;
ALTER TABLE withdraws ADD COLUMN IF NOT EXISTS last_failure_reason VARCHAR(500);
ALTER TABLE withdraws ADD COLUMN IF NOT EXISTS compensation_claimed_at TIMESTAMP;
ALTER TABLE withdraws ADD COLUMN IF NOT EXISTS compensation_claimed_by VARCHAR(100);
ALTER TABLE withdraws ADD COLUMN IF NOT EXISTS compensation_claim_token VARCHAR(255);
ALTER TABLE withdraws ADD COLUMN IF NOT EXISTS compensation_lease_until TIMESTAMP;
ALTER TABLE withdraws ADD COLUMN IF NOT EXISTS compensation_next_attempt_at TIMESTAMP;
ALTER TABLE withdraws ADD COLUMN IF NOT EXISTS compensation_leg_a_card VARCHAR(50);
ALTER TABLE withdraws ADD COLUMN IF NOT EXISTS compensation_leg_a_delta INTEGER;
ALTER TABLE withdraws ADD COLUMN IF NOT EXISTS compensation_leg_a_applied BOOLEAN DEFAULT FALSE;
ALTER TABLE withdraws ADD COLUMN IF NOT EXISTS compensation_leg_b_card VARCHAR(50);
ALTER TABLE withdraws ADD COLUMN IF NOT EXISTS compensation_leg_b_delta INTEGER;
ALTER TABLE withdraws ADD COLUMN IF NOT EXISTS compensation_leg_b_applied BOOLEAN DEFAULT FALSE;

-- Unique indexes to enforce idempotency at the database level
CREATE UNIQUE INDEX IF NOT EXISTS idx_topups_idempotency_key ON topups (idempotency_key);
CREATE UNIQUE INDEX IF NOT EXISTS idx_transactions_idempotency_key ON transactions (idempotency_key);
CREATE UNIQUE INDEX IF NOT EXISTS idx_transfers_idempotency_key ON transfers (idempotency_key);
CREATE UNIQUE INDEX IF NOT EXISTS idx_withdraws_idempotency_key ON withdraws (idempotency_key);
