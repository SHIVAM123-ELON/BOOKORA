# BOOKORA — Disaster Recovery & Business Continuity Plan

## 1. Objectives: RPO & RTO

- **Recovery Point Objective (RPO)**: **< 15 minutes** (Maximum allowable data loss in catastrophic event, achieved via Continuous WAL Archiving + Automated Snapshots).
- **Recovery Time Objective (RTO)**: **< 30 minutes** (Maximum allowable downtime to restore full customer service).

---

## 2. Backup Strategy & Retention Policy

| Backup Type | Frequency | Storage Location | Retention Period |
| :--- | :--- | :--- | :--- |
| **Transaction Logs (WAL)** | Continuous / Every 5 min | Multi-region S3 Vault | 14 Days |
| **Full Database Snapshot** | Daily at 02:00 UTC | Encrypted Offsite S3 Bucket | 30 Days |
| **Weekly Deep Archive** | Weekly (Sunday) | AWS Glacier / Cold Storage | 1 Year |
| **Financial Ledger Archive** | Monthly Snapshot | Immutable WORM Compliance Storage | 7 Years (Legal mandate) |

---

## 3. Disaster Scenarios & Recovery Playbooks

### Scenario A: Database Cluster Outage / Data Corruption
1. **Diagnosis**: AlertingEngine triggers `SYS_ALERT_CRITICAL: Database connection failed`.
2. **Action**:
   - If primary node failed: Trigger automatic RDS/Cloud SQL Multi-AZ failover to standby replica (RTO < 2 min).
   - If catastrophic corruption: Execute Point-in-Time Recovery (PITR) using `infrastructure/database/backup_and_restore.sh restore /var/backups/bookora/postgres/<latest_valid_backup>.sql.gz.gpg`.
3. **Verification**: Run `/ready` health check and execute sample read query.

### Scenario B: Object Storage Outage
1. **Diagnosis**: E-book file downloads or cover art return HTTP 500/503.
2. **Action**:
   - Flip Cloudflare origin DNS to Secondary Cross-Region Bucket Replica.
   - Degrade reading gracefully: Cached books in reader offline memory remain fully readable.

### Scenario C: Payment Gateway Outage
1. **Diagnosis**: Payment verification webhook error spike.
2. **Action**:
   - Order service enters Queueing Mode: Orders are stored with `PENDING_RECONCILIATION` status.
   - Asynchronous worker `PaymentReconciliationJob` retries status polling with exponential backoff once the gateway is restored.

### Scenario D: Redis Cluster Crash
1. **Diagnosis**: Cache miss spike or rate limiting degradation.
2. **Action**:
   - Application automatically degrades to in-memory caching mode without crashing core database transactions.
   - Restart Redis container via `docker compose restart redis` or provision new Redis cluster instance.
