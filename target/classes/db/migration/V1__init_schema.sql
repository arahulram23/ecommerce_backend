-- Users & roles
CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name     VARCHAR(255) NOT NULL,
    role          VARCHAR(20)  NOT NULL CHECK (role IN ('ADMIN', 'VENDOR', 'CUSTOMER')),
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP    NOT NULL DEFAULT now()
);

-- Vendors (one-to-one with a VENDOR-role user)
CREATE TABLE vendors (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    store_name   VARCHAR(255) NOT NULL,
    description  TEXT,
    approved     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (user_id)
);

CREATE TABLE categories (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE products (
    id           BIGSERIAL PRIMARY KEY,
    vendor_id    BIGINT NOT NULL REFERENCES vendors(id) ON DELETE CASCADE,
    category_id  BIGINT REFERENCES categories(id),
    name         VARCHAR(255) NOT NULL,
    description  TEXT,
    price_cents  BIGINT NOT NULL CHECK (price_cents >= 0),
    stock        INTEGER NOT NULL DEFAULT 0 CHECK (stock >= 0),
    image_url    VARCHAR(1024),
    active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_products_vendor ON products(vendor_id);
CREATE INDEX idx_products_category ON products(category_id);

CREATE TABLE carts (
    id      BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE cart_items (
    id         BIGSERIAL PRIMARY KEY,
    cart_id    BIGINT NOT NULL REFERENCES carts(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    quantity   INTEGER NOT NULL CHECK (quantity > 0),
    UNIQUE (cart_id, product_id)
);

CREATE TABLE orders (
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT NOT NULL REFERENCES users(id),
    status        VARCHAR(20) NOT NULL CHECK (status IN ('PENDING','PAID','SHIPPED','DELIVERED','CANCELLED')),
    total_cents   BIGINT NOT NULL CHECK (total_cents >= 0),
    shipping_address TEXT NOT NULL,
    created_at    TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE order_items (
    id            BIGSERIAL PRIMARY KEY,
    order_id      BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id    BIGINT NOT NULL REFERENCES products(id),
    vendor_id     BIGINT NOT NULL REFERENCES vendors(id),
    quantity      INTEGER NOT NULL CHECK (quantity > 0),
    price_cents_at_purchase BIGINT NOT NULL
);
CREATE INDEX idx_order_items_order ON order_items(order_id);
CREATE INDEX idx_order_items_vendor ON order_items(vendor_id);

CREATE TABLE payments (
    id                  BIGSERIAL PRIMARY KEY,
    order_id            BIGINT NOT NULL UNIQUE REFERENCES orders(id),
    stripe_payment_intent_id VARCHAR(255),
    status              VARCHAR(20) NOT NULL CHECK (status IN ('PENDING','SUCCEEDED','FAILED','REFUNDED')),
    amount_cents        BIGINT NOT NULL,
    created_at          TIMESTAMP NOT NULL DEFAULT now()
);
