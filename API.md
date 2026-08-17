# BOOKORA — REST API Specification & Rate Limiting Guidelines

## 1. Global API Standards

- **Base URL**: `https://api.bookora.com/v1`
- **Authentication**: `Authorization: Bearer <JWT_ACCESS_TOKEN>`
- **Content-Type**: `application/json`
- **Request Tracing**: Every response includes `X-Request-ID: req_xxxxxx` for distributed tracing.
- **Security Headers**:
  - `Strict-Transport-Security: max-age=63072000; includeSubDomains; preload`
  - `X-Content-Type-Options: nosniff`
  - `X-Frame-Options: DENY`
  - `Content-Security-Policy: default-src 'none'; frame-ancestors 'none'`

---

## 2. Rate Limiting Tiers (HTTP 429 Too Many Requests)

| Category | Endpoint Scope | Limit | Retry-After Header |
| :--- | :--- | :---: | :---: |
| **Authentication** | `/auth/login`, `/auth/register`, `/auth/reset-password` | **5 req / min** | `Retry-After: 60` |
| **Payments / Checkout** | `/orders/checkout`, `/payments/process`, `/refunds` | **10 req / min** | `Retry-After: 60` |
| **AI / Gemini Inference** | `/ai/summary`, `/ai/assistant`, `/ai/recommendations` | **15 req / min** | `Retry-After: 60` |
| **Digital Downloads** | `/books/{id}/download-url` | **20 req / min** | `Retry-After: 60` |
| **Admin Operations** | `/admin/*` | **30 req / min** | `Retry-After: 60` |
| **Search Queries** | `/books/search`, `/books/semantic-search` | **60 req / min** | `Retry-After: 60` |
| **General Catalog** | `/books`, `/categories`, `/authors` | **120 req / min** | `Retry-After: 60` |

---

## 3. Standard Response & Error Format

### Standard Success Response with Cursor Pagination
```json
{
  "success": true,
  "data": [ ... ],
  "pagination": {
    "nextCursor": "eyJpZCI6ImJvb2tfMTIzIn0=",
    "hasMore": true,
    "limit": 20
  },
  "requestId": "req_8f1c4e9b2a1d0001"
}
```

### Standard Error Response (Sanitized — Zero Stack Trace Exposure)
```json
{
  "success": false,
  "error": {
    "code": "INVALID_COUPON_CODE",
    "message": "The promotional code 'SUMMER50' has expired or reached usage limits.",
    "statusCode": 400
  },
  "requestId": "req_8f1c4e9b2a1d0001",
  "timestamp": "2026-08-17T08:00:00Z"
}
```

---

## 4. Key Endpoint Reference

### Authentication
- `POST /v1/auth/register` — Create user account with NIST password validation.
- `POST /v1/auth/login` — Authenticate and receive rotated JWT & refresh token.
- `POST /v1/auth/logout` — Revoke token across all active device sessions.

### Books & Marketplace
- `GET /v1/books` — Paginated book catalog with search, genre, and sort filters.
- `GET /v1/books/{id}` — Retrieve book details and preview metadata.
- `GET /v1/books/{id}/download-url` — Generate short-lived HMAC-signed download URL for entitled readers.

### Orders & Financials
- `POST /v1/orders/checkout` — Idempotent order placement and payment execution.
- `GET /v1/orders/history` — Browse authenticated user's receipts and entitlement statuses.
- `POST /v1/payouts/request` — Author payout withdrawal request from available wallet balance.

### Health & Observability
- `GET /health` — Liveness probe (Returns HTTP 200 `{ "status": "UP" }`).
- `GET /ready` — Readiness probe checking DB, Redis, Storage, and AI services.
