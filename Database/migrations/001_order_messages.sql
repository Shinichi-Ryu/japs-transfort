-- Migration: 001_order_messages.sql
-- IMPORTANT: This script must be run BEFORE deploying code that uses it,
-- because ddl-auto=validate does not create tables.

CREATE TABLE IF NOT EXISTS order_messages (
  message_id       SERIAL PRIMARY KEY,
  order_id         INT NOT NULL REFERENCES orders(order_id),
  sender_user_id   INT NOT NULL REFERENCES users(user_id),
  sender_role      VARCHAR(20) NOT NULL,
  body             VARCHAR(3000) NOT NULL,
  created_at       TIMESTAMP NOT NULL DEFAULT NOW(),
  read_by_admin    BOOLEAN NOT NULL DEFAULT FALSE,
  read_by_reseller BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_order_messages_order_created ON order_messages(order_id, created_at);
