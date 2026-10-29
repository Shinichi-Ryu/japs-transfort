-- Migration: Increase product_info field limit to 3000 characters
-- The column is already TEXT in the current schema (no VARCHAR length limit applies).
-- This migration is a safety net for any environment where the column was previously
-- created as VARCHAR(1000) by Hibernate DDL or an older schema version.
--
-- Run on PostgreSQL: psql -U postgres -d distributor_db -f migration_product_info_3000.sql

-- This is safe to run even if the column is already TEXT; ALTER TYPE on a TEXT column is a no-op.
ALTER TABLE products ALTER COLUMN product_info TYPE TEXT;
