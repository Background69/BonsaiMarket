ALTER TABLE stores ADD COLUMN banner_url VARCHAR(500) NULL;
ALTER TABLE stores ADD COLUMN about_shop TEXT NULL;
ALTER TABLE products ADD COLUMN content_revision BIGINT NOT NULL DEFAULT 0;
CREATE INDEX idx_products_category_store_public ON products(category_id, status, store_id, id);
