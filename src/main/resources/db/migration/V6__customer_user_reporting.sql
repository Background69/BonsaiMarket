-- Extend the existing report aggregate. Legacy reports and history remain unchanged.
ALTER TABLE marketplace_reports
    ADD COLUMN customer_user_id BIGINT NULL,
    ADD COLUMN context_order_id BIGINT NULL,
    ADD COLUMN request_key VARCHAR(36) NULL,
    ADD COLUMN request_fingerprint VARCHAR(64) NULL,
    ADD CONSTRAINT fk_report_customer FOREIGN KEY (customer_user_id) REFERENCES users(id),
    ADD CONSTRAINT fk_report_context_order FOREIGN KEY (context_order_id) REFERENCES orders(id),
    ADD CONSTRAINT uk_report_request UNIQUE (reporter_id, request_key),
    ADD CONSTRAINT chk_customer_report_context CHECK (
        (target_type = 'USER' AND customer_user_id IS NOT NULL AND customer_user_id = target_id
         AND context_order_id IS NOT NULL AND request_key IS NOT NULL AND request_fingerprint IS NOT NULL)
        OR (target_type <> 'USER' AND customer_user_id IS NULL AND context_order_id IS NULL
         AND request_key IS NULL AND request_fingerprint IS NULL)
    );
ALTER TABLE marketplace_reports DROP CHECK chk_report_type;
ALTER TABLE marketplace_reports ADD CONSTRAINT chk_report_type
    CHECK (target_type IN ('PRODUCT','STORE','USER','COMMUNITY_POST','COMMUNITY_COMMENT'));
