-- Checkout module only. Existing orders/cart/address schemas and seed data are unchanged.
-- This receipt makes a retried confirmation return the same orders, including after a restart.
CREATE TABLE checkout_attempts (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    buyer_user_id BIGINT NOT NULL,
    request_json TEXT NOT NULL,
    review_json TEXT NOT NULL,
    receipt_json TEXT NULL,
    created_at DATETIME(6) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    INDEX idx_checkout_buyer_created (buyer_user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
