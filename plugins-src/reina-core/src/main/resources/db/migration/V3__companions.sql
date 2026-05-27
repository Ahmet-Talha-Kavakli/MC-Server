CREATE TABLE IF NOT EXISTS player_companions (
    uuid           CHAR(36)     NOT NULL,
    companion_id   VARCHAR(32)  NOT NULL,
    purchased_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (uuid, companion_id),
    INDEX idx_uuid (uuid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS player_companion_equipped (
    uuid           CHAR(36)     NOT NULL PRIMARY KEY,
    companion_id   VARCHAR(32)  NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
