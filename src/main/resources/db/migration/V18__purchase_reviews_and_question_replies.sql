-- Preserve legacy reviews without inventing a purchase association.
ALTER TABLE reviews ADD COLUMN order_item_id BIGINT NULL;
-- The old composite index may back fk_reviews_user in InnoDB. Replace that support first.
CREATE INDEX idx_reviews_user ON reviews(user_id);
ALTER TABLE reviews DROP INDEX uk_reviews_user_product;
ALTER TABLE reviews ADD CONSTRAINT uk_reviews_order_item UNIQUE (order_item_id);
ALTER TABLE reviews ADD CONSTRAINT fk_reviews_order_item FOREIGN KEY (order_item_id) REFERENCES order_items(id);
CREATE INDEX idx_reviews_product_created ON reviews(product_id,created_at,id);

-- One level of replies under the existing question. Official answers stay untouched.
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
