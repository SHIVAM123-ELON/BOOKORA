# BOOKORA — Google Play Store Data Safety & Privacy Inventory

## 1. Data Collection & Sharing Summary

| Data Category | Data Type | Collected | Shared with 3rd Party | Purpose | Optional or Required |
| :--- | :--- | :---: | :---: | :--- | :--- |
| **Personal Info** | Name, Email Address | Yes | No | Account management, authentication, order receipts | Required |
| **Financial Info** | Payment history, Order amounts | Yes | Payment Processor (Stripe/Gateway) | Fraud prevention, payment processing, tax invoices | Required for purchase |
| **App Activity** | Book views, Reading progress, Search history | Yes | No | Analytics, personalization, reading progress sync | Optional (User can opt out) |
| **App Info & Performance**| Crash logs, Diagnostics | Yes | Sentry/Monitoring | Bug fixes, platform stability | Required |
| **Device or Other IDs** | Device installation ID, Push Token | Yes | Firebase FCM | Transactional push notifications | Optional |

---

## 2. Security Practices

- **Data Encrypted in Transit**: Yes (All network traffic enforced over TLS 1.3 / HTTPS).
- **Data Encrypted at Rest**: Yes (AES-256 for e-book files and database snapshots).
- **User Account Deletion**: Yes (Users can request complete account erasure via Settings -> Delete Account).
- **Children's Privacy (COPPA)**: Bookora does not intentionally collect information from children under 13 without verified parental consent.

---

## 3. Data Retention & Deletion Policy

- **Account Data**: Deleted within 30 days of account deletion request.
- **Financial & Tax Records**: Retained in anonymized format for 7 years to comply with statutory accounting requirements.
- **Reading Progress & Wishlists**: Cleared immediately upon account deletion.
