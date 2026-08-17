# BOOKORA — Enterprise Digital Book Marketplace, Reader & AI Ecosystem

BOOKORA is an enterprise-scale, production-hardened digital book marketplace and reading ecosystem built with modern Kotlin, Jetpack Compose, Clean Architecture, and a resilient distributed backend architecture.

---

## 🏛 Architecture Overview

```
├── /app                       # Android Client (Jetpack Compose, Clean Architecture, Room, DataStore)
│   ├── src/main/java/com/example
│   │   ├── core               # SecurityManager, RBAC, SignedUrls, RateLimiter, Cache, Worker, Observability
│   │   ├── domain             # Domain Models (Book, User, Order, Royalty, Subscription, Entitlement)
│   │   ├── data               # Room Database, DAOs, Entitlements, Repositories
│   │   └── presentation       # UI Composables, ViewModels, Financial/Author/Admin Hubs, Reader
│   └── src/test/java/com/example # Unit, Security & Hardening Tests
├── /apps                      # Backend Services
│   └── /backend               # NestJS Enterprise API Gateway & Microservice Workers
├── /infrastructure            # Production DevOps, Containerization & Database
│   ├── /ci-cd                 # GitHub Actions multi-stage CI/CD workflow
│   ├── /docker                # Dockerfile and production/dev docker-compose
│   └── /database              # Flyway/SQL Migrations (V1-V4), PITR backup/restore scripts
└── /docs                      # Enterprise Documentation Suite
```

---

## 🚀 Key Modules Across Phases 1–6

### 1. Reader Marketplace & Digital Reader (Phases 1–3)
- **Marketplace & Discovery**: Multi-faceted catalog browsing, curated carousels, genre tags, and instant search.
- **Kindle-Style Reader**: Offline-capable reading with page progression tracking, custom typography, font sizing, line height, themes (Light, Sepia, Obsidian Dark), and reading bookmarks.
- **Author Studio & Admin Operations**: Manuscript drafting, publication review queue, content moderation, and platform revenue metrics.

### 2. AI & Gemini Intelligence (Phase 4)
- **Executive AI Book Summaries**: Distilled chapter briefs, thematic breakdown, and key takeaways.
- **Reading Assistant**: In-context reading Q&A, character relationship tracking, and concept explanations.
- **Semantic & Intent Search**: Natural language book discovery and mood-based recommendations.
- **Author AI Writing Assistant**: Blurb generation, chapter outline brainstorming, and copy editing.

### 3. Financial, Monetization & Entitlements (Phase 5)
- **Cart & Direct Checkout**: Multi-item cart, coupon validation, promo codes, and multi-rail payment support.
- **Digital Entitlements Engine**: Instant access provisioning, order receipts, and automated refund processing.
- **Subscriptions & VIP Club**: Tiered recurring memberships (*Bookora Unlimited*, *VIP Scholar*).
- **Author Royalty Ledger & Wallets**: Immutable accounting ledger, platform fee splits, author balance tracking, and payout disbursement workflows.
- **Admin Financial Operations**: Gross Merchandise Value (GMV) tracking, payout approvals, and refund resolutions.

### 4. Production Hardening, Security, DevOps & Scale (Phase 6)
- **Enterprise Security**: Strict RBAC across 6 roles (`READER`, `AUTHOR`, `PUBLISHER`, `MODERATOR`, `ADMIN`, `SUPER_ADMIN`), NIST password policies, account lockout, token revocation, signed download URLs, and magic byte file upload validation.
- **High-Performance Caching & Rate Limiting**: Tagged multi-tier cache invalidation, token-bucket rate limiting per endpoint category, and database index optimizations.
- **Observability & Health Checks**: Structured JSON logs with automated secret redaction, `/health` and `/ready` probes, metric collection (p95 latency, error rates, cache hits), and real-time threshold alerting.
- **Resilient Background Jobs**: Idempotent workers for payment reconciliation, subscription renewals, payout execution, and dead-letter queues (DLQ).
- **Disaster Recovery & CI/CD**: GPG-encrypted automated PostgreSQL backups, Point-In-Time Recovery (PITR), and 6-stage GitHub Actions CI/CD pipeline.

---

## 🛠 Local Development & Verification

### Running Android Tests
```bash
gradle :app:testDebugUnitTest
```

### Running Application Verification
```bash
gradle :app:assembleDebug
```

---

## 📚 Complete Production Documentation Suite
- [System Architecture (ARCHITECTURE.md)](./ARCHITECTURE.md)
- [Security & RBAC Matrix (SECURITY.md)](./SECURITY.md)
- [Deployment & Production Infrastructure (DEPLOYMENT.md)](./DEPLOYMENT.md)
- [Disaster Recovery & PITR (DISASTER_RECOVERY.md)](./DISASTER_RECOVERY.md)
- [REST API & Rate Limiting (API.md)](./API.md)
- [Environment Configuration (ENVIRONMENT.md)](./ENVIRONMENT.md)
- [Database Schema & Migrations (DATABASE.md)](./DATABASE.md)
- [Testing & Load Test Strategy (TESTING.md)](./TESTING.md)
