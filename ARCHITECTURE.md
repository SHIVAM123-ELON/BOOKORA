# BOOKORA — Systems Architecture & Scalability Blueprint

## 1. High-Level Architectural Diagram

```
+---------------------------------------------------------------+
|                       Bookora Clients                         |
|  [ Android Compose App ]              [ Web Reader / Studio ] |
+-------------------------------+-------------------------------+
                                | (HTTPS / TLS 1.3 + Request-ID)
                                v
+---------------------------------------------------------------+
|                     Cloudflare CDN / WAF                      |
|  - Edge Caching (Book Covers, Public Previews)                |
|  - DDoS Mitigation & Rate Limiting Guard                      |
+-------------------------------+-------------------------------+
                                |
                                v
+---------------------------------------------------------------+
|                      API Gateway & Ingress                    |
|  - JWT Authentication & RBAC Filter                           |
|  - Rate Limiter (Token Bucket per Tier)                       |
|  - Request-ID Injection & Structured JSON Logging             |
+-------------------------------+-------------------------------+
                                |
        +-----------------------+-----------------------+
        |                                               |
        v                                               v
+-------------------------------+       +-------------------------------+
|   Core API Services           |       |   Asynchronous Workers        |
|  - Catalog & Search           |       |  - Payment Reconciliation     |
|  - Orders & Entitlements      |       |  - Subscription Sync          |
|  - Subscriptions & Cart       |       |  - Payout Processing          |
|  - Reader Progress & Library  |       |  - Email / Notification Queue |
|  - Author Studio & Royalties  |       |  - Expired Entitlement Worker |
|  - AI / Gemini Inference      |       |  - Dead Letter Queue (DLQ)    |
+---------------+---------------+       +---------------+---------------+
                |                                       |
                +-------------------+-------------------+
                                    |
        +---------------------------+---------------------------+
        |                           |                           |
        v                           v                           v
+---------------+           +---------------+           +---------------+
| PostgreSQL 16 |           | Redis 7       |           | Object Storage|
| Primary & RO  |           | Cache & Locks |           | S3/GCS Vault  |
| - Transactions|           | - API Cache   |           | - Encrypted   |
| - Audit Logs  |           | - Rate Buckets|           |   Manuscripts |
| - Entitlement |           | - Distr. Lock |           | - Signed URLs |
+---------------+           +---------------+           +---------------+
```

---

## 2. Clean Architecture Layers

1. **Presentation Layer**:
   - Built in **Jetpack Compose** using declarative UI, M3 dynamic themes, and unidirectional data flow (UDF).
   - ViewModels expose immutable `StateFlow` and handle UI intent dispatching.
   - Screen decomposition isolates feature boundaries: `HomeScreen`, `BookDetailsScreen`, `ReaderScreen`, `CartScreen`, `CheckoutScreen`, `SubscriptionScreen`, `AuthorEarningsScreen`, `AdminFinancialScreen`.

2. **Domain Layer**:
   - Pure Kotlin business logic and entities: `Book`, `User`, `Order`, `Entitlement`, `RoyaltyEntry`, `Subscription`, `AuthorWallet`.
   - Clear contract interfaces for all repositories.

3. **Data Layer**:
   - Room SQLite database for persistent offline cache.
   - Centralized repository pattern encapsulating local database and remote REST/HTTP data synchronization.
   - Reactive caching with tag-based invalidation.

4. **Core Security & Infrastructure**:
   - `SecurityManager`, `RbacController`, `SignedUrlManager`, `FileUploadValidator`, `RateLimiter`, `PrivacyManager`, `BackgroundJobEngine`, `MetricsCollector`, `StructuredLogger`, `HealthCheckManager`.

---

## 3. Scalability Roadmap: MVP to Millions of Users

| Scale Tier | Concurrent Users | Database Architecture | Caching & Storage | Ingress & Compute |
| :--- | :--- | :--- | :--- | :--- |
| **Tier 1 (MVP)** | 1k – 10k | Single PostgreSQL 16 (16GB RAM) | Redis Standalone (2GB) + S3 | Single Docker / Cloud Run Node (2-4 Replicas) |
| **Tier 2 (Growth)** | 50k – 250k | PostgreSQL Primary + 2 Read Replicas (PgBouncer pool) | Redis Sentinel / Cluster (8GB) + Cloudflare CDN | Multi-AZ Kubernetes Cluster (10-20 Pods) |
| **Tier 3 (Enterprise)**| 1M – 10M+ | Sharded PostgreSQL / Spanner + Partitioned Ledger | Distributed Redis Multi-Region + Global S3 Multi-Region | Kubernetes Auto-Scaling (HPA) + Envoy Mesh |
