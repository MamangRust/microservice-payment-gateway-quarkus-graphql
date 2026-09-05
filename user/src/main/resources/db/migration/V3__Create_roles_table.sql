-- Create roles table
CREATE TABLE IF NOT EXISTS "roles" (
    "id" BIGSERIAL PRIMARY KEY,
    "role_name" VARCHAR(50) UNIQUE NOT NULL,
    "created_at" timestamp DEFAULT current_timestamp,
    "updated_at" timestamp DEFAULT current_timestamp,
    "deleted_at" TIMESTAMP DEFAULT NULL
);

-- Create sequences explicitly for Hibernate
CREATE SEQUENCE IF NOT EXISTS roles_seq START 1;

-- Create indexes for roles
CREATE INDEX "idx_roles_role_name" ON "roles" ("role_name");

CREATE INDEX "idx_roles_created_at" ON "roles" ("created_at");

CREATE INDEX "idx_roles_updated_at" ON "roles" ("updated_at");
