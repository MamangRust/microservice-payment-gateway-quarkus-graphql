-- Create users table
CREATE TABLE IF NOT EXISTS "users" (
    "id" BIGSERIAL PRIMARY KEY,
    "firstname" VARCHAR(100) NOT NULL,
    "lastname" varchar(100) NOT NULL,
    "username" VARCHAR(100) UNIQUE NOT NULL,
    "email" varchar(100) UNIQUE NOT NULL,
    "password" varchar(255) NOT NULL,
    "created_at" timestamp DEFAULT current_timestamp,
    "updated_at" timestamp DEFAULT current_timestamp,
    "deleted_at" TIMESTAMP DEFAULT NULL
);

-- Create sequences explicitly for Hibernate
CREATE SEQUENCE IF NOT EXISTS users_seq START 1;

-- Create indexes for users
CREATE INDEX "idx_users_email" ON "users" ("email");

CREATE INDEX "idx_users_firstname" ON "users" ("firstname");

CREATE INDEX "idx_users_lastname" ON "users" ("lastname");

CREATE INDEX "idx_users_username" ON "users" ("username");

CREATE INDEX "idx_users_firstname_lastname" ON "users" ("firstname", "lastname");

CREATE INDEX "idx_users_created_at" ON "users" ("created_at");
