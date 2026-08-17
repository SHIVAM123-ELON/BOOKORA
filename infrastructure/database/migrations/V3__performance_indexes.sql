-- ====================================================================
-- BOOKORA V3: Query Optimization & Performance Indexes
-- ====================================================================

-- Index frequently queried book filters
CREATE INDEX IF NOT EXISTS idx_books_category ON books(category_id);
CREATE INDEX IF NOT EXISTS idx_books_author ON books(author_id);
CREATE INDEX IF NOT EXISTS idx_books_published ON books(is_published);
CREATE INDEX IF NOT EXISTS idx_books_rating ON books(rating_average DESC);
CREATE INDEX IF NOT EXISTS idx_books_created ON books(created_at DESC);

-- Index user library and entitlement queries
CREATE INDEX IF NOT EXISTS idx_library_user ON library_items(user_id);
CREATE INDEX IF NOT EXISTS idx_entitlements_lookup ON entitlements(user_id, book_id, is_active);

-- Index order queries
CREATE INDEX IF NOT EXISTS idx_orders_user ON orders(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_orders_status ON orders(status);
CREATE INDEX IF NOT EXISTS idx_orders_idempotency ON orders(idempotency_key);

-- Index author royalty calculations
CREATE INDEX IF NOT EXISTS idx_royalty_author ON royalty_ledger(author_id, created_at DESC);
