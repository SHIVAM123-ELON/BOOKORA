# BOOKORA — Database Schema, Performance & Migration Guidelines

## 1. Schema Design Principles

- **ACID Financial Integrity**: All order placements, royalty splits, and wallet debits execute inside strict database transactions with row-level locks (`SELECT FOR UPDATE`).
- **Idempotency Constraints**: Orders enforce unique `idempotency_key` indexes to eliminate double-charging risks during network retries.
- **Entitlement Uniqueness**: Enforced compound constraint `UNIQUE(user_id, book_id)` on `entitlements` table.

---

## 2. Migration Execution Order

Migrations are located in `infrastructure/database/migrations/` and run sequentially:
1. `V1__initial_schema.sql`: Core tables (`users`, `books`, `library_items`).
2. `V2__financial_tables.sql`: Financial tables (`orders`, `order_items`, `entitlements`, `subscriptions`, `royalty_ledger`).
3. `V3__performance_indexes.sql`: Multi-column indexes for catalog filtering, rating sorts, and user entitlement lookups.
4. `V4__audit_and_security.sql`: Administrative compliance audit logs and token revocation tracking.

---

## 3. High-Performance Indexing Strategy

| Table | Index Name | Indexed Columns | Query Target |
| :--- | :--- | :--- | :--- |
| `books` | `idx_books_category` | `(category_id)` | Category filtering |
| `books` | `idx_books_rating` | `(rating_average DESC)` | Popular / Top Rated carousels |
| `books` | `idx_books_created` | `(created_at DESC)` | New releases carousels |
| `entitlements` | `idx_entitlements_lookup` | `(user_id, book_id, is_active)` | Sub-millisecond DRM check |
| `orders` | `idx_orders_user` | `(user_id, created_at DESC)` | User order history |
| `royalty_ledger`| `idx_royalty_author` | `(author_id, created_at DESC)` | Author earnings dashboard |

---

## 4. Connection Pooling Configuration

- **Pool Min Size**: 5 connections
- **Pool Max Size**: 25 connections per replica node
- **Connection Timeout**: 10,000 ms
- **Max Lifetime**: 1,800,000 ms (30 minutes)
- **Idle Timeout**: 600,000 ms (10 minutes)
