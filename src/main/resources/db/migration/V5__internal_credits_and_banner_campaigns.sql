-- Closed non-monetary credits. No provider, VND balance, deposit, withdrawal or order-payment integration.
CREATE TABLE credit_control (id INT NOT NULL PRIMARY KEY, CONSTRAINT chk_credit_control CHECK(id=1)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
INSERT INTO credit_control(id) VALUES(1);
CREATE TABLE credit_accounts (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    account_key VARCHAR(80) NOT NULL UNIQUE,
    owner_id BIGINT NULL UNIQUE,
    available DECIMAL(18,0) NOT NULL DEFAULT 0,
    reserved DECIMAL(18,0) NOT NULL DEFAULT 0,
    CONSTRAINT fk_credit_owner FOREIGN KEY(owner_id) REFERENCES users(id),
    CONSTRAINT chk_credit_balance CHECK(reserved>=0 AND (account_key='TREASURY' OR available>=0)),
    CONSTRAINT chk_credit_kind CHECK((owner_id IS NULL AND account_key='TREASURY' AND reserved=0) OR (owner_id IS NOT NULL AND account_key<>'TREASURY'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE credit_journals (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    actor_id BIGINT NOT NULL,
    request_key VARCHAR(36) NOT NULL,
    fingerprint VARCHAR(64) NOT NULL,
    type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    reference VARCHAR(120) NOT NULL,
    description VARCHAR(1000) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_credit_actor FOREIGN KEY(actor_id) REFERENCES users(id),
    CONSTRAINT uk_credit_request UNIQUE(actor_id,request_key),
    CONSTRAINT chk_credit_type CHECK(type IN ('GRANT','RESERVE','RELEASE','CHARGE')),
    CONSTRAINT chk_credit_status CHECK(status='COMPLETED'),
    INDEX idx_credit_created(created_at,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE credit_entries (
    journal_id VARCHAR(36) NOT NULL,
    account_id BIGINT NOT NULL,
    available_delta DECIMAL(18,0) NOT NULL,
    reserved_delta DECIMAL(18,0) NOT NULL,
    PRIMARY KEY(journal_id,account_id),
    CONSTRAINT fk_credit_entry_journal FOREIGN KEY(journal_id) REFERENCES credit_journals(id),
    CONSTRAINT fk_credit_entry_account FOREIGN KEY(account_id) REFERENCES credit_accounts(id),
    CONSTRAINT chk_credit_entry CHECK(available_delta<>0 OR reserved_delta<>0),
    INDEX idx_credit_account(account_id,journal_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE banner_slots (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(120) NOT NULL UNIQUE,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    revision BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_banner_slot_revision CHECK(revision>=0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE banner_campaigns (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    slot_id BIGINT NOT NULL,
    title VARCHAR(180) NOT NULL,
    bidding_ends_at DATETIME(6) NOT NULL,
    starts_at DATETIME(6) NOT NULL,
    ends_at DATETIME(6) NOT NULL,
    minimum_bid DECIMAL(18,0) NOT NULL,
    status VARCHAR(20) NOT NULL,
    revision BIGINT NOT NULL DEFAULT 0,
    created_by BIGINT NOT NULL,
    request_key VARCHAR(36) NOT NULL,
    request_fingerprint VARCHAR(64) NOT NULL,
    winner_bid_id BIGINT NULL,
    CONSTRAINT fk_banner_slot FOREIGN KEY(slot_id) REFERENCES banner_slots(id),
    CONSTRAINT fk_banner_creator FOREIGN KEY(created_by) REFERENCES users(id),
    CONSTRAINT uk_banner_campaign_request UNIQUE(created_by,request_key),
    CONSTRAINT chk_banner_schedule CHECK(bidding_ends_at<=starts_at AND starts_at<ends_at),
    CONSTRAINT chk_banner_minimum CHECK(minimum_bid BETWEEN 1 AND 1000000),
    CONSTRAINT chk_banner_campaign_revision CHECK(revision>=0),
    CONSTRAINT chk_banner_campaign_status CHECK(status IN ('OPEN','SCHEDULED','EXPIRED','CANCELLED')),
    INDEX idx_banner_campaign_live(status,starts_at,ends_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE banner_bids (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    campaign_id BIGINT NOT NULL,
    seller_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    amount DECIMAL(18,0) NOT NULL,
    status VARCHAR(20) NOT NULL,
    approved BOOLEAN NOT NULL DEFAULT FALSE,
    request_key VARCHAR(36) NOT NULL,
    fingerprint VARCHAR(64) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_banner_bid_campaign FOREIGN KEY(campaign_id) REFERENCES banner_campaigns(id),
    CONSTRAINT fk_banner_bid_seller FOREIGN KEY(seller_id) REFERENCES users(id),
    CONSTRAINT fk_banner_bid_product FOREIGN KEY(product_id) REFERENCES products(id),
    CONSTRAINT uk_banner_bid_request UNIQUE(seller_id,request_key),
    CONSTRAINT uk_banner_bid_seller UNIQUE(campaign_id,seller_id),
    CONSTRAINT uk_banner_bid_parent UNIQUE(id,campaign_id),
    CONSTRAINT chk_banner_bid_amount CHECK(amount BETWEEN 1 AND 1000000),
    CONSTRAINT chk_banner_bid_status CHECK(status IN ('RESERVED','WITHDRAWN','REJECTED','LOST','WON','RELEASED')),
    INDEX idx_banner_bid_rank(campaign_id,status,approved,amount,created_at,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
ALTER TABLE banner_campaigns ADD CONSTRAINT fk_banner_winner FOREIGN KEY(winner_bid_id,id) REFERENCES banner_bids(id,campaign_id);
CREATE TABLE banner_audit (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    campaign_id BIGINT NULL,
    actor_id BIGINT NOT NULL,
    action VARCHAR(40) NOT NULL,
    reason VARCHAR(1000) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_banner_audit_campaign FOREIGN KEY(campaign_id) REFERENCES banner_campaigns(id),
    CONSTRAINT fk_banner_audit_actor FOREIGN KEY(actor_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
