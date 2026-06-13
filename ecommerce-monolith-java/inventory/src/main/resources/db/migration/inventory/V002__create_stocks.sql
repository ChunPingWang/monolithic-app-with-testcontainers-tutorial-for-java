SET search_path TO inventory;

CREATE TABLE stocks (
    product_id      UUID            PRIMARY KEY,
    available       INTEGER         NOT NULL CHECK (available >= 0),
    reserved        INTEGER         NOT NULL DEFAULT 0 CHECK (reserved >= 0),
    version         BIGINT          NOT NULL DEFAULT 0
);

CREATE TABLE reservations (
    order_id        UUID            PRIMARY KEY,
    product_id      UUID            NOT NULL REFERENCES stocks(product_id),
    quantity        INTEGER         NOT NULL,
    reserved_at     TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_reservations_product ON reservations (product_id);
