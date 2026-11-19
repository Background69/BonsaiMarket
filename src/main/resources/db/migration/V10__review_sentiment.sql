-- Durable inference outbox/result, separate from review stars and mail delivery.
-- No backfill: only newly submitted verified-purchase reviews are enqueued when enabled.
CREATE TABLE review_sentiment (
    review_id BIGINT NOT NULL PRIMARY KEY,
    content_version VARCHAR(64) NOT NULL,
    status VARCHAR(16) NOT NULL,
    label VARCHAR(16) NULL,
    model_version VARCHAR(100) NULL,
    evaluation_mode VARCHAR(40) NULL,
    demo_only BOOLEAN NULL,
    attempts INT NOT NULL DEFAULT 0,
    lease_token VARCHAR(36) NULL,
    next_attempt_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    error_code VARCHAR(40) NULL,
    CONSTRAINT fk_review_sentiment_review FOREIGN KEY (review_id) REFERENCES reviews(id) ON DELETE CASCADE,
    CONSTRAINT ck_review_sentiment_status CHECK (status IN ('PENDING','PROCESSING','ANALYZED','FAILED','STALE')),
    CONSTRAINT ck_review_sentiment_label CHECK (label IS NULL OR label IN ('positive','neutral','negative','mixed')),
    CONSTRAINT ck_review_sentiment_result CHECK ((status = 'ANALYZED' AND label IS NOT NULL AND model_version IS NOT NULL) OR (status <> 'ANALYZED' AND label IS NULL)),
    CONSTRAINT ck_review_sentiment_attempts CHECK (attempts BETWEEN 0 AND 3)
);
CREATE INDEX idx_review_sentiment_due ON review_sentiment(status, next_attempt_at, review_id);
