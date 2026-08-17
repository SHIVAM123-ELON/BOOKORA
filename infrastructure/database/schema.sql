-- ====================================================================
-- BOOKORA PRODUCTION DATABASE SCHEMA (PostgreSQL 16)
-- Complete normalized schema for Digital Book Marketplace & Ecosystem
-- ====================================================================

-- 1. Enable required extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- 2. Enumerations
CREATE TYPE user_role_enum AS ENUM (
    'READER',
    'AUTHOR',
    'PUBLISHER',
    'MODERATOR',
    'ADMIN',
    'SUPER_ADMIN'
);

CREATE TYPE book_status_enum AS ENUM (
    'DRAFT',
    'SUBMITTED',
    'UNDER_REVIEW',
    'APPROVED',
    'PUBLISHED',
    'REJECTED',
    'ARCHIVED'
);

CREATE TYPE order_status_enum AS ENUM (
    'PENDING',
    'PROCESSING',
    'COMPLETED',
    'FAILED',
    'REFUNDED'
);

CREATE TYPE payment_status_enum AS ENUM (
    'INITIATED',
    'AUTHORIZED',
    'CAPTURED',
    'FAILED',
    'REFUNDED'
);

CREATE TYPE reading_status_enum AS ENUM (
    'NOT_STARTED',
    'IN_PROGRESS',
    'COMPLETED'
);

CREATE TYPE copyright_status_enum AS ENUM (
    'OPEN',
    'UNDER_REVIEW',
    'RESOLVED',
    'REJECTED'
);

-- 3. Users Table
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(150) NOT NULL,
    role user_role_enum NOT NULL DEFAULT 'READER',
    avatar_url TEXT,
    is_verified BOOLEAN NOT NULL DEFAULT FALSE,
    is_suspended BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);

-- 4. Authors Table
CREATE TABLE authors (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    pen_name VARCHAR(150) NOT NULL,
    bio TEXT,
    avatar_url TEXT,
    website_url TEXT,
    is_verified BOOLEAN NOT NULL DEFAULT FALSE,
    payout_account_info JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_authors_user_id ON authors(user_id);
CREATE INDEX idx_authors_pen_name ON authors(pen_name);

-- 5. Publishers Table
CREATE TABLE publishers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(200) NOT NULL,
    slug VARCHAR(200) NOT NULL UNIQUE,
    description TEXT,
    logo_url TEXT,
    owner_user_id UUID NOT NULL REFERENCES users(id),
    is_verified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_publishers_slug ON publishers(slug);

-- 6. Categories Table
CREATE TABLE categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    icon_name VARCHAR(50) NOT NULL DEFAULT 'book',
    display_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_categories_slug ON categories(slug);

-- 7. Books Table
CREATE TABLE books (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(255) NOT NULL,
    subtitle VARCHAR(255),
    slug VARCHAR(255) NOT NULL UNIQUE,
    author_id UUID NOT NULL REFERENCES authors(id) ON DELETE RESTRICT,
    publisher_id UUID REFERENCES publishers(id) ON DELETE SET NULL,
    description TEXT NOT NULL,
    cover_image_url TEXT NOT NULL,
    preview_url TEXT,
    content_file_key TEXT, -- S3/Object Storage Key
    price_cents INT NOT NULL DEFAULT 0, -- Store in smallest currency unit (cents/paise)
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    discount_percentage INT NOT NULL DEFAULT 0,
    page_count INT NOT NULL DEFAULT 0,
    language VARCHAR(50) NOT NULL DEFAULT 'English',
    isbn VARCHAR(20),
    status book_status_enum NOT NULL DEFAULT 'DRAFT',
    rejection_reason TEXT,
    average_rating NUMERIC(3, 2) NOT NULL DEFAULT 0.00,
    total_reviews INT NOT NULL DEFAULT 0,
    sales_count INT NOT NULL DEFAULT 0,
    published_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ
);

CREATE INDEX idx_books_author_id ON books(author_id);
CREATE INDEX idx_books_status ON books(status);
CREATE INDEX idx_books_slug ON books(slug);
CREATE INDEX idx_books_rating ON books(average_rating DESC);
CREATE INDEX idx_books_price ON books(price_cents);

-- 8. Book Categories Mapping (Many-to-Many)
CREATE TABLE book_categories (
    book_id UUID NOT NULL REFERENCES books(id) ON DELETE CASCADE,
    category_id UUID NOT NULL REFERENCES categories(id) ON DELETE CASCADE,
    PRIMARY KEY (book_id, category_id)
);

-- 9. Orders Table
CREATE TABLE orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    order_number VARCHAR(50) NOT NULL UNIQUE,
    total_amount_cents INT NOT NULL,
    discount_amount_cents INT NOT NULL DEFAULT 0,
    net_amount_cents INT NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    status order_status_enum NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_orders_user_id ON orders(user_id);
CREATE INDEX idx_orders_status ON orders(status);

-- 10. Order Items Table
CREATE TABLE order_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    book_id UUID NOT NULL REFERENCES books(id) ON DELETE RESTRICT,
    unit_price_cents INT NOT NULL,
    discount_cents INT NOT NULL DEFAULT 0,
    final_price_cents INT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_order_items_order ON order_items(order_id);

-- 11. Payments Table
CREATE TABLE payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE RESTRICT,
    payment_gateway VARCHAR(50) NOT NULL DEFAULT 'MOCK_SANDBOX',
    gateway_transaction_id VARCHAR(255),
    amount_cents INT NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    status payment_status_enum NOT NULL DEFAULT 'INITIATED',
    payment_method VARCHAR(50),
    raw_response JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_payments_order ON payments(order_id);

-- 12. User Library (Entitlements) Table
CREATE TABLE user_libraries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    book_id UUID NOT NULL REFERENCES books(id) ON DELETE RESTRICT,
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE RESTRICT,
    reading_progress NUMERIC(5, 2) NOT NULL DEFAULT 0.00, -- 0.00% to 100.00%
    last_read_page INT NOT NULL DEFAULT 1,
    status reading_status_enum NOT NULL DEFAULT 'NOT_STARTED',
    last_read_at TIMESTAMPTZ,
    acquired_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, book_id)
);

