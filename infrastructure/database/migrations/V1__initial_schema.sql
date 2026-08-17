-- ====================================================================
-- BOOKORA V1: Core Domain Schema
-- ====================================================================

CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(64) PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(32) NOT NULL DEFAULT 'READER',
    display_name VARCHAR(128) NOT NULL,
    avatar_url TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS books (
    id VARCHAR(64) PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    author_id VARCHAR(64) NOT NULL REFERENCES users(id),
    author_name VARCHAR(128) NOT NULL,
    category_id VARCHAR(64) NOT NULL,
    category_name VARCHAR(128) NOT NULL,
    description TEXT NOT NULL,
    price_cents INT NOT NULL DEFAULT 0,
    cover_image_url TEXT NOT NULL,
    file_vault_path TEXT NOT NULL,
    rating_average DECIMAL(3,2) NOT NULL DEFAULT 0.00,
    rating_count INT NOT NULL DEFAULT 0,
    is_published BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS library_items (
    id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL REFERENCES users(id),
    book_id VARCHAR(64) NOT NULL REFERENCES books(id),
    progress_percentage DECIMAL(5,2) NOT NULL DEFAULT 0.00,
    last_read_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_user_book UNIQUE (user_id, book_id)
);
