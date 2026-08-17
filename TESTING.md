# BOOKORA — Testing Strategy & Quality Assurance

## 1. Multi-Layered Testing Architecture

```
                      +-------------------+
                      |   End-to-End CUJs |  (Checkout, Reading, Payouts)
                      +---------+---------+
                                |
                      +---------+---------+
                      | Integration Tests |  (Repositories, DB, Workers)
                      +---------+---------+
                                |
            +-------------------+-------------------+
            |                                       |
+-----------+-----------+               +-----------+-----------+
|    Unit Tests (JVM)   |               | Security & Hardening  |
|  - ViewModels & State |               |  - RBAC & Signed URLs |
|  - Domain Logics      |               |  - Magic Bytes & Rate |
+-----------------------+               +-----------------------+
```

---

## 2. Test Execution Commands

### Execute Local Unit & Security Suite
```bash
gradle :app:testDebugUnitTest
```

### Run Roborazzi Visual Regression Verification
```bash
gradle :app:verifyRoborazziDebug
```

---

## 3. Critical User Journey (CUJ) Test Scenarios

1. **Reader Purchase & Entitlement Journey**:
   - Register account -> Browse catalog -> Add to cart -> Apply coupon -> Checkout -> Verify Entitlement -> Open in Reader -> Track Progress.
2. **Author Studio & Payout Journey**:
   - Author login -> Upload manuscript -> Admin approval -> First customer purchase -> Royalty ledger credited -> Payout request submitted -> Admin payout approved.
3. **Security & Vulnerability Test Suite** (`SecurityAndHardeningTest.kt`):
   - NIST password validation & salted SHA-256 / Argon2 verification.
   - Account lockout after 5 consecutive failed attempts.
   - RBAC matrix enforcement across all 6 roles.
   - HMAC signed URL generation, expiration & tamper rejection.
   - File upload magic byte validation for PDF/EPUB vs fake executables.
   - Rate limiting token bucket enforcement (HTTP 429).
   - Privacy manager PII anonymization vs legal ledger preservation.
   - Background worker idempotency & exponential backoff.
   - Structured logger PII / Secret masking.

---

## 4. Load Testing Plan (k6 / JMeter Scenarios)

| Scenario | Virtual Users (VU) | Target RPS | Max P95 Latency | Error Threshold |
| :--- | :---: | :---: | :---: | :---: |
| **Homepage Catalog Browsing** | 500 VUs | 2,000 req/s | < 80 ms | < 0.1% |
| **Book Search & Autocomplete** | 250 VUs | 1,000 req/s | < 120 ms | < 0.2% |
| **Direct Checkout & Payment** | 100 VUs | 150 req/s | < 350 ms | < 0.05% |
| **Signed URL Download Handshake** | 200 VUs | 500 req/s | < 50 ms | < 0.01% |
