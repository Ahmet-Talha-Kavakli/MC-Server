CREATE TABLE IF NOT EXISTS player_cosmetics (
    uuid          CHAR(36)     NOT NULL,
    cosmetic_id   VARCHAR(48)  NOT NULL,
    purchased_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (uuid, cosmetic_id),
    INDEX idx_uuid (uuid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS player_cosmetic_equipped (
    uuid          CHAR(36)     NOT NULL,
    category      VARCHAR(16)  NOT NULL,
    cosmetic_id   VARCHAR(48)  NOT NULL,
    PRIMARY KEY (uuid, category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
