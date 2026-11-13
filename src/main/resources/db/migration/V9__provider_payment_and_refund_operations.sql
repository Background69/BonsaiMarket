-- Additive provider lifecycle. No historical transaction is inferred to be paid/refunded.
ALTER TABLE transactions ADD COLUMN currency VARCHAR(3) NULL,
    ADD COLUMN gateway_created_date VARCHAR(14) NULL,
    ADD COLUMN expires_at DATETIME(6) NULL,
    ADD COLUMN query_count INT NOT NULL DEFAULT 0,
    ADD COLUMN query_after DATETIME(6) NULL;
CREATE TABLE payment_events (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    transaction_id BIGINT NOT NULL,
    dedup_key VARCHAR(64) NOT NULL UNIQUE,
    source VARCHAR(30) NOT NULL,
    outcome VARCHAR(30) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_payment_event_transaction FOREIGN KEY(transaction_id) REFERENCES transactions(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
ALTER TABLE checkout_quotes ADD COLUMN payment_method VARCHAR(30) NOT NULL DEFAULT 'COD';
ALTER TABLE order_payments DROP CHECK chk_order_payment_amount;
ALTER TABLE order_payments ADD CONSTRAINT chk_order_payment_amount CHECK (
    currency='VND' AND shipping_fee>=0 AND
    ((method='COD' AND fee_payer=2 AND cod_amount>0 AND amount_due=cod_amount+shipping_fee AND status IN ('AWAITING_COLLECTION','CANCELLED','REVIEW_REQUIRED')) OR
     (method='WALLET_SANDBOX' AND fee_payer=1 AND cod_amount=0 AND amount_due>0 AND status IN ('SANDBOX_PAID','SANDBOX_REFUNDED','SANDBOX_PARTIALLY_REFUNDED')) OR
     (method='VNPAY' AND fee_payer=1 AND cod_amount=0 AND amount_due>0 AND status IN ('PENDING','SUCCESS','FAILED','EXPIRED','UNKNOWN','CANCELLED','REFUNDED','PARTIALLY_REFUNDED')))
);
ALTER TABLE order_refunds DROP CHECK chk_refund_method;
ALTER TABLE order_refunds ADD CONSTRAINT chk_refund_method CHECK (
    (method='VNPAY' AND transaction_id IS NOT NULL) OR (method IN ('COD','WALLET_SANDBOX') AND transaction_id IS NULL)
);
ALTER TABLE order_refunds ADD COLUMN provider_request_id VARCHAR(32) NULL UNIQUE,
    ADD COLUMN provider_started_at DATETIME(6) NULL,
    ADD COLUMN query_count INT NOT NULL DEFAULT 0,
    ADD COLUMN query_after DATETIME(6) NULL;
-- Multiple partial refunds share an original debit. Per-request identity and the wallet lock bound their sum.
CREATE INDEX idx_vnd_original ON vnd_wallet_transactions(original_id);
ALTER TABLE vnd_wallet_transactions DROP INDEX uk_vnd_original_type;
