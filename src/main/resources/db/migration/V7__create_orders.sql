CREATE TABLE orders (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(40) NOT NULL,
    buyer_user_id BIGINT NOT NULL,
    store_id BIGINT NOT NULL,
    shipping_address_snapshot TEXT NOT NULL,
    shipping_method VARCHAR(50) NOT NULL,
    shipping_fee DECIMAL(15,2) NOT NULL,
    subtotal DECIMAL(15,2) NOT NULL,
    total DECIMAL(15,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    notes TEXT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_orders_code UNIQUE (code),
    INDEX idx_orders_buyer_created (buyer_user_id, created_at),
    INDEX idx_orders_store_status_created (store_id, status, created_at),
    CONSTRAINT fk_orders_buyer FOREIGN KEY (buyer_user_id) REFERENCES users(id),
    CONSTRAINT fk_orders_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT chk_orders_amounts CHECK (shipping_fee >= 0 AND subtotal >= 0 AND total = subtotal + shipping_fee),
    CONSTRAINT chk_orders_status CHECK (status IN ('PENDING', 'CONFIRMED', 'SHIPPING', 'DELIVERED', 'COMPLETED', 'CANCELLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE order_items (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    product_name_snapshot VARCHAR(200) NOT NULL,
    unit_price DECIMAL(15,2) NOT NULL,
    quantity INT NOT NULL,
    line_total DECIMAL(15,2) NOT NULL,
    INDEX idx_order_items_order (order_id),
    INDEX idx_order_items_product (product_id),
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT fk_order_items_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT chk_order_items_amounts CHECK (unit_price >= 0 AND quantity > 0 AND line_total = unit_price * quantity)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
