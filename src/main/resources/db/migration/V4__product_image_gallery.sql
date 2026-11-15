-- Additive MySQL 8.4 migration; DDL commits implicitly. Do not edit V1-V3.
ALTER TABLE products
    ADD COLUMN gallery_revision BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN gallery_initialized BOOLEAN NOT NULL DEFAULT FALSE,
    ADD CONSTRAINT chk_product_gallery_revision CHECK (gallery_revision >= 0);

CREATE TABLE product_gallery_images (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    storage_key VARCHAR(50) NULL,
    thumbnail_key VARCHAR(50) NULL,
    image_url VARCHAR(500) NOT NULL,
    sort_order INT NOT NULL,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    content_type VARCHAR(30) NULL,
    file_size_bytes BIGINT NULL,
    width INT NULL,
    height INT NULL,
    checksum VARCHAR(64) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    primary_product_id BIGINT GENERATED ALWAYS AS (CASE WHEN is_primary THEN product_id ELSE NULL END) VIRTUAL,
    CONSTRAINT fk_gallery_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    CONSTRAINT uk_gallery_primary UNIQUE (primary_product_id),
    CONSTRAINT uk_gallery_checksum UNIQUE (product_id,checksum),
    CONSTRAINT chk_gallery_order CHECK (sort_order BETWEEN 0 AND 7),
    CONSTRAINT chk_gallery_primary CHECK (is_primary IN (0,1)),
    CONSTRAINT chk_gallery_size CHECK (file_size_bytes IS NULL OR file_size_bytes > 0),
    CONSTRAINT chk_gallery_dimensions CHECK ((width IS NULL AND height IS NULL) OR (width IS NOT NULL AND height IS NOT NULL AND width BETWEEN 1 AND 6000 AND height BETWEEN 1 AND 6000 AND CAST(width AS SIGNED)*height <= 20000000)),
    CONSTRAINT chk_gallery_storage CHECK (storage_key IS NOT NULL OR (thumbnail_key IS NULL AND checksum IS NULL)),
    INDEX idx_gallery_product_order (product_id,sort_order,id),
    INDEX idx_gallery_storage (storage_key),
    INDEX idx_gallery_thumbnail (thumbnail_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Durable idempotency receipt, including after an image has been removed.
CREATE TABLE product_gallery_uploads (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    request_key VARCHAR(36) NOT NULL,
    checksum VARCHAR(64) NOT NULL,
    image_id BIGINT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_gallery_upload_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    CONSTRAINT fk_gallery_upload_image FOREIGN KEY (image_id) REFERENCES product_gallery_images(id) ON DELETE SET NULL,
    CONSTRAINT uk_gallery_upload_request UNIQUE (product_id,request_key),
    CONSTRAINT chk_gallery_upload_status CHECK (status IN ('COMPLETED','REMOVED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- products.image_url owns the legacy cover choice. Use product_images keys only when
-- the URL is exactly a known legacy serving route (or is absent), never parse/guess keys.
-- A different external URL and a stale product_images row represent ONE chosen cover.
INSERT INTO product_gallery_images(product_id,storage_key,image_url,sort_order,is_primary,content_type,file_size_bytes)
SELECT p.id,
    CASE WHEN i.file_key REGEXP '^[0-9a-f-]{36}[.]png$' AND
        (NULLIF(TRIM(p.image_url),'') IS NULL OR p.image_url=CONCAT('/api/products/',p.id,'/image') OR p.image_url=CONCAT('/api/products/',p.id,'/image?v=',LEFT(i.file_key,36))) THEN i.file_key ELSE NULL END,
    COALESCE(CASE WHEN NULLIF(TRIM(p.image_url),'') IS NOT NULL THEN p.image_url ELSE NULL END,CONCAT('/api/products/',p.id,'/image?v=',LEFT(i.file_key,36))),0,TRUE,
    CASE WHEN i.file_key REGEXP '^[0-9a-f-]{36}[.]png$' AND
        (NULLIF(TRIM(p.image_url),'') IS NULL OR p.image_url=CONCAT('/api/products/',p.id,'/image') OR p.image_url=CONCAT('/api/products/',p.id,'/image?v=',LEFT(i.file_key,36))) THEN i.content_type ELSE NULL END,
    CASE WHEN i.file_key REGEXP '^[0-9a-f-]{36}[.]png$' AND i.bytes>0 AND
        (NULLIF(TRIM(p.image_url),'') IS NULL OR p.image_url=CONCAT('/api/products/',p.id,'/image') OR p.image_url=CONCAT('/api/products/',p.id,'/image?v=',LEFT(i.file_key,36))) THEN i.bytes ELSE NULL END
FROM products p LEFT JOIN product_images i ON i.product_id=p.id
WHERE NULLIF(TRIM(p.image_url),'') IS NOT NULL OR i.file_key REGEXP '^[0-9a-f-]{36}[.]png$';

UPDATE products p JOIN product_gallery_images g ON g.product_id=p.id AND g.is_primary=TRUE
SET p.gallery_initialized=TRUE,p.image_url=g.image_url;
-- Retain product_images and its files. No external fetching or invented dimensions/MIME/bytes.