CREATE INDEX idx_user_libraries_user ON user_libraries(user_id);
CREATE INDEX idx_user_libraries_book ON user_libraries(book_id);

-- 13. Bookmarks Table
CREATE TABLE bookmarks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    book_id UUID NOT NULL REFERENCES books(id) ON DELETE CASCADE,
    page_number INT NOT NULL,
    title VARCHAR(150),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_bookmarks_user_book ON bookmarks(user_id, book_id);

-- 14. Highlights & Notes Table
CREATE TABLE reader_notes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    book_id UUID NOT NULL REFERENCES books(id) ON DELETE CASCADE,
    page_number INT NOT NULL,
    selected_text TEXT,
    note_text TEXT NOT NULL,
    color_hex VARCHAR(7) DEFAULT '#FFD54F',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 15. Reviews & Ratings Table
CREATE TABLE reviews (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    book_id UUID NOT NULL REFERENCES books(id) ON DELETE CASCADE,
    rating INT NOT NULL CHECK (rating >= 1 AND rating <= 5),
    title VARCHAR(150),
    content TEXT,
    is_verified_purchase BOOLEAN NOT NULL DEFAULT TRUE,
    is_approved BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, book_id)
);

CREATE INDEX idx_reviews_book ON reviews(book_id);

-- 16. Wishlists Table
CREATE TABLE wishlists (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    book_id UUID NOT NULL REFERENCES books(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, book_id)
);

-- 17. Copyright Claims Table
CREATE TABLE copyright_claims (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    book_id UUID NOT NULL REFERENCES books(id) ON DELETE CASCADE,
    claimant_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    claimant_name VARCHAR(150) NOT NULL,
    claimant_email VARCHAR(255) NOT NULL,
    evidence_description TEXT NOT NULL,
    evidence_urls JSONB,
    status copyright_status_enum NOT NULL DEFAULT 'OPEN',
    resolution_notes TEXT,
    resolved_by_admin_id UUID REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 18. Audit Logs Table
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_id UUID REFERENCES users(id) ON DELETE SET NULL,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id UUID,
    ip_address VARCHAR(45),
    user_agent TEXT,
    payload JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_logs_actor ON audit_logs(actor_id);
CREATE INDEX idx_audit_logs_action ON audit_logs(action);
