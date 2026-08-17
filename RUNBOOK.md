# BOOKORA — Production Operations Runbook

## 1. Zero-Downtime Deployment Procedure

```bash
# 1. Pull latest verified release tag
git checkout tags/v1.0.0

# 2. Run test and lint verification
gradle :app:testDebugUnitTest

# 3. Build optimized Release App Bundle (AAB)
gradle :app:bundleRelease

# 4. Deploy Backend & Run Database Migrations
docker compose -f docker-compose.prod.yml up -d --build
```

---

## 2. Emergency Rollback Procedure

If a critical crash spike (> 1%) or payment failure spike occurs immediately post-deployment:

1. **Feature Flag Kill-Switch**:
   - In Admin Panel: Flip problematic flag (e.g., `NEW_CHECKOUT`, `AI_ASSISTANT`, `SUBSCRIPTIONS`) to `OFF`.
   - Propagates to client instances immediately without code redeployment.
2. **Backend Blue-Green Switch**:
   ```bash
   # Switch load balancer upstream to previous stable container group
   nginx -s reload
   ```
3. **Database Migration Rollback**:
   - Apply rollback migration script: `psql $DATABASE_URL -f infrastructure/database/migrations/undo_V4.sql`
4. **Google Play Console Release Halting**:
   - In Google Play Console -> Production -> Releases -> **Halt rollout**.

---

## 3. Incident Mitigation Playbooks

### A. Database Outage / Connection Exhaustion
1. **Symptom**: `/ready` probe returns HTTP 503 `DB_DOWN`.
2. **Action**:
   - Check connection pool metrics (`MetricsCollector.getStats()`).
   - If PostgreSQL crashed: Trigger Multi-AZ failover to read-replica.
   - If deadlocks occurred: Run `SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE state = 'idle in transaction';`.

### B. Payment Gateway Failure / Webhook Outage
1. **Symptom**: AlertingEngine triggers `SYS_ALERT_CRITICAL: High payment failure rate`.
2. **Action**:
   - Check Stripe status dashboard.
   - Switch orders to `PENDING_RECONCILIATION` queue.
   - `PaymentReconciliationJob` automatically retries verification via exponential backoff.
   - Update `StatusPageService.updateServiceStatus("Payments & Checkout", DEGRADED_PERFORMANCE)`.

### C. AI Provider Quota Exhaustion / Outage
1. **Symptom**: Gemini API returns HTTP 429 / 503.
2. **Action**:
   - `AiCostController` automatically activates graceful fallback summary algorithms.
   - The reader screen displays offline/cached chapter outlines without blocking reading experience.

### D. Object Storage / CDN Outage
1. **Symptom**: Cover images or e-book downloads fail with HTTP 500.
2. **Action**:
   - Flip DNS to secondary regional S3 bucket replica.
   - Readers with cached offline e-books continue reading uninterrupted.
