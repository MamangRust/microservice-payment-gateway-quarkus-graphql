-- payment_merchant sequences: Hibernate PanacheEntity default id generation
-- uses SEQUENCE {table}_SEQ with allocationSize = 50.
ALTER SEQUENCE IF EXISTS merchants_seq INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS merchants_seq INCREMENT BY 50 START WITH 1;

ALTER SEQUENCE IF EXISTS merchant_documents_seq INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS merchant_documents_seq INCREMENT BY 50 START WITH 1;
