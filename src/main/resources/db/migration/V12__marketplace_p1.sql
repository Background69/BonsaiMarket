-- Additive P1 schema. Existing users, stores, products, orders and auth state remain intact.
ALTER TABLE categories ADD COLUMN revision BIGINT NOT NULL DEFAULT 0;
ALTER TABLE categories ADD COLUMN name_key VARCHAR(180) NULL;
-- Preserve any historical duplicate names. Reserve their canonical name once without deleting rows.
UPDATE categories c JOIN (SELECT MIN(id) AS id FROM categories GROUP BY LOWER(TRIM(name))) canonical ON canonical.id=c.id
SET c.name_key=LOWER(TRIM(c.name));
ALTER TABLE categories ADD CONSTRAINT uk_categories_name_key UNIQUE (name_key);
ALTER TABLE stores ADD COLUMN revision BIGINT NOT NULL DEFAULT 0;

CREATE TABLE seller_applications (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    applicant_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    name VARCHAR(200) NOT NULL,
    description VARCHAR(4000) NOT NULL,
    phone VARCHAR(30) NOT NULL,
    address VARCHAR(500) NOT NULL,
    reason VARCHAR(1000) NOT NULL DEFAULT '',
    processed_by BIGINT NULL,
    submitted_at DATETIME(6) NOT NULL,
    processed_at DATETIME(6) NULL,
    revision BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_applications_applicant UNIQUE (applicant_id),
    CONSTRAINT fk_applications_applicant FOREIGN KEY (applicant_id) REFERENCES users(id),
    CONSTRAINT fk_applications_processor FOREIGN KEY (processed_by) REFERENCES users(id),
    CONSTRAINT chk_application_status CHECK (status IN ('PENDING','APPROVED','REJECTED')),
    INDEX idx_applications_status_time (status,submitted_at,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE marketplace_reports (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    reporter_id BIGINT NOT NULL,
    target_type VARCHAR(20) NOT NULL,
    target_id BIGINT NOT NULL,
    reason VARCHAR(20) NOT NULL,
    description VARCHAR(2000) NOT NULL,
    status VARCHAR(20) NOT NULL,
    resolution VARCHAR(1000) NOT NULL DEFAULT '',
    processed_by BIGINT NULL,
    created_at DATETIME(6) NOT NULL,
    processed_at DATETIME(6) NULL,
    revision BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_reports_reporter FOREIGN KEY (reporter_id) REFERENCES users(id),
    CONSTRAINT fk_reports_processor FOREIGN KEY (processed_by) REFERENCES users(id),
    CONSTRAINT uk_report_target UNIQUE (reporter_id,target_type,target_id),
    CONSTRAINT chk_report_type CHECK (target_type IN ('PRODUCT','STORE')),
    CONSTRAINT chk_report_reason CHECK (reason IN ('MISLEADING','PROHIBITED','FRAUD','OTHER')),
    CONSTRAINT chk_report_status CHECK (status IN ('OPEN','IN_REVIEW','RESOLVED','DISMISSED')),
    INDEX idx_reports_status_time (status,created_at,id),
    INDEX idx_reports_rate (reporter_id,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE product_images (
    product_id BIGINT NOT NULL PRIMARY KEY,
    file_key VARCHAR(50) NOT NULL,
    content_type VARCHAR(30) NOT NULL,
    bytes BIGINT NOT NULL,
    CONSTRAINT fk_images_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT uk_images_file UNIQUE (file_key),
    CONSTRAINT chk_image_bytes CHECK (bytes>0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE marketplace_audit (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    actor_id BIGINT NOT NULL,
    action VARCHAR(40) NOT NULL,
    target_type VARCHAR(30) NOT NULL,
    target_id BIGINT NOT NULL,
    reason VARCHAR(1000) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_audit_actor FOREIGN KEY (actor_id) REFERENCES users(id),
    INDEX idx_marketplace_audit_target (target_type,target_id,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
