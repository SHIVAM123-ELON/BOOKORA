# BOOKORA v1.0 — Staging to Public Launch Pipeline (13 Steps)

This document details the step-by-step execution protocol for taking BOOKORA from the completed code state to a live Google Play Store public launch.

---

## 1. Staging Deployment
- **Target Architecture**: AWS ECS Fargate / GCP Cloud Run connected to Staging PostgreSQL and Redis.
- **Environment Isolation**: Staging endpoints configured (`https://api-staging.bookora.com`).
- **Configuration**:
  ```bash
  # Deploy backend staging environment
  docker compose -f docker-compose.staging.yml up -d --build
  ```
- **Validation**: Verify `/health` (liveness) and `/ready` (readiness) endpoints return HTTP 200 `UP`.

---

## 2. Database Migration + Backup Test
- **Migration Execution**: Apply sequential SQL migrations `V1__init.sql` through `V4__financial_ledgers.sql`.
- **Integrity Validation**: Execute automated schema check to verify all foreign key constraints and composite indexes.
- **Backup Verification**:
  ```bash
  # Test PostgreSQL snapshot creation and restoration on staging
  pg_dump -Fc $STAGING_DATABASE_URL > /backups/staging_backup_test.dump
  pg_restore -C -d staging_restore_verify /backups/staging_backup_test.dump
  ```
- **Target SLA**: Point-in-time recovery (PITR) RPO < 15 minutes, RTO < 30 minutes.

---

## 3. Real Payment Sandbox Test
- **Gateway**: Stripe / Razorpay Sandbox Mode.
- **Test Scenarios**:
  1. Successful single-book purchase via Card / UPI Test IDs.
  2. Subscriptions recurring monthly & annual charge lifecycle.
  3. Insufficient funds / 3D Secure failure handling (graceful state transitions).
  4. Idempotency test: Multiple rapid tap checks preventing duplicate charge.
  5. Webhook listener verification (`payment_intent.succeeded` & `charge.refunded`).

---

## 4. Real E-Book Storage + Entitlement Test
- **Object Storage**: Private AWS S3 bucket / Google Cloud Storage bucket with IAM least-privilege access.
- **Upload Validation**: Author uploads EPUB/PDF → Magic-byte verification passes → File encrypted at rest (AES-256).
- **Entitlement Flow**:
  1. Reader purchases book → Entitlement record committed.
  2. App requests download → Backend validates entitlement and generates 15-minute HMAC-SHA256 signed URL.
  3. Direct download to Android local protected storage → Offline Reader opens seamlessly.

---

## 5. Gemini Production Configuration
- **API Setup**: Inject live production Gemini API key via Secrets Manager / `BuildConfig.GEMINI_API_KEY`.
- **Budget & Guardrails**:
  - `AiCostController` daily cap: 1,000 requests / day (adjustable by Owner).
  - Per-user rate limit: 50 requests / user / day.
  - Fallback engine: When offline or over quota, graceful local summary outlines render.

---

## 6. Android Release Build
- **Build Type**: Release Android App Bundle (`.aab`) with R8 full-mode shrinking and ProGuard obfuscation.
- **Commands**:
  ```bash
  gradle :app:bundleRelease
  ```
- **Verification**:
  - Debug logs suppressed (`StructuredLogger.isRelease = true`).
  - Mock payment interfaces and developer debug menus excluded.
  - APK/AAB size optimized (< 15MB base download).

---

## 7. Security + UAT (User Acceptance Testing)
- **Security Verification**:
  - RBAC permission enforcement across all 6 roles (Super Admin, Finance, Moderator, Author, Reader, Support).
  - Rate limiting verification (Token bucket throttles auth, payments, AI, and downloads).
  - NIST password salting, account lockout (5 failed attempts / 15-min lock), and GDPR PII erasure.
- **Critical UAT Journeys**:
  - Reader: Registration → Search → Buy → Read → AI Assistant → Review.
  - Author: Manuscript Submission → Review → Publish → Royalty Ledger → Payout.
  - Admin: Content Moderation → Refund Processing → Audit Log Verification.

---

## 8. Closed Beta
- **Platform**: Google Play Console Closed Testing Track (Alpha/Beta).
- **Target Audience**: 20–100 initial test readers and authors.
- **Feedback Loop**: In-app Help Center & crash telemetry monitoring.

---

## 9. Fix Beta Issues
- **Monitoring**: Real-time error monitoring via `StructuredLogger` and incident queue.
- **Triage**: High-priority UX / edge-case bug fixes committed and verified in staging before hotfix release.

---

## 10. Final Production Smoke Test
- **Sanity Verification**:
  - Verify live DNS (`api.bookora.com`).
  - Run end-to-end synthetic transaction on live production database.
  - Verify `StatusPageService` displays all green across Storefront, Payments, Reader, AI, and Notifications.

---

## 11. Google Play Submission
- **Checklist**:
  - Upload signed `app-release.aab`.
  - Submit Play Store listing copy, icon, feature graphic, and screenshots.
  - Submit completed Data Safety questionnaire (`DATA_SAFETY.md`).
  - Provide public Privacy Policy URL (`https://bookora.com/privacy`).
  - Submit IARC Content Rating questionnaire.

---

## 12. Public Launch 🚀
- **Staged Rollout**: Release to 10% → 25% → 50% → 100% of Android users in target markets.
- **Live Ops**: Monitor DAU, conversion rate, crash-free session rate (> 99.5%), and payment success rate.

---

## 13. BOOKORA v1.0
- **Operational Milestone**: Stable, production-ready, globally scalable digital marketplace and reading ecosystem.
