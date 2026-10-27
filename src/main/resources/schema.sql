-- One row per audit. JdbcAuditRepository stores the whole Audit as JSON in "data".
CREATE TABLE IF NOT EXISTS audit (
    id   VARCHAR(64) PRIMARY KEY,
    data CLOB        NOT NULL
);
