-- Distributor / Reseller System - PostgreSQL schema
-- Works locally and on Supabase / Neon / Railway / Render Postgres.
-- Run: psql -U postgres -d distributor_db -f schema.sql

CREATE TYPE user_role AS ENUM ('admin', 'staff', 'warehouse', 'reseller');
CREATE TYPE order_status AS ENUM ('pending', 'approved', 'packed', 'shipped', 'delivered', 'cancelled', 'returned');
CREATE TYPE payment_status AS ENUM ('unpaid', 'partial', 'paid', 'refunded');
CREATE TYPE move_type AS ENUM ('in', 'out', 'adjust', 'return');

-- ===== Users & Resellers =====
CREATE TABLE users (
  user_id       SERIAL PRIMARY KEY,
  full_name     VARCHAR(100) NOT NULL,
  email         VARCHAR(120) UNIQUE NOT NULL,
  password_hash TEXT NOT NULL,
  role          user_role NOT NULL DEFAULT 'reseller',
  is_active     BOOLEAN DEFAULT TRUE,
  created_at    TIMESTAMP DEFAULT NOW()
);

CREATE TABLE reseller_tiers (
  tier_id        SERIAL PRIMARY KEY,
  tier_name      VARCHAR(30) UNIQUE NOT NULL,      -- e.g. Bronze, Silver, Gold
  discount_pct   NUMERIC(5,2) DEFAULT 0,
  commission_pct NUMERIC(5,2) DEFAULT 0,
  min_monthly_purchase NUMERIC(12,2) DEFAULT 0
);

CREATE TABLE resellers (
  reseller_id  SERIAL PRIMARY KEY,
  user_id      INT UNIQUE REFERENCES users(user_id) ON DELETE CASCADE,
  business_name VARCHAR(120) NOT NULL,
  contact_no   VARCHAR(20),
  address      TEXT,
  territory    VARCHAR(80),
  tier_id      INT REFERENCES reseller_tiers(tier_id),
  parent_reseller_id INT REFERENCES resellers(reseller_id), -- multi-level / sub-resellers
  credit_limit NUMERIC(12,2) DEFAULT 0,
  balance      NUMERIC(12,2) DEFAULT 0,             -- outstanding payable
  status       VARCHAR(15) DEFAULT 'active',
  created_at   TIMESTAMP DEFAULT NOW()
);

-- ===== Products & Inventory =====
CREATE SEQUENCE IF NOT EXISTS product_sku_seq START 1;

CREATE TABLE categories (
  category_id SERIAL PRIMARY KEY,
  name        VARCHAR(80) UNIQUE NOT NULL
);

CREATE TABLE products (
  product_id  SERIAL PRIMARY KEY,
  sku         VARCHAR(40) UNIQUE NOT NULL,
  name        VARCHAR(150) NOT NULL,
  category_id INT REFERENCES categories(category_id),
  unit        VARCHAR(20) DEFAULT 'pc',
  cost_price  NUMERIC(12,2) NOT NULL,
  srp         NUMERIC(12,2) NOT NULL,               -- suggested retail price
  reorder_level INT DEFAULT 10,
  is_active   BOOLEAN DEFAULT TRUE,
  brand       VARCHAR(100),
  product_info TEXT
);

