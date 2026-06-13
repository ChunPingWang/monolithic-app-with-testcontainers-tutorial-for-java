SET search_path TO product;

CREATE TABLE products (
    id              UUID            PRIMARY KEY,
    name            VARCHAR(200)    NOT NULL,
    description     TEXT,
    price_amount    NUMERIC(19, 4)  NOT NULL,
    price_currency  CHAR(3)         NOT NULL,
    image_object_key VARCHAR(500),
    version         BIGINT          NOT NULL DEFAULT 0
);

CREATE TABLE orders (
    id              UUID            PRIMARY KEY,
    buyer_id        VARCHAR(100)    NOT NULL,
    total_amount    NUMERIC(19, 4)  NOT NULL,
    total_currency  CHAR(3)         NOT NULL,
    status_kind     VARCHAR(20)     NOT NULL,
    status_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    status_payment_id UUID,
    status_reason   VARCHAR(500),
    version         BIGINT          NOT NULL DEFAULT 0
);
CREATE INDEX idx_orders_buyer ON orders (buyer_id);

CREATE TABLE order_lines (
    order_id        UUID            NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    line_index      INTEGER         NOT NULL,
    product_id      UUID            NOT NULL,
    quantity        INTEGER         NOT NULL,
    unit_amount     NUMERIC(19, 4)  NOT NULL,
    unit_currency   CHAR(3)         NOT NULL,
    PRIMARY KEY (order_id, line_index)
);
