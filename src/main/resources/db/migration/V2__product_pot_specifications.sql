-- Additive MySQL 8.4 migration. DDL auto-commits; rehearse on a copy before deployment.
ALTER TABLE categories ADD COLUMN specification_type VARCHAR(20) NULL,
    ADD CONSTRAINT chk_category_specification_type CHECK (specification_type IS NULL OR specification_type = 'POT');
ALTER TABLE products ADD COLUMN description_format VARCHAR(20) NOT NULL DEFAULT 'PLAIN',
    ADD CONSTRAINT chk_product_description_format CHECK (description_format IN ('PLAIN','MARKDOWN'));

CREATE TABLE product_pot_specs (
    product_id BIGINT NOT NULL PRIMARY KEY,
    material VARCHAR(160) NULL,
    shape VARCHAR(20) NULL,
    color VARCHAR(100) NULL,
    diameter_cm DECIMAL(6,1) NULL,
    length_cm DECIMAL(6,1) NULL,
    width_cm DECIMAL(6,1) NULL,
    height_cm DECIMAL(6,1) NULL,
    drainage_status VARCHAR(20) NULL,
    drainage_hole_count INT NULL,
    weight_grams INT NULL,
    has_tray BOOLEAN NULL,
    suitable_plants VARCHAR(1000) NULL,
    usage_instructions VARCHAR(2000) NULL,
    CONSTRAINT fk_pot_specs_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    CONSTRAINT chk_pot_shape CHECK (shape IS NULL OR shape IN ('ROUND','RECTANGULAR','SQUARE','OVAL','OTHER')),
    CONSTRAINT chk_pot_drainage CHECK (drainage_status IS NULL OR drainage_status IN ('YES','NO','UNKNOWN')),
    CONSTRAINT chk_pot_diameter CHECK (diameter_cm IS NULL OR diameter_cm BETWEEN 0.1 AND 500),
    CONSTRAINT chk_pot_length CHECK (length_cm IS NULL OR length_cm BETWEEN 0.1 AND 500),
    CONSTRAINT chk_pot_width CHECK (width_cm IS NULL OR width_cm BETWEEN 0.1 AND 500),
    CONSTRAINT chk_pot_height CHECK (height_cm IS NULL OR height_cm BETWEEN 0.1 AND 500),
    CONSTRAINT chk_pot_weight CHECK (weight_grams IS NULL OR weight_grams BETWEEN 1 AND 1000000),
    CONSTRAINT chk_pot_tray CHECK (has_tray IS NULL OR has_tray IN (0,1)),
    CONSTRAINT chk_pot_holes CHECK (drainage_hole_count IS NULL OR drainage_hole_count BETWEEN 0 AND 1000),
    CONSTRAINT chk_pot_no_holes CHECK (drainage_status <> 'NO' OR drainage_hole_count IS NULL OR drainage_hole_count = 0),
    CONSTRAINT chk_pot_yes_holes CHECK (drainage_status <> 'YES' OR drainage_hole_count IS NULL OR drainage_hole_count > 0),
    INDEX idx_pot_material (material),
    INDEX idx_pot_diameter (diameter_cm)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Recognized dedicated taxonomy only; mixed pots/accessories and plants are deliberately excluded.
UPDATE categories SET specification_type='POT'
WHERE specification_type IS NULL AND (
    (slug='chau-gom-men' AND name='Chậu gốm men') OR
    (slug='chau-dat-nung' AND name='Chậu đất nung') OR
    (slug='chau-xi-mang' AND name='Chậu xi măng') OR
    (slug='chau-treo' AND name='Chậu treo'));

-- CASE guards malformed JSON before extraction/casting. Reject strings, invalid scale and out-of-range numbers.
-- Old JSON is retained byte-for-byte; no heightCm copy (that field means tree height).
INSERT INTO product_pot_specs(product_id,material,diameter_cm)
SELECT candidate.id,candidate.material,candidate.diameter_cm FROM (
    SELECT p.id,
        CASE WHEN JSON_TYPE(JSON_EXTRACT(CASE WHEN JSON_VALID(p.technical_specs_json) THEN p.technical_specs_json ELSE '{}' END,'$.potMaterial'))='STRING'
            AND CHAR_LENGTH(TRIM(JSON_UNQUOTE(JSON_EXTRACT(CASE WHEN JSON_VALID(p.technical_specs_json) THEN p.technical_specs_json ELSE '{}' END,'$.potMaterial')))) BETWEEN 1 AND 160
            THEN TRIM(JSON_UNQUOTE(JSON_EXTRACT(p.technical_specs_json,'$.potMaterial'))) ELSE NULL END AS material,
        CASE WHEN JSON_TYPE(JSON_EXTRACT(CASE WHEN JSON_VALID(p.technical_specs_json) THEN p.technical_specs_json ELSE '{}' END,'$.potDiameterCm')) IN ('INTEGER','DOUBLE','DECIMAL')
            AND JSON_UNQUOTE(JSON_EXTRACT(CASE WHEN JSON_VALID(p.technical_specs_json) THEN p.technical_specs_json ELSE '{}' END,'$.potDiameterCm')) REGEXP '^[0-9]{1,3}([.][0-9])?$'
            THEN CASE WHEN CAST(JSON_UNQUOTE(JSON_EXTRACT(p.technical_specs_json,'$.potDiameterCm')) AS DECIMAL(6,1)) BETWEEN 0.1 AND 500
                THEN CAST(JSON_UNQUOTE(JSON_EXTRACT(p.technical_specs_json,'$.potDiameterCm')) AS DECIMAL(6,1)) ELSE NULL END ELSE NULL END AS diameter_cm
    FROM products p JOIN categories c ON c.id=p.category_id WHERE c.specification_type='POT'
) candidate WHERE candidate.material IS NOT NULL OR candidate.diameter_cm IS NOT NULL;
