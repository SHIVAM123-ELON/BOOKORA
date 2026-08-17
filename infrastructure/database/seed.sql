-- ====================================================================
-- BOOKORA DEVELOPMENT SEED DATA
-- Marked explicitly as [DEVELOPMENT SEED]
-- ====================================================================

-- 1. Initial Categories
INSERT INTO categories (id, name, slug, description, icon_name, display_order) VALUES
('c0000001-0000-0000-0000-000000000001', 'Software Engineering', 'software-engineering', 'Architecture, clean code, distributed systems & modern web', 'code', 1),
('c0000001-0000-0000-0000-000000000002', 'Artificial Intelligence', 'artificial-intelligence', 'Machine learning, LLMs, neural networks and agentic AI', 'psychology', 2),
('c0000001-0000-0000-0000-000000000003', 'Startup & Business', 'startup-business', 'Venture capital, product-market fit, leadership and scaling', 'trending_up', 3),
('c0000001-0000-0000-0000-000000000004', 'Self Improvement', 'self-improvement', 'Habit mastery, mental models, deep work and productivity', 'self_improvement', 4),
('c0000001-0000-0000-0000-000000000005', 'Design & UI/UX', 'design-ui-ux', 'Design systems, typography, cognitive psychology and visual craft', 'palette', 5),
('c0000001-0000-0000-0000-000000000006', 'Sci-Fi & Fiction', 'sci-fi-fiction', 'Speculative futures, space exploration and immersive narratives', 'auto_stories', 6),
('c0000001-0000-0000-0000-000000000007', 'Finance & Investing', 'finance-investing', 'Financial freedom, macroeconomics and intelligent asset allocation', 'account_balance', 7),
('c0000001-0000-0000-0000-000000000008', 'History & Philosophy', 'history-philosophy', 'Timeless human thought, stoicism and civilizational evolution', 'menu_book', 8);

-- 2. Core Users (password: "Password123!" hashed with bcrypt)
INSERT INTO users (id, email, password_hash, full_name, role, is_verified) VALUES
('u0000001-0000-0000-0000-000000000001', 'admin@bookora.com', '$2b$10$wE9O0.oP6fRj4w7aX1Zz2.q0WwP/rN0oN7VqOaKjV3uF1gN9vX12y', 'Bookora SuperAdmin', 'SUPER_ADMIN', TRUE),
('u0000001-0000-0000-0000-000000000002', 'author.martin@bookora.com', '$2b$10$wE9O0.oP6fRj4w7aX1Zz2.q0WwP/rN0oN7VqOaKjV3uF1gN9vX12y', 'Robert C. Martin', 'AUTHOR', TRUE),
('u0000001-0000-0000-0000-000000000003', 'author.sam@bookora.com', '$2b$10$wE9O0.oP6fRj4w7aX1Zz2.q0WwP/rN0oN7VqOaKjV3uF1gN9vX12y', 'Sam Altman', 'AUTHOR', TRUE),
('u0000001-0000-0000-0000-000000000004', 'author.james@bookora.com', '$2b$10$wE9O0.oP6fRj4w7aX1Zz2.q0WwP/rN0oN7VqOaKjV3uF1gN9vX12y', 'James Clear', 'AUTHOR', TRUE),
('u0000001-0000-0000-0000-000000000005', 'reader@bookora.com', '$2b$10$wE9O0.oP6fRj4w7aX1Zz2.q0WwP/rN0oN7VqOaKjV3uF1gN9vX12y', 'Alex Mercer', 'READER', TRUE);

-- 3. Authors
INSERT INTO authors (id, user_id, pen_name, bio, is_verified) VALUES
('a0000001-0000-0000-0000-000000000001', 'u0000001-0000-0000-0000-000000000002', 'Robert C. Martin', 'Software craftsman and pioneer of clean code practices and agile software craftsmanship.', TRUE),
('a0000001-0000-0000-0000-000000000002', 'u0000001-0000-0000-0000-000000000003', 'Sam Altman', 'Tech entrepreneur, investor, and visionary exploring frontier AI architectures and societal evolution.', TRUE),
('a0000001-0000-0000-0000-000000000003', 'u0000001-0000-0000-0000-000000000004', 'James Clear', 'Writer and speaker focused on habits, decision making, and continuous personal systems improvement.', TRUE);

-- 4. Initial Books (Sample Seed)
INSERT INTO books (id, title, subtitle, slug, author_id, description, cover_image_url, price_cents, currency, discount_percentage, page_count, language, status, average_rating, total_reviews, sales_count, published_at) VALUES
(
    'b0000001-0000-0000-0000-000000000001',
    'Clean Architecture: A Craftsman''s Guide',
    'Structure and Design of Software Systems',
    'clean-architecture-craftsman-guide',
    'a0000001-0000-0000-0000-000000000001',
    'By applying universal rules of software architecture, you can dramatically improve developer productivity throughout the life of any software system. Uncle Bob presents timeless rules for component design, decoupled boundaries, and testable domain logic.',
    'https://images.unsplash.com/photo-1532012164546-f432f2e3edd4?w=600&auto=format&fit=crop&q=80',
    49900, 'INR', 15, 352, 'English', 'PUBLISHED', 4.85, 342, 1280, CURRENT_TIMESTAMP
),
(
    'b0000001-0000-0000-0000-000000000002',
    'Frontier AI & The Agentic Paradigm',
    'Architecting Multi-Agent Intelligence Systems',
    'frontier-ai-agentic-paradigm',
    'a0000001-0000-0000-0000-000000000002',
    'A comprehensive exploration of reasoning models, tool-use execution loops, chain-of-thought distillation, and the transition from static LLMs to autonomous cognitive software systems.',
    'https://images.unsplash.com/photo-1620712943543-bcc4688e7485?w=600&auto=format&fit=crop&q=80',
    79900, 'INR', 20, 280, 'English', 'PUBLISHED', 4.92, 189, 940, CURRENT_TIMESTAMP
),
(
    'b0000001-0000-0000-0000-000000000003',
    'Atomic Habits: Proven Framework',
    'Tiny Changes, Remarkable Results',
    'atomic-habits-proven-framework',
    'a0000001-0000-0000-0000-000000000003',
    'No matter your goals, Atomic Habits offers a proven framework for improving every day. James Clear reveals practical strategies that teach you exactly how to form good habits, break bad ones, and master the tiny behaviors that lead to remarkable results.',
    'https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=600&auto=format&fit=crop&q=80',
    39900, 'INR', 10, 320, 'English', 'PUBLISHED', 4.90, 810, 3400, CURRENT_TIMESTAMP
);

-- Map Categories
INSERT INTO book_categories (book_id, category_id) VALUES
('b0000001-0000-0000-0000-000000000001', 'c0000001-0000-0000-0000-000000000001'),
('b0000001-0000-0000-0000-000000000002', 'c0000001-0000-0000-0000-000000000002'),
('b0000001-0000-0000-0000-000000000003', 'c0000001-0000-0000-0000-000000000004');
