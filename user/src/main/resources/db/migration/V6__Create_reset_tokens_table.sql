-- Create reset_tokens table
CREATE TABLE IF NOT EXISTS reset_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES payment_identity.users (id) ON DELETE CASCADE,
    token VARCHAR(500) NOT NULL UNIQUE,
    expiration TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT current_timestamp,
    updated_at TIMESTAMP DEFAULT current_timestamp,
    deleted_at TIMESTAMP DEFAULT NULL
);

-- Create sequence explicitly for reset_tokens
CREATE SEQUENCE IF NOT EXISTS reset_tokens_seq START 1;

CREATE INDEX idx_reset_tokens_user_id ON reset_tokens (user_id);

CREATE INDEX idx_reset_tokens_token ON reset_tokens (token);

CREATE INDEX idx_reset_tokens_expiration ON reset_tokens (expiration);
