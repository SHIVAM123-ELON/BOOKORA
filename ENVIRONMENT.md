# BOOKORA — Environment Separation & Secret Management

## 1. Environment Topology

Bookora maintains strict physical and logical boundary separation across three tiers:

```
[ Local / Development ]   --->   [ Staging Cluster ]   --->   [ Production Multi-AZ ]
- Local SQLite / Docker          - Staging PostgreSQL          - Clustered PostgreSQL 16
- In-memory cache / Local Redis  - Staging Redis Sentinel      - Redis 7 Cluster (TLS)
- Mock payment allowed           - Stripe Test Mode (sk_test)  - Live Stripe Gateway (sk_live)
- Degraded / Local AI allowed    - Gemini Test Project         - Production Gemini Enterprise
- Hot Reloading & Debug Logs     - Structured JSON Logs        - Structured Logs + Sentry
```

---

## 2. Fail-Fast Production Rules

The `ProductionConfig` validator enforces zero-tolerance startup checks in production mode:
1. **No Mock Payments**: `ALLOW_MOCK_PAYMENTS=true` causes immediate crash loop on startup.
2. **No Test Keys**: `STRIPE_SECRET_KEY` starting with `sk_test_` is rejected in production.
3. **Cryptographic JWT Secret**: Secrets shorter than 32 characters or containing placeholder tokens are rejected.
4. **No Localhost DB**: Production database URLs pointing to `localhost` or `127.0.0.1` are rejected.

---

## 3. Secret Management Matrix

| Secret Name | Purpose | Development Default | Production Storage |
| :--- | :--- | :--- | :--- |
| `JWT_SECRET` | Token signature | `dev_secret_key_123` | AWS Secrets Manager / Vault |
| `SIGNED_URL_SECRET` | HMAC Download signature | `dev_signed_secret_123` | AWS Secrets Manager / Vault |
| `DATABASE_URL` | PostgreSQL connection string | `localhost:5432` | Kubernetes Encrypted Secret |
| `REDIS_URL` | Redis connection string | `localhost:6379` | Kubernetes Encrypted Secret |
| `STRIPE_SECRET_KEY` | Payment processing | `sk_test_mock` | Cloud Secret Manager (Restricted IAM) |
| `GEMINI_API_KEY` | AI inference | AI Studio Secrets panel | AI Studio Secrets / Secret Manager |
