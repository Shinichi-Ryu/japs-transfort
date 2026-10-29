-- Migration: Add brand and product_info columns to products table
-- Existing products keep working; columns are nullable.
-- Run on PostgreSQL: psql -U postgres -d distributor_db -f migration_add_brand_and_product_info.sql

ALTER TABLE products ADD COLUMN IF NOT EXISTS brand VARCHAR(100);
ALTER TABLE products ADD COLUMN IF NOT EXISTS product_info TEXT;
