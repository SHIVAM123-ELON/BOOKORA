# BOOKORA — Enterprise Security & Compliance Policy

## 1. Security Architecture & Threat Model

Bookora adheres to strict defense-in-depth principles across client, network, API, and storage layers:
- **Least Privilege Principle**: All operations require explicit backend verification against user roles and permissions.
- **Zero Client Trust**: Frontend UI restrictions are never relied upon for security; all data mutations and queries are enforced server-side.
- **Fail-Secure Defaults**: Any unidentified role, missing authorization token, or invalid signature fails closed.

---

## 2. RBAC Permission Matrix

| Role | Hierarchy | Allowed Permissions | Description |
| :--- | :---: | :--- | :--- |
| **READER** | Level 1 | `book:read`, `library:read`, `review:create`, `cart:manage`, `order:create`, `refund:request` | End consumer reading books, managing cart, and purchasing. |
| **AUTHOR** | Level 2 | Reader permissions + `book:create`, `book:update`, `sales:read`, `payout:request`, `studio:access` | Content creator managing publication drafts and royalties. |
| **PUBLISHER**| Level 3 | Author permissions + `bulk:publish`, `catalog:analytics` | Multi-author imprint management and sales analytics. |
| **MODERATOR**| Level 4 | Reader permissions + `review:moderate`, `report:resolve` | Content safety, review approvals, and dispute resolution. |
| **ADMIN** | Level 5 | `book:approve`, `book:reject`, `user:suspend`, `refund:approve`, `payout:approve`, `coupon:manage`, `subscription:manage`, `audit:read` | Business operations, financial reconciliations, publication approvals. |
| **SUPER_ADMIN**| Level 6 | **All System Permissions** including `commission:change`, `system:configure`, `featureflag:manage`, `admin:manage` | Platform owners and system administrators. |

---

## 3. Authentication & Credential Hardening

- **Password Standards (NIST SP 800-63B)**: Minimum 10 characters, upper/lowercase, digit, and special character enforcement.
- **Cryptographic Hashing**: Salted SHA-256 (client simulation) and Argon2id (production backend) with random 16-byte secure salts.
- **Brute-Force & Lockout Protection**: 5 consecutive failed login attempts locks the targeted account for 15 minutes with exponential backoff.
- **Token Security**: Opaque refresh tokens with automatic rotation upon reuse, short-lived JWT access tokens (15–60 min), and global token revocation for instant session termination across all devices.

---

## 4. Digital Asset Protection & Signed URLs

- **E-Book File Vault**: All PDF and EPUB manuscripts are housed in private object storage buckets with zero public access.
- **HMAC Signed URLs**: Authorized readers receive temporary, tamper-proof signed URLs valid for 15 minutes.
- **Dynamic DRM Watermarking**: Download streams inject user identity metadata (`Licensed to: <userId>`) to discourage illicit distribution.
- **Magic Byte Validation**: File uploads undergo binary header inspection (`%PDF-`, `PK..` EPUB ZIP container, `PNG`, `JPEG`) to prevent polyglot malware attacks.

---

## 5. OWASP Top 10 Defenses

1. **Injection (SQL/Command)**: Parameterized queries, ORM abstraction, and strict input sanitization.
2. **Broken Authentication**: Multi-factor ready, session revocation, rate-limited auth endpoints (5 req/min).
3. **Sensitive Data Exposure**: Zero plaintext secret storage, automated secret masking in logs, TLS 1.3 in transit.
4. **Security Misconfiguration**: Production Fail-Fast validator blocking mock payments or placeholder JWT secrets.
5. **IDOR (Insecure Direct Object References)**: Contextual ownership checks (`verifyResourceOwnership`) for all book, order, and library modifications.

---

## 6. Responsible Vulnerability Disclosure
For security concerns or vulnerability disclosures, please email `security@bookora.com` with reproducible steps.