CREATE TABLE IF NOT EXISTS product_images (
  product_id   INT PRIMARY KEY REFERENCES products(product_id) ON DELETE CASCADE,
  content_type VARCHAR(50) NOT NULL,
  data         BYTEA NOT NULL,
  updated_at   TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE warehouses (
  warehouse_id SERIAL PRIMARY KEY,
  name         VARCHAR(80) NOT NULL,
  location     TEXT
);

CREATE TABLE inventory (
  warehouse_id INT REFERENCES warehouses(warehouse_id),
  product_id   INT REFERENCES products(product_id),
  quantity     INT NOT NULL DEFAULT 0 CHECK (quantity >= 0),
  PRIMARY KEY (warehouse_id, product_id)
);

CREATE TABLE stock_movements (
  movement_id  SERIAL PRIMARY KEY,
  warehouse_id INT REFERENCES warehouses(warehouse_id),
  product_id   INT REFERENCES products(product_id),
  type         move_type NOT NULL,
  quantity     INT NOT NULL,
  reference    VARCHAR(60),                          -- PO no., order no., etc.
  created_by   INT REFERENCES users(user_id),
  created_at   TIMESTAMP DEFAULT NOW()
);

-- ===== Suppliers (distributor buys from these) =====
CREATE TABLE suppliers (
  supplier_id SERIAL PRIMARY KEY,
  name        VARCHAR(120) NOT NULL,
  contact_no  VARCHAR(20),
  address     TEXT
);

CREATE TABLE purchase_orders (
  po_id       SERIAL PRIMARY KEY,
  supplier_id INT REFERENCES suppliers(supplier_id),
  warehouse_id INT REFERENCES warehouses(warehouse_id),
  status      VARCHAR(15) DEFAULT 'ordered',        -- ordered / received / cancelled
  total       NUMERIC(12,2) DEFAULT 0,
  created_at  TIMESTAMP DEFAULT NOW(),
  received_at TIMESTAMP
);

CREATE TABLE po_items (
  po_item_id SERIAL PRIMARY KEY,
  po_id      INT REFERENCES purchase_orders(po_id) ON DELETE CASCADE,
  product_id INT REFERENCES products(product_id),
  quantity   INT NOT NULL,
  unit_cost  NUMERIC(12,2) NOT NULL
);

-- ===== Reseller Orders =====
CREATE TABLE orders (
  order_id     SERIAL PRIMARY KEY,
  order_no     VARCHAR(30) UNIQUE NOT NULL,
  reseller_id  INT REFERENCES resellers(reseller_id),
  warehouse_id INT REFERENCES warehouses(warehouse_id),
  status       order_status DEFAULT 'pending',
  payment_status payment_status DEFAULT 'unpaid',
  payment_method VARCHAR(20) NOT NULL DEFAULT 'CREDIT',
  subtotal     NUMERIC(12,2) DEFAULT 0,
  discount     NUMERIC(12,2) DEFAULT 0,
  total        NUMERIC(12,2) DEFAULT 0,
  notes        TEXT,
  cancel_reason  VARCHAR(255),
  cancelled_by   VARCHAR(20),
  cancelled_at   TIMESTAMP,
  created_at   TIMESTAMP DEFAULT NOW(),
  updated_at   TIMESTAMP DEFAULT NOW()
);

CREATE TABLE order_items (
  order_item_id SERIAL PRIMARY KEY,
  order_id   INT REFERENCES orders(order_id) ON DELETE CASCADE,
  product_id INT REFERENCES products(product_id),
  quantity   INT NOT NULL CHECK (quantity > 0),
  unit_price NUMERIC(12,2) NOT NULL,                 -- price after tier discount (snapshot)
  line_total NUMERIC(12,2) NOT NULL
);

CREATE TABLE payments (
  payment_id  SERIAL PRIMARY KEY,
  order_id    INT REFERENCES orders(order_id),
  amount      NUMERIC(12,2) NOT NULL,
  method      VARCHAR(30),                           -- cash, bank, GCash, etc.
  reference_no VARCHAR(60),
  paid_at     TIMESTAMP DEFAULT NOW(),
  received_by INT REFERENCES users(user_id)
);

CREATE TABLE shipments (
  shipment_id SERIAL PRIMARY KEY,
  order_id    INT REFERENCES orders(order_id),
  courier     VARCHAR(60),
  tracking_no VARCHAR(60),
  status      VARCHAR(20) DEFAULT 'preparing',
  shipped_at  TIMESTAMP,
  delivered_at TIMESTAMP
);

CREATE TABLE returns (
  return_id  SERIAL PRIMARY KEY,
  order_id   INT REFERENCES orders(order_id),
  product_id INT REFERENCES products(product_id),
  quantity   INT NOT NULL,
  reason     TEXT,
  created_at TIMESTAMP DEFAULT NOW()
);

-- ===== Commissions =====
CREATE TABLE commissions (
  commission_id SERIAL PRIMARY KEY,
  reseller_id INT REFERENCES resellers(reseller_id),
  order_id    INT REFERENCES orders(order_id),
  amount      NUMERIC(12,2) NOT NULL,
  status      VARCHAR(15) DEFAULT 'pending',         -- pending / paid
  created_at  TIMESTAMP DEFAULT NOW()
);

-- ===== Audit log =====
CREATE TABLE audit_logs (
  log_id     SERIAL PRIMARY KEY,
  user_id    INT REFERENCES users(user_id),
  action     VARCHAR(100) NOT NULL,
  details    TEXT,
  created_at TIMESTAMP DEFAULT NOW()
);

-- ===== Indexes =====
CREATE INDEX idx_orders_reseller ON orders(reseller_id);
CREATE INDEX idx_orders_status   ON orders(status);
CREATE INDEX idx_items_order     ON order_items(order_id);
CREATE INDEX idx_stock_product   ON stock_movements(product_id);
CREATE UNIQUE INDEX IF NOT EXISTS ux_commissions_order ON commissions(order_id);

-- ===== Seed data =====
INSERT INTO reseller_tiers (tier_name, discount_pct, commission_pct, min_monthly_purchase) VALUES
  ('Bronze', 5, 3, 0), ('Silver', 10, 5, 20000), ('Gold', 15, 8, 50000);
INSERT INTO warehouses (name, location) VALUES ('Main Warehouse', 'Head Office');
INSERT INTO categories (name) VALUES ('General');
