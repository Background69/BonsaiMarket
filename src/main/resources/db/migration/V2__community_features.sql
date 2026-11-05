-- Approved 2026-11-05: additive Community upgrade; preserve the applied V1 and dev seed.
ALTER TABLE community_posts ADD COLUMN post_type VARCHAR(20) NOT NULL DEFAULT 'GUIDE';
ALTER TABLE community_posts ADD COLUMN plant_name VARCHAR(180) NOT NULL DEFAULT '';
ALTER TABLE community_posts ADD COLUMN symptoms VARCHAR(1000) NOT NULL DEFAULT '';
ALTER TABLE community_posts ADD COLUMN symptom_duration VARCHAR(180) NOT NULL DEFAULT '';
ALTER TABLE community_posts ADD COLUMN lighting VARCHAR(500) NOT NULL DEFAULT '';
ALTER TABLE community_posts ADD COLUMN watering VARCHAR(500) NOT NULL DEFAULT '';
ALTER TABLE community_posts ADD COLUMN substrate VARCHAR(500) NOT NULL DEFAULT '';
ALTER TABLE community_posts ADD COLUMN attempted_care VARCHAR(1000) NOT NULL DEFAULT '';
ALTER TABLE community_posts ADD COLUMN help_status VARCHAR(20) NOT NULL DEFAULT 'OPEN';
ALTER TABLE community_posts ADD COLUMN accepted_comment_id BIGINT NULL;
ALTER TABLE community_posts ADD COLUMN discussion_locked BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE community_posts ADD COLUMN pinned BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE community_posts ADD COLUMN request_key VARCHAR(36) NULL;
ALTER TABLE community_posts ADD COLUMN request_hash VARCHAR(64) NULL;
ALTER TABLE community_posts ADD CONSTRAINT chk_community_post_type CHECK (post_type IN ('QUESTION','SHOWCASE','GUIDE','PLANT_HELP','ANNOUNCEMENT'));
ALTER TABLE community_posts ADD CONSTRAINT chk_community_help_status CHECK (help_status IN ('OPEN','HELPFUL','RESOLVED'));
ALTER TABLE community_posts ADD CONSTRAINT fk_community_accepted_comment FOREIGN KEY (accepted_comment_id) REFERENCES community_comments(id);
ALTER TABLE community_posts ADD CONSTRAINT uk_community_post_request UNIQUE (author_id,request_key);
ALTER TABLE community_posts ADD INDEX idx_community_type_public (status,post_type,pinned,created_at,id);

ALTER TABLE community_comments ADD COLUMN request_key VARCHAR(36) NULL;
ALTER TABLE community_comments ADD COLUMN request_hash VARCHAR(64) NULL;
ALTER TABLE community_comments ADD CONSTRAINT uk_community_comment_request UNIQUE (author_id,request_key);

CREATE TABLE community_likes (
    user_id BIGINT NOT NULL,
    post_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (user_id,post_id),
    CONSTRAINT fk_community_like_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_community_like_post FOREIGN KEY (post_id) REFERENCES community_posts(id),
    INDEX idx_community_like_post (post_id,user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE community_bookmarks (
    user_id BIGINT NOT NULL,
    post_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (user_id,post_id),
    CONSTRAINT fk_community_bookmark_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_community_bookmark_post FOREIGN KEY (post_id) REFERENCES community_posts(id),
    INDEX idx_community_bookmark_post (post_id,user_id),
    INDEX idx_community_bookmark_user_time (user_id,created_at,post_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE community_product_links (
    post_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    PRIMARY KEY (post_id,product_id),
    CONSTRAINT fk_community_product_post FOREIGN KEY (post_id) REFERENCES community_posts(id),
    CONSTRAINT fk_community_product_product FOREIGN KEY (product_id) REFERENCES products(id),
    INDEX idx_community_product_product (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE community_images (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    post_id BIGINT NOT NULL,
    file_key VARCHAR(50) NOT NULL,
    bytes BIGINT NOT NULL,
    request_key VARCHAR(36) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_community_image_post FOREIGN KEY (post_id) REFERENCES community_posts(id),
    CONSTRAINT uk_community_image_file UNIQUE (file_key),
    CONSTRAINT uk_community_image_request UNIQUE (post_id,request_key),
    CONSTRAINT chk_community_image_bytes CHECK (bytes BETWEEN 1 AND 5242880),
    INDEX idx_community_image_post (post_id,created_at,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE marketplace_reports DROP CHECK chk_report_type;
ALTER TABLE marketplace_reports ADD CONSTRAINT chk_report_type CHECK (target_type IN ('PRODUCT','STORE','COMMUNITY_POST','COMMUNITY_COMMENT'));
ALTER TABLE marketplace_reports DROP CHECK chk_report_reason;
ALTER TABLE marketplace_reports ADD CONSTRAINT chk_report_reason CHECK (reason IN ('MISLEADING','PROHIBITED','FRAUD','OTHER','SPAM','HARASSMENT'));
ALTER TABLE order_notification_outbox DROP CHECK chk_notification_target;
ALTER TABLE order_notification_outbox ADD CONSTRAINT chk_notification_target CHECK (order_id IS NOT NULL OR event_type IN ('PRODUCT_QUESTION_ASKED','PRODUCT_QUESTION_ANSWERED','PRODUCT_QUESTION_REPLIED','PRODUCT_QUESTION_MODERATED','COMMUNITY_COMMENTED','COMMUNITY_ACCEPTED','COMMUNITY_MODERATED'));
