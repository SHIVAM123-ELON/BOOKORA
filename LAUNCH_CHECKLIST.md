# BOOKORA — Production Launch Checklist

This checklist tracks all core operational, technical, legal, and store submission criteria required before opening Bookora to the general public.

---

## 1. Codebase & Build Architecture

| Item | Status | Verification & Action |
| :--- | :---: | :--- |
| **Clean Architecture Separation** | **READY** | MVVM + Clean Architecture repository separation verified. |
| **Room Database & Type Converters**| **READY** | Local DB migrations and schema tables compiled. |
| **ProGuard / R8 Optimization** | **READY** | Obfuscation rules and reflection guards active in `proguard-rules.pro`. |
| **Debug Logs Suppression** | **READY** | `StructuredLogger` filters debug statements in release builds. |
| **No Hardcoded Secrets** | **READY** | Secrets Gradle Plugin + `.env` integration active. |

---

## 2. Security & Data Protection

| Item | Status | Verification & Action |
| :--- | :---: | :--- |
| **NIST Password Policy & Salting** | **READY** | Cryptographic salted hashing verified. |
| **Account Lockout (5 Fails / 15m)**| **READY** | Brute-force throttling unit tested. |
| **Role-Based Access Control (RBAC)**| **READY** | 6-tier permission enforcement active. |
| **HMAC Signed E-Book Downloads** | **READY** | Short-lived 15-min signed URLs with tamper verification. |
| **Magic Byte Binary Upload Check** | **READY** | Header validation for PDF/EPUB vs disguise scripts. |
| **Rate Limiting (Tiered Bucket)** | **READY** | Token bucket rate limiting on auth, payments, AI, downloads. |
| **GDPR / CCPA Account Deletion** | **READY** | Anonymizes PII while retaining statutory accounting entries. |
| **External Security Audit** | **REQUIRES OWNER ACTION** | Professional third-party penetration test recommended. |

---

## 3. Financials, Payments & Author Payouts

| Item | Status | Verification & Action |
| :--- | :---: | :--- |
| **Idempotent Order Transactions** | **READY** | Double-charge prevention verified via idempotency keys. |
| **Coupon Engine & Rules** | **READY** | Minimum spend, category scoping & expiration enforced. |
| **Subscription Billing Engine** | **READY** | Monthly/Annual/Student tiers with auto-renewal state. |
| **Author Royalty Ledger & Splits** | **READY** | 70/30, 80/20, 85/15 tiered revenue splits calculated. |
| **Production Payment Gateway** | **REQUIRES OWNER ACTION** | Inject live production `STRIPE_SECRET_KEY` into Secrets Manager. |
| **Bank KYC & Stripe Connect** | **REQUIRES OWNER ACTION** | Configure production Stripe Connect webhook endpoints for Indian banking (UPI/IMPS/NEFT). |

---

## 4. Google Play Store Submission

| Item | Status | Verification & Action |
| :--- | :---: | :--- |
| **Application ID & Target SDK** | **READY** | Set to `com.aistudio.bookora.app`, Target SDK 36 (Android 15/16). |
| **Adaptive Launcher Icon** | **READY** | Adaptive vector icon configured in `AndroidManifest.xml`. |
| **Release Signing Key** | **REQUIRES OWNER ACTION** | Generate upload keystore and store credentials in GitHub Actions Secrets. |
| **Play Store Data Safety Form** | **READY** | Filled in `DATA_SAFETY.md`. |
| **Privacy Policy Public URL** | **REQUIRES OWNER ACTION** | Host `LEGAL_AND_POLICIES.md` on `https://bookora.com/privacy`. |
| **Feature Graphic & Screenshots** | **REQUIRES OWNER ACTION** | Upload 1024x500 feature graphic & tablet/phone screenshots to Google Play Console. |

---

## 5. Operations & Incident Readiness

| Item | Status | Verification & Action |
| :--- | :---: | :--- |
| **Liveness & Readiness Probes** | **READY** | `/health` and `/ready` probes operational. |
| **Public Status Page** | **READY** | `StatusPageService` tracking service degradation states. |
| **Automated Encrypted Backups** | **READY** | PostgreSQL WAL archive & snapshot script ready. |
| **Disaster Recovery Playbooks** | **READY** | `DISASTER_RECOVERY.md` detailing RPO < 15m, RTO < 30m. |
| **Production Runbook** | **READY** | `RUNBOOK.md` detailing deployment, rollback & mitigation steps. |
| **Support Helpdesk & DMCA Triage**| **READY** | `SupportService` & `CopyrightModerationService` active. |
