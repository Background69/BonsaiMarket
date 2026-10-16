-- Additive only: existing stock, addresses, timestamps and historical orders are preserved.
-- No stock backfill. products.stock already represents the available, unsold quantity (V4).
CREATE INDEX idx_products_public_price ON products (status, price, id);
CREATE INDEX idx_products_public_created ON products (status, created_at, id);

-- Internal, order-scoped idempotency ledger. Never backfill historical orders automatically.
CREATE TABLE inventory_debits (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    restored BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_inventory_debit_order_product UNIQUE (order_id, product_id),
    CONSTRAINT fk_inventory_debit_order FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT fk_inventory_debit_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT chk_inventory_debit_quantity CHECK (quantity BETWEEN 1 AND 1000000),
    INDEX idx_inventory_debit_product (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
