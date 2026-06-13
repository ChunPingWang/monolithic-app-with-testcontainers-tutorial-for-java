SET search_path TO payment;

CREATE TABLE payments (
    id                  UUID            PRIMARY KEY,
    order_id            UUID            NOT NULL,
    amount              NUMERIC(19, 4)  NOT NULL,
    currency            CHAR(3)         NOT NULL,
    idempotency_key     VARCHAR(200)    NOT NULL UNIQUE,
    status_kind         VARCHAR(20)     NOT NULL,
    status_at           TIMESTAMP WITH TIME ZONE NOT NULL,
    status_receipt_key  VARCHAR(500),
    status_reason       VARCHAR(500),
    version             BIGINT          NOT NULL DEFAULT 0
);
CREATE INDEX idx_payments_order ON payments (order_id);
