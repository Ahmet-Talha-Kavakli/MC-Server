CREATE TABLE IF NOT EXISTS player_data (
    uuid           CHAR(36)     NOT NULL PRIMARY KEY,
    name           VARCHAR(16)  NOT NULL,
    rank_id        VARCHAR(32)  NOT NULL DEFAULT 'MEMBER',
    coins          BIGINT       NOT NULL DEFAULT 0,
    gems           BIGINT       NOT NULL DEFAULT 0,
    level          INT          NOT NULL DEFAULT 1,
    xp             BIGINT       NOT NULL DEFAULT 0,
    first_login    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_server    VARCHAR(32)  NOT NULL DEFAULT 'hub',
    INDEX idx_name (name),
    INDEX idx_rank (rank_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS economy_transactions (
    id            BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    uuid          CHAR(36)     NOT NULL,
    currency      VARCHAR(8)   NOT NULL,
    delta         BIGINT       NOT NULL,
    balance_after BIGINT       NOT NULL,
    reason        VARCHAR(64)  NOT NULL,
    server        VARCHAR(32)  NOT NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_uuid (uuid),
    INDEX idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
