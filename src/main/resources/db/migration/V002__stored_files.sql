CREATE TABLE IF NOT EXISTS stored_files (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    bucket       VARCHAR(100)  NOT NULL,
    filename     VARCHAR(255)  NOT NULL,
    content_type VARCHAR(100)  NOT NULL,
    data         BYTEA         NOT NULL,
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT now()
);
