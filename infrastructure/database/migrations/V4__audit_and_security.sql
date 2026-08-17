-- ====================================================================
-- BOOKORA V4: Admin Compliance Audit Logs & Security Token Revocations
-- ====================================================================

CREATE TABLE IF NOT EXISTS admin_audit_logs (
    id VARCHAR(64) PRIMARY KEY,
    actor_admin_id VARCHAR(64) NOT NULL REFERENCES users(id),
    action VARCHAR(64) NOT NULL, -- BOOK_APPROVED, USER_SUSPENDED, REFUND_APPROVED, etc.
    target_entity VARCHAR(64) NOT NULL,
    target_entity_id VARCHAR(64) NOT NULL,
    request_id VARCHAR(64) NOT NULL,
    ip_address VARCHAR(45),
    user_agent TEXT,
    metadata_json TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS revoked_tokens (
    token_id VARCHAR(128) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    revoked_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_audit_actor ON admin_audit_logs(actor_admin_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_action ON admin_audit_logs(action);
CREATE INDEX IF NOT EXISTS idx_audit_request ON admin_audit_logs(request_id);
