-- VND SANDBOX ONLY. No credit conversion, initial grant, provider credentials or real bank data.
CREATE TABLE vnd_wallet_control (id INT NOT NULL PRIMARY KEY) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
INSERT INTO vnd_wallet_control(id) VALUES(1);
CREATE TABLE vnd_wallet_qa_members (
    user_id BIGINT NOT NULL PRIMARY KEY,
    CONSTRAINT fk_vnd_qa_user FOREIGN KEY(user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE vnd_wallet_accounts (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    account_key VARCHAR(80) NOT NULL UNIQUE,
    owner_id BIGINT NULL UNIQUE,
    available BIGINT NOT NULL DEFAULT 0,
    held BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_vnd_account_owner FOREIGN KEY(owner_id) REFERENCES users(id),
    CONSTRAINT chk_vnd_account_nonnegative CHECK (owner_id IS NULL OR (available>=0 AND held>=0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE vnd_wallet_banks (
    owner_id BIGINT NOT NULL PRIMARY KEY,
    provider_reference VARCHAR(80) NOT NULL UNIQUE,
    display_name VARCHAR(80) NOT NULL,
    masked_number VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    CONSTRAINT fk_vnd_bank_owner FOREIGN KEY(owner_id) REFERENCES users(id),
    CONSTRAINT chk_vnd_bank_simulated CHECK(status='SIMULATED')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE vnd_wallet_transactions (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    owner_id BIGINT NOT NULL,
    request_key VARCHAR(80) NOT NULL,
    fingerprint VARCHAR(64) NOT NULL,
    type VARCHAR(24) NOT NULL,
    status VARCHAR(24) NOT NULL,
    amount BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'VND',
    original_id VARCHAR(36) NULL,
    order_id BIGINT NULL,
    provider_reference VARCHAR(80) NULL,
    created_at DATETIME(6) NOT NULL,
    processed_at DATETIME(6) NULL,
    CONSTRAINT uk_vnd_request UNIQUE(owner_id,request_key),
    CONSTRAINT uk_vnd_original_type UNIQUE(original_id,type),
    CONSTRAINT fk_vnd_tx_owner FOREIGN KEY(owner_id) REFERENCES users(id),
    CONSTRAINT fk_vnd_tx_original FOREIGN KEY(original_id) REFERENCES vnd_wallet_transactions(id),
    CONSTRAINT fk_vnd_tx_order FOREIGN KEY(order_id) REFERENCES orders(id),
    CONSTRAINT chk_vnd_tx_type CHECK(type IN ('TOP_UP','WITHDRAWAL','WALLET_PAYMENT','REFUND','REVERSAL')),
    CONSTRAINT chk_vnd_tx_status CHECK(status IN ('PENDING','COMPLETED','FAILED','CANCELLED','REVERSED')),
    CONSTRAINT chk_vnd_tx_amount CHECK(amount>0 AND amount<=10000000 AND currency='VND'),
    INDEX idx_vnd_owner_history(owner_id,created_at,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE vnd_wallet_journals (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    transaction_id VARCHAR(36) NOT NULL,
    phase VARCHAR(24) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_vnd_journal_phase UNIQUE(transaction_id,phase),
    CONSTRAINT fk_vnd_journal_tx FOREIGN KEY(transaction_id) REFERENCES vnd_wallet_transactions(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE vnd_wallet_entries (
    journal_id VARCHAR(36) NOT NULL,
    account_id BIGINT NOT NULL,
    available_delta BIGINT NOT NULL,
    held_delta BIGINT NOT NULL,
    available_after BIGINT NOT NULL,
    held_after BIGINT NOT NULL,
    PRIMARY KEY(journal_id,account_id),
    CONSTRAINT fk_vnd_entry_journal FOREIGN KEY(journal_id) REFERENCES vnd_wallet_journals(id),
    CONSTRAINT fk_vnd_entry_account FOREIGN KEY(account_id) REFERENCES vnd_wallet_accounts(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE vnd_wallet_provider_events (
    transaction_id VARCHAR(36) NOT NULL PRIMARY KEY,
    event_id VARCHAR(36) NOT NULL UNIQUE,
    outcome VARCHAR(20) NOT NULL,
    received_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_vnd_event_tx FOREIGN KEY(transaction_id) REFERENCES vnd_wallet_transactions(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE vnd_wallet_obligations (
    order_id BIGINT NOT NULL PRIMARY KEY,
    seller_id BIGINT NOT NULL,
    subtotal BIGINT NOT NULL,
    shipping_fee BIGINT NOT NULL,
    total BIGINT NOT NULL,
    status VARCHAR(24) NOT NULL,
    CONSTRAINT fk_vnd_obligation_order FOREIGN KEY(order_id) REFERENCES orders(id),
    CONSTRAINT fk_vnd_obligation_seller FOREIGN KEY(seller_id) REFERENCES users(id),
    CONSTRAINT chk_vnd_obligation_amount CHECK(subtotal>0 AND shipping_fee>=0 AND total=subtotal+shipping_fee),
    CONSTRAINT chk_vnd_obligation_status CHECK(status IN ('SANDBOX_HELD','REFUNDED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
ALTER TABLE order_payments DROP CHECK chk_order_payment_cod;
ALTER TABLE order_payments DROP CHECK chk_order_payment_status;
ALTER TABLE order_payments ADD CONSTRAINT chk_order_payment_amount CHECK (
    currency='VND' AND shipping_fee>=0 AND
    ((method='COD' AND fee_payer=2 AND cod_amount>0 AND amount_due=cod_amount+shipping_fee AND status IN ('AWAITING_COLLECTION','CANCELLED','REVIEW_REQUIRED')) OR
     (method='WALLET_SANDBOX' AND fee_payer=1 AND cod_amount=0 AND amount_due>0 AND status IN ('SANDBOX_PAID','SANDBOX_REFUNDED')))
);
-- Database-enforced append-only accounting evidence. Compensations require NEW journals.
CREATE TRIGGER vnd_entries_no_update BEFORE UPDATE ON vnd_wallet_entries FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='VND ledger is append only';
CREATE TRIGGER vnd_entries_no_delete BEFORE DELETE ON vnd_wallet_entries FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='VND ledger is append only';
CREATE TRIGGER vnd_journals_no_update BEFORE UPDATE ON vnd_wallet_journals FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='VND journal is append only';
CREATE TRIGGER vnd_journals_no_delete BEFORE DELETE ON vnd_wallet_journals FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='VND journal is append only';
