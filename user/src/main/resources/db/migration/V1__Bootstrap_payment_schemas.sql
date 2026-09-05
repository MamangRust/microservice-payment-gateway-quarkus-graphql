-- F1: Bootstrap per-domain schemas + role-level search_path.
--
-- Creates the four domain schemas owned by the payment gateway modules and
-- configures the connecting DB role so unqualified table names (used by
-- native SQL in repositories) resolve across all of them. This is the Quarkus
-- equivalent of "search_path di-set per koneksi service" in the Go design:
-- since the modules share one PostgreSQL database, the search_path is set once
-- per role instead of per connection.
--
-- Must run before any other migration in this schema. The `user` service is
-- the first service to start in deploy ordering.

CREATE SCHEMA IF NOT EXISTS payment_identity;
CREATE SCHEMA IF NOT EXISTS payment_merchant;
CREATE SCHEMA IF NOT EXISTS payment_card;
CREATE SCHEMA IF NOT EXISTS payment_finance;

-- Applies to every new connection made by this role (all payment services).
-- Non-existent schemas in search_path are skipped, so listing all four is safe
-- even before a schema is created. `public` stays last for legacy objects.
ALTER ROLE CURRENT_USER SET search_path = payment_identity, payment_merchant, payment_card, payment_finance, public;
