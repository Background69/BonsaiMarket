-- Nullable dedicated image; existing categories continue to use the UI fallback.
ALTER TABLE categories ADD COLUMN image_url VARCHAR(500) NULL;
