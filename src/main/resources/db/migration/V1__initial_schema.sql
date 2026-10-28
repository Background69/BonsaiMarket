-- Schema thống nhất BonsaiMarket; chỉ áp dụng trên database mới, rỗng.
-- 36 bảng; giữ đầy đủ ràng buộc và index từ source trước baseline.

CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    phone VARCHAR(30) NULL,
    gender VARCHAR(30) NULL,
    bio TEXT NULL,
    role VARCHAR(20) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT chk_users_role CHECK (role IN ('CUSTOMER', 'SELLER', 'ADMIN')),
    email_verified_at DATETIME(6) NULL,
    auth_version BIGINT NOT NULL DEFAULT 0,
    email_key VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin
        GENERATED ALWAYS AS (LOWER(TRIM(email))) STORED,
    CONSTRAINT uk_users_email_key UNIQUE (email_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE categories (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    slug VARCHAR(180) NOT NULL,
    description TEXT NULL,
    parent_id BIGINT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_categories_slug UNIQUE (slug),
    CONSTRAINT fk_categories_parent FOREIGN KEY (parent_id) REFERENCES categories(id),
    revision BIGINT NOT NULL DEFAULT 0,
    name_key VARCHAR(180) NULL,
    CONSTRAINT uk_categories_name_key UNIQUE (name_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE stores (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    owner_user_id BIGINT NOT NULL,
    name VARCHAR(200) NOT NULL,
    description TEXT NULL,
    logo_url VARCHAR(500) NULL,
    phone VARCHAR(30) NULL,
    address VARCHAR(500) NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_stores_owner UNIQUE (owner_user_id),
    CONSTRAINT fk_stores_owner FOREIGN KEY (owner_user_id) REFERENCES users(id),
    CONSTRAINT chk_stores_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    revision BIGINT NOT NULL DEFAULT 0,
    banner_url VARCHAR(500) NULL,
    about_shop TEXT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE products (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    name VARCHAR(200) NOT NULL,
    slug VARCHAR(220) NOT NULL,
    description TEXT NULL,
    price DECIMAL(15,2) NOT NULL,
    compare_price DECIMAL(15,2) NULL,
    stock INT NOT NULL DEFAULT 0,
    image_url VARCHAR(500) NULL,
    status VARCHAR(20) NOT NULL,
    technical_specs_json TEXT NULL,
    metadata_json TEXT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_products_slug UNIQUE (slug),
    INDEX idx_products_store_status (store_id, status),
    INDEX idx_products_category_status (category_id, status),
    CONSTRAINT fk_products_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories(id),
    CONSTRAINT chk_products_price CHECK (price >= 0 AND (compare_price IS NULL OR compare_price >= 0)),
    CONSTRAINT chk_products_stock CHECK (stock >= 0),
    CONSTRAINT chk_products_status CHECK (status IN ('DRAFT', 'PENDING', 'ACTIVE', 'REJECTED', 'ARCHIVED')),
    INDEX idx_products_public_price (status, price, id),
    INDEX idx_products_public_created (status, created_at, id),
    content_revision BIGINT NOT NULL DEFAULT 0,
    INDEX idx_products_category_store_public (category_id, status, store_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE addresses (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    label VARCHAR(80) NOT NULL,
    recipient_name VARCHAR(150) NOT NULL,
    phone VARCHAR(30) NOT NULL,
    province_name VARCHAR(150) NOT NULL,
    province_id VARCHAR(30) NULL,
    district_name VARCHAR(150) NOT NULL,
    district_id VARCHAR(30) NULL,
    ward_name VARCHAR(150) NOT NULL,
    ward_code VARCHAR(30) NULL,
    address_detail VARCHAR(500) NOT NULL,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    INDEX idx_addresses_user (user_id),
    CONSTRAINT fk_addresses_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE carts (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_carts_user UNIQUE (user_id),
    CONSTRAINT fk_carts_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE cart_items (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    cart_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    CONSTRAINT uk_cart_items_cart_product UNIQUE (cart_id, product_id),
    INDEX idx_cart_items_product (product_id),
    CONSTRAINT fk_cart_items_cart FOREIGN KEY (cart_id) REFERENCES carts(id),
    CONSTRAINT fk_cart_items_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT chk_cart_items_quantity CHECK (quantity > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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

CREATE TABLE reviews (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    rating INT NOT NULL,
    comment TEXT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    INDEX idx_reviews_product (product_id),
    CONSTRAINT fk_reviews_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT fk_reviews_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT chk_reviews_rating CHECK (rating BETWEEN 1 AND 5),
    order_item_id BIGINT NULL,
    INDEX idx_reviews_user (user_id),
    CONSTRAINT uk_reviews_order_item UNIQUE (order_item_id),
    CONSTRAINT fk_reviews_order_item FOREIGN KEY (order_item_id) REFERENCES order_items(id),
    INDEX idx_reviews_product_created (product_id,created_at,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE wishlist_items (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_wishlist_items_user_product UNIQUE (user_id, product_id),
    INDEX idx_wishlist_items_product (product_id),
    CONSTRAINT fk_wishlist_items_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_wishlist_items_product FOREIGN KEY (product_id) REFERENCES products(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE auth_challenges (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    purpose VARCHAR(20) NOT NULL,
    nonce VARCHAR(36) NOT NULL,
    code_mac VARCHAR(64) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    sent_at DATETIME(6) NULL,
    attempts INT NOT NULL DEFAULT 0,
    consumed_at DATETIME(6) NULL,
    CONSTRAINT uk_auth_challenge UNIQUE (user_id, purpose),
    CONSTRAINT fk_auth_challenge_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT chk_auth_challenge_purpose CHECK (purpose IN ('VERIFY_EMAIL', 'RESET_PASSWORD')),
    CONSTRAINT chk_auth_challenge_attempts CHECK (attempts BETWEEN 0 AND 5),
    INDEX idx_auth_challenge_expiry (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE oauth_identities (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    provider VARCHAR(20) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    provider_subject VARCHAR(255) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_oauth_subject UNIQUE (provider, provider_subject),
    CONSTRAINT uk_oauth_user_provider UNIQUE (user_id, provider),
    CONSTRAINT fk_oauth_identity_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT chk_oauth_provider CHECK (provider IN ('google', 'facebook'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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

CREATE TABLE product_questions (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    body VARCHAR(2000) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PUBLIC',
    answer_author_id BIGINT NULL,
    answer_body VARCHAR(4000) NULL,
    answered_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    revision BIGINT NOT NULL DEFAULT 0,
    seed_key VARCHAR(100) NULL,
    CONSTRAINT uk_questions_seed UNIQUE (seed_key),
    CONSTRAINT fk_questions_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT fk_questions_author FOREIGN KEY (author_id) REFERENCES users(id),
    CONSTRAINT fk_questions_answer_author FOREIGN KEY (answer_author_id) REFERENCES users(id),
    CONSTRAINT chk_questions_status CHECK (status IN ('PUBLIC','HIDDEN','REMOVED')),
    CONSTRAINT chk_questions_body CHECK (CHAR_LENGTH(TRIM(body)) BETWEEN 1 AND 2000),
    CONSTRAINT chk_questions_answer CHECK ((answer_author_id IS NULL AND answer_body IS NULL AND answered_at IS NULL) OR (answer_author_id IS NOT NULL AND answer_body IS NOT NULL AND CHAR_LENGTH(TRIM(answer_body)) BETWEEN 1 AND 4000 AND answered_at IS NOT NULL)),
    INDEX idx_questions_public (product_id,status,created_at,id),
    INDEX idx_questions_rate (author_id,created_at),
    INDEX idx_answers_rate (answer_author_id,answered_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE community_posts (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    author_id BIGINT NOT NULL,
    title VARCHAR(180) NOT NULL,
    body VARCHAR(12000) NOT NULL,
    topic VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PUBLIC',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    revision BIGINT NOT NULL DEFAULT 0,
    seed_key VARCHAR(100) NULL,
    CONSTRAINT uk_community_posts_seed UNIQUE (seed_key),
    CONSTRAINT fk_community_posts_author FOREIGN KEY (author_id) REFERENCES users(id),
    CONSTRAINT chk_community_posts_status CHECK (status IN ('PUBLIC','HIDDEN','REMOVED')),
    CONSTRAINT chk_community_posts_topic CHECK (topic IN ('BONSAI','SUCCULENTS','CARE','OTHER')),
    CONSTRAINT chk_community_posts_title CHECK (CHAR_LENGTH(TRIM(title)) BETWEEN 1 AND 180),
    CONSTRAINT chk_community_posts_body CHECK (CHAR_LENGTH(TRIM(body)) BETWEEN 1 AND 12000),
    INDEX idx_community_posts_public (status,created_at,id),
    INDEX idx_community_posts_topic (status,topic,created_at,id),
    INDEX idx_community_posts_rate (author_id,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE community_comments (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    post_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    body VARCHAR(3000) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PUBLIC',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    revision BIGINT NOT NULL DEFAULT 0,
    seed_key VARCHAR(100) NULL,
    CONSTRAINT uk_community_comments_seed UNIQUE (seed_key),
    CONSTRAINT fk_community_comments_post FOREIGN KEY (post_id) REFERENCES community_posts(id),
    CONSTRAINT fk_community_comments_author FOREIGN KEY (author_id) REFERENCES users(id),
    CONSTRAINT chk_community_comments_status CHECK (status IN ('PUBLIC','HIDDEN','REMOVED')),
    CONSTRAINT chk_community_comments_body CHECK (CHAR_LENGTH(TRIM(body)) BETWEEN 1 AND 3000),
    INDEX idx_community_comments_public (post_id,status,created_at,id),
    INDEX idx_community_comments_rate (author_id,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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

CREATE TABLE product_question_replies (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    question_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    body VARCHAR(4000) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_question_replies_question FOREIGN KEY (question_id) REFERENCES product_questions(id),
    CONSTRAINT fk_question_replies_author FOREIGN KEY (author_id) REFERENCES users(id),
    CONSTRAINT chk_question_replies_body CHECK (CHAR_LENGTH(TRIM(body)) BETWEEN 1 AND 4000),
    INDEX idx_question_replies_parent (question_id,created_at,id),
    INDEX idx_question_replies_rate (author_id,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE transactions (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    transaction_code VARCHAR(100) NOT NULL,
    payment_method VARCHAR(30) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    status VARCHAR(30) NOT NULL,
    gateway_transaction_no VARCHAR(100) NULL,
    bank_code VARCHAR(50) NULL,
    card_type VARCHAR(50) NULL,
    response_code VARCHAR(50) NULL,
    pay_date VARCHAR(30) NULL,
    notes TEXT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_transactions_code UNIQUE (transaction_code),
    CONSTRAINT fk_transactions_order FOREIGN KEY (order_id) REFERENCES orders(id),
    INDEX idx_transactions_order (order_id),
    INDEX idx_transactions_status (status),
    CONSTRAINT chk_transactions_amount CHECK (amount >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE store_public_locations (
    store_id BIGINT NOT NULL PRIMARY KEY,
    mode VARCHAR(10) NOT NULL,
    published BOOLEAN NOT NULL DEFAULT FALSE,
    region VARCHAR(120) NOT NULL,
    ward VARCHAR(120) NOT NULL,
    public_address VARCHAR(500) NOT NULL,
    latitude DECIMAL(10,7) NULL,
    longitude DECIMAL(10,7) NULL,
    consent_at DATETIME(6) NULL,
    updated_at DATETIME(6) NOT NULL,
    revision BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_public_location_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT chk_location_mode CHECK (mode IN ('AREA','EXACT')),
    CONSTRAINT chk_location_consent CHECK (published=FALSE OR consent_at IS NOT NULL),
    CONSTRAINT chk_location_coordinates CHECK ((mode='AREA' AND latitude IS NULL AND longitude IS NULL AND public_address='') OR
        (mode='EXACT' AND latitude IS NOT NULL AND longitude IS NOT NULL AND latitude BETWEEN -90 AND 90 AND longitude BETWEEN -180 AND 180 AND NOT (latitude=0 AND longitude=0))),
    INDEX idx_public_locations_visibility (published,store_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE shipping_parcels (
    order_id BIGINT NOT NULL PRIMARY KEY,
    weight_grams INT NOT NULL,
    length_cm INT NOT NULL,
    width_cm INT NOT NULL,
    height_cm INT NOT NULL,
    carrier_accepted_goods BOOLEAN NOT NULL,
    revision BIGINT NOT NULL DEFAULT 0,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_shipping_parcel_order FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT chk_shipping_parcel CHECK (weight_grams BETWEEN 1 AND 19999 AND length_cm BETWEEN 1 AND 200 AND width_cm BETWEEN 1 AND 200 AND height_cm BETWEEN 1 AND 200)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE shipping_quotes (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    buyer_id BIGINT NOT NULL,
    shop_id INT NOT NULL,
    parcel_revision BIGINT NOT NULL,
    service_type INT NOT NULL,
    service_name VARCHAR(120) NOT NULL,
    fee DECIMAL(15,2) NOT NULL,
    order_fingerprint VARCHAR(64) NOT NULL,
    destination_snapshot TEXT NOT NULL,
    pickup_snapshot TEXT NOT NULL,
    parcel_snapshot TEXT NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    selected_at DATETIME(6) NULL,
    CONSTRAINT fk_shipping_quote_order FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT fk_shipping_quote_buyer FOREIGN KEY (buyer_id) REFERENCES users(id),
    CONSTRAINT uk_shipping_quote_order UNIQUE (id,order_id),
    CONSTRAINT chk_shipping_quote_fee CHECK (fee>=0),
    INDEX idx_shipping_quotes_order (order_id,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE shipments (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    quote_id VARCHAR(36) NOT NULL,
    shop_id INT NOT NULL,
    client_reference VARCHAR(50) NOT NULL,
    tracking_code VARCHAR(50) NULL,
    state VARCHAR(30) NOT NULL,
    last_event_at DATETIME(6) NULL,
    last_sync_at DATETIME(6) NULL,
    sync_after DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_shipment_order UNIQUE (order_id),
    CONSTRAINT uk_shipment_reference UNIQUE (client_reference),
    CONSTRAINT uk_shipment_tracking UNIQUE (tracking_code),
    CONSTRAINT fk_shipment_order FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT fk_shipment_quote FOREIGN KEY (quote_id,order_id) REFERENCES shipping_quotes(id,order_id),
    CONSTRAINT chk_shipment_state CHECK (state IN ('CREATING','UNKNOWN','WAITING_PICKUP','PICKED_UP','IN_TRANSIT','OUT_FOR_DELIVERY','DELIVERY_FAILED','DELIVERED','RETURN_REQUESTED','RETURNING','RETURN_FAILED','RETURNED','CANCELLED','EXCEPTION','LOST','DAMAGED','DISPOSED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE shipment_events (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    shipment_id VARCHAR(36) NOT NULL,
    dedup_key VARCHAR(64) NOT NULL,
    carrier_status VARCHAR(40) NOT NULL,
    state VARCHAR(30) NOT NULL,
    event_at DATETIME(6) NOT NULL,
    received_at DATETIME(6) NOT NULL,
    source VARCHAR(12) NOT NULL,
    CONSTRAINT fk_shipment_event FOREIGN KEY (shipment_id) REFERENCES shipments(id),
    CONSTRAINT uk_shipment_event UNIQUE (shipment_id,dedup_key),
    INDEX idx_shipment_events_time (shipment_id,event_at,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE checkout_packages (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
 store_id BIGINT NOT NULL,
 contents TEXT NOT NULL,
 parcel TEXT NOT NULL,
 enabled BOOLEAN NOT NULL,
 revision BIGINT NOT NULL,
 updated_at DATETIME(6) NOT NULL,
 CONSTRAINT fk_checkout_package_store FOREIGN KEY (store_id) REFERENCES stores(id),
 INDEX idx_checkout_package_store (store_id,enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE checkout_quotes (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
 buyer_id BIGINT NOT NULL,
 store_id BIGINT NOT NULL,
 package_id VARCHAR(36) NOT NULL,
 package_revision BIGINT NOT NULL,
 cart_fingerprint VARCHAR(64) NOT NULL,
 address_input TEXT NOT NULL,
 destination_snapshot TEXT NOT NULL,
 pickup_snapshot TEXT NOT NULL,
 parcel_snapshot TEXT NOT NULL,
 shop_id INT NOT NULL,
 service_type INT NOT NULL,
 service_name VARCHAR(120) NOT NULL,
 fee DECIMAL(15,2) NOT NULL,
 subtotal DECIMAL(15,2) NOT NULL,
 expires_at DATETIME(6) NOT NULL,
 created_at DATETIME(6) NOT NULL,
 CONSTRAINT fk_checkout_quote_buyer FOREIGN KEY (buyer_id) REFERENCES users(id),
 CONSTRAINT fk_checkout_quote_store FOREIGN KEY (store_id) REFERENCES stores(id),
 CONSTRAINT fk_checkout_quote_package FOREIGN KEY (package_id) REFERENCES checkout_packages(id),
 CONSTRAINT chk_checkout_quote_amount CHECK (fee>=0 AND subtotal>0),
 INDEX idx_checkout_quote_buyer (buyer_id,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE order_payments (
    order_id BIGINT NOT NULL PRIMARY KEY,
 quote_id VARCHAR(36) NOT NULL UNIQUE,
 method VARCHAR(20) NOT NULL,
 status VARCHAR(30) NOT NULL,
 currency VARCHAR(3) NOT NULL,
 amount_due DECIMAL(15,2) NOT NULL,
 cod_amount DECIMAL(15,2) NOT NULL,
 shipping_fee DECIMAL(15,2) NOT NULL,
 fee_payer INT NOT NULL,
 collection_status VARCHAR(30) NOT NULL,
 settlement_status VARCHAR(30) NOT NULL,
 collected_amount DECIMAL(15,2) NULL,
 settled_amount DECIMAL(15,2) NULL,
 store_name_snapshot VARCHAR(200) NOT NULL,
 created_at DATETIME(6) NOT NULL,
 CONSTRAINT fk_order_payment_order FOREIGN KEY (order_id) REFERENCES orders(id),
 CONSTRAINT fk_order_payment_quote FOREIGN KEY (quote_id) REFERENCES shipping_quotes(id),
 CONSTRAINT chk_order_payment_cod CHECK (method='COD' AND currency='VND' AND fee_payer=2 AND cod_amount>0 AND shipping_fee>=0 AND amount_due=cod_amount+shipping_fee),
 CONSTRAINT chk_order_payment_status CHECK (status IN ('AWAITING_COLLECTION','CANCELLED','REVIEW_REQUIRED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE checkout_requests (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
 buyer_id BIGINT NOT NULL,
 request_key VARCHAR(80) NOT NULL,
 request_hash VARCHAR(64) NOT NULL,
 quote_id VARCHAR(36) NOT NULL UNIQUE,
 order_id BIGINT NOT NULL UNIQUE,
 created_at DATETIME(6) NOT NULL,
 CONSTRAINT uk_checkout_buyer_key UNIQUE (buyer_id,request_key),
 CONSTRAINT fk_checkout_request_buyer FOREIGN KEY (buyer_id) REFERENCES users(id),
 CONSTRAINT fk_checkout_request_quote FOREIGN KEY (quote_id) REFERENCES checkout_quotes(id),
 CONSTRAINT fk_checkout_request_order FOREIGN KEY (order_id) REFERENCES orders(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE order_notification_outbox (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
 order_id BIGINT NOT NULL,
 event_type VARCHAR(40) NOT NULL,
 recipient VARCHAR(255) NOT NULL,
 subject VARCHAR(250) NOT NULL,
 body TEXT NOT NULL,
 dedup_key VARCHAR(160) NOT NULL UNIQUE,
 status VARCHAR(24) NOT NULL,
 attempts INT NOT NULL,
 next_attempt_at DATETIME(6) NOT NULL,
 updated_at DATETIME(6) NOT NULL,
 created_at DATETIME(6) NOT NULL,
 error_code VARCHAR(40) NULL,
 CONSTRAINT fk_notification_order FOREIGN KEY (order_id) REFERENCES orders(id),
 CONSTRAINT chk_notification_attempts CHECK (attempts BETWEEN 0 AND 8),
 CONSTRAINT chk_notification_status CHECK (status IN ('PENDING','SENDING','SMTP_ACCEPTED','UNKNOWN','FAILED')),
 INDEX idx_notification_due (status,next_attempt_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE order_returns (
    order_id BIGINT NOT NULL PRIMARY KEY,
 kind VARCHAR(20) NOT NULL,
 status VARCHAR(30) NOT NULL,
 reason VARCHAR(1000) NOT NULL,
 inspection VARCHAR(1000) NOT NULL,
 restock BOOLEAN NOT NULL DEFAULT FALSE,
 carrier_request_status VARCHAR(24) NOT NULL,
 requested_at DATETIME(6) NOT NULL,
 approved_at DATETIME(6) NULL,
 received_at DATETIME(6) NULL,
 inspected_at DATETIME(6) NULL,
 updated_at DATETIME(6) NOT NULL,
 CONSTRAINT fk_return_order FOREIGN KEY (order_id) REFERENCES orders(id),
 CONSTRAINT chk_return_kind CHECK (kind IN ('BUYER_RETURN','CARRIER_RETURN')),
 CONSTRAINT chk_return_status CHECK (status IN ('REQUESTED','APPROVED','INSPECTED')),
 CONSTRAINT chk_return_stock CHECK (restock=FALSE OR (status='INSPECTED' AND received_at IS NOT NULL AND inspected_at IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE cod_provider_reports (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
 order_id BIGINT NOT NULL,
 shipment_id VARCHAR(36) NOT NULL,
 dedup_key VARCHAR(64) NOT NULL UNIQUE,
 reported_amount DECIMAL(15,2) NOT NULL,
 reported_fee DECIMAL(15,2) NULL,
 transfer_at DATETIME(6) NOT NULL,
 reported_at DATETIME(6) NOT NULL,
 received_at DATETIME(6) NOT NULL,
 comparison VARCHAR(30) NOT NULL,
 CONSTRAINT fk_cod_report_order FOREIGN KEY (order_id) REFERENCES orders(id),
 CONSTRAINT fk_cod_report_shipment FOREIGN KEY (shipment_id) REFERENCES shipments(id),
 CONSTRAINT chk_cod_report_amount CHECK (reported_amount>=0 AND (reported_fee IS NULL OR reported_fee>=0)),
 INDEX idx_cod_reports_order (order_id,reported_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
