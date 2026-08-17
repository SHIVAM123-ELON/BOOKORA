# BOOKORA — Production Deployment & DevOps Runbook

## 1. Production Architecture Overview

The Bookora production stack is containerized and deployable on AWS (ECS/EKS), Google Cloud (Cloud Run / GKE), or standard Docker Swarm:

```
                  Internet / Mobile App
                           |
                     Cloudflare WAF
                           | (SSL Termination)
             Load Balancer / Ingress Controller
                           |
        +------------------+------------------+
        |                                     |
 Backend Replicas (Node 1)          Backend Replicas (Node 2)
        |                                     |
        +------------------+------------------+
                           |
     +---------------------+---------------------+
     |                     |                     |
PostgreSQL Primary    Redis Cluster       S3/GCS Object Vault
```

---

## 2. Prerequisites & Infrastructure Setup

1. **PostgreSQL 16 High-Availability Database** (e.g. AWS RDS or GCP Cloud SQL) with automatic backups enabled.
2. **Redis 7 Cluster** (e.g. AWS ElastiCache or GCP Memorystore) with TLS and authentication enabled.
3. **Cloud Object Storage** (AWS S3 Bucket or GCP Cloud Storage Bucket) configured as private with CORS restricted to Bookora domains.
4. **Domain & SSL Certificate** configured with Let's Encrypt or AWS ACM.

---

## 3. Environment Secret Configuration

Create production `.env` (or inject secrets into Kubernetes Secrets / AWS Secrets Manager):

```bash
APP_ENV=production
PORT=8080
LOG_LEVEL=info
DATABASE_URL=postgresql://bookora_admin:<SECURE_PASSWORD>@db.production.internal:5432/bookora_prod
REDIS_URL=redis://:<REDIS_PASSWORD>@redis.production.internal:6379/0
JWT_SECRET=<CRYPTOGRAPHIC_RANDOM_32_CHAR_STRING>
SIGNED_URL_SECRET=<CRYPTOGRAPHIC_RANDOM_32_CHAR_STRING>
STRIPE_SECRET_KEY=sk_live_...
STRIPE_WEBHOOK_SECRET=whsec_...
ALLOW_MOCK_PAYMENTS=false
GEMINI_API_KEY=<LIVE_GEMINI_KEY>
```

---

## 4. Zero-Downtime Rolling Deployment Steps

1. **Step 1: Database Migration Execution**
   ```bash
   # Run backward-compatible schema migrations before starting new application containers
   psql -h $DB_HOST -U $DB_USER -d $DB_NAME -f infrastructure/database/migrations/V1__initial_schema.sql
   psql -h $DB_HOST -U $DB_USER -d $DB_NAME -f infrastructure/database/migrations/V2__financial_tables.sql
   psql -h $DB_HOST -U $DB_USER -d $DB_NAME -f infrastructure/database/migrations/V3__performance_indexes.sql
   psql -h $DB_HOST -U $DB_USER -d $DB_NAME -f infrastructure/database/migrations/V4__audit_and_security.sql
   ```

2. **Step 2: Start Container Replicas**
   ```bash
   cd infrastructure/docker
   docker compose -f docker-compose.prod.yml up -d --build
   ```

3. **Step 3: Verify Health Probes**
   ```bash
   curl -f http://localhost:8080/health
   curl -f http://localhost:8080/ready
   ```

4. **Step 4: Shift Ingress Traffic & Drain Old Replicas**
   Route 100% of live traffic to the healthy new revision.
