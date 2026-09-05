-- Create refresh_tokens table
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES payment_identity.users (id) ON DELETE CASCADE,
    token VARCHAR(1100) NOT NULL UNIQUE,
    expiration TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT current_timestamp,
    updated_at TIMESTAMP DEFAULT current_timestamp,
    deleted_at TIMESTAMP DEFAULT NULL
);

-- Create sequence explicitly for refresh_tokens
CREATE SEQUENCE IF NOT EXISTS refresh_tokens_seq START 1;

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);

CREATE INDEX idx_refresh_tokens_token ON refresh_tokens (token);

CREATE INDEX idx_refresh_tokens_expiration ON refresh_tokens (expiration);
