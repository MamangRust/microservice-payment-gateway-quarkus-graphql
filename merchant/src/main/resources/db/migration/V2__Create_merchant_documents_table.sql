-- Create merchant_documents table
CREATE TABLE IF NOT EXISTS "merchant_documents" (
    "document_id" BIGSERIAL PRIMARY KEY,
    "merchant_id" BIGINT NOT NULL REFERENCES "merchants" ("merchant_id") ON DELETE CASCADE,
    "document_type" VARCHAR(50) NOT NULL,
    "document_url" VARCHAR(500) NOT NULL,
    "status" VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    "note" VARCHAR(500),
    "created_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "deleted_at" TIMESTAMP DEFAULT NULL
);

-- Create sequence explicitly for Hibernate document_id if needed
CREATE SEQUENCE IF NOT EXISTS merchant_documents_seq START 1;

CREATE INDEX IF NOT EXISTS "idx_merchant_documents_merchant_id" ON "merchant_documents" ("merchant_id");
CREATE INDEX IF NOT EXISTS "idx_merchant_documents_status" ON "merchant_documents" ("status");
