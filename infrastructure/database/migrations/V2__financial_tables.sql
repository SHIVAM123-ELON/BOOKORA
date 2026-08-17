-- ====================================================================
-- BOOKORA V2: Orders, Entitlements, Payments & Subscriptions
-- ====================================================================

CREATE TABLE IF NOT EXISTS orders (
    id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL REFERENCES users(id),
    total_amount_cents INT NOT NULL,
    discount_amount_cents INT NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    payment_method VARCHAR(64) NOT NULL,
    idempotency_key VARCHAR(128) UNIQUE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS order_items (
    id VARCHAR(64) PRIMARY KEY,
    order_id VARCHAR(64) NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    book_id VARCHAR(64) NOT NULL REFERENCES books(id),
    price_cents INT NOT NULL,
    author_id VARCHAR(64) NOT NULL REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS entitlements (
    id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL REFERENCES users(id),
    book_id VARCHAR(64) NOT NULL REFERENCES books(id),
    order_id VARCHAR(64) REFERENCES orders(id),
    grant_type VARCHAR(32) NOT NULL DEFAULT 'PURCHASE', -- PURCHASE, SUBSCRIPTION, PROMO
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    expires_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_entitlement_user_book UNIQUE (user_id, book_id)
);

CREATE TABLE IF NOT EXISTS subscriptions (
    id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL REFERENCES users(id),
    plan_tier VARCHAR(64) NOT NULL, -- UNLIMITED, VIP_SCHOLAR
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    renews_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS royalty_ledger (
    id VARCHAR(64) PRIMARY KEY,
    author_id VARCHAR(64) NOT NULL REFERENCES users(id),
    order_id VARCHAR(64) NOT NULL REFERENCES orders(id),
    book_id VARCHAR(64) NOT NULL REFERENCES books(id),
    gross_amount_cents INT NOT NULL,
    platform_fee_cents INT NOT NULL,
    author_net_cents INT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
