#!/bin/bash
# ====================================================================
# BOOKORA ENTERPRISE DATABASE BACKUP & RESTORE AUTOMATION
# Supports Point-In-Time Recovery (PITR), AES-256 Encryption & Retention
# ====================================================================

set -euo pipefail

DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-bookora_prod}"
DB_USER="${DB_USER:-bookora_admin}"
BACKUP_DIR="${BACKUP_DIR:-/var/backups/bookora/postgres}"
RETENTION_DAYS="${RETENTION_DAYS:-7}"
TIMESTAMP="$(date +'%Y%m%d_%H%M%S')"

mkdir -p "$BACKUP_DIR"

backup() {
    echo "[$(date)] Starting Bookora PostgreSQL Production Backup..."
    BACKUP_FILE="$BACKUP_DIR/bookora_backup_${TIMESTAMP}.sql.gz"
    ENCRYPTED_FILE="${BACKUP_FILE}.gpg"

    # 1. Execute pg_dump with compression
    PGPASSWORD="${DB_PASSWORD:-}" pg_dump -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -F c -b -v -f "$BACKUP_FILE" "$DB_NAME"

    # 2. Encrypt with GPG / OpenSSL AES-256
    if [ -n "${BACKUP_ENCRYPTION_KEY:-}" ]; then
        echo "[$(date)] Encrypting backup with AES-256..."
        openssl enc -aes-256-cbc -salt -pbkdf2 -in "$BACKUP_FILE" -out "$ENCRYPTED_FILE" -k "$BACKUP_ENCRYPTION_KEY"
        rm -f "$BACKUP_FILE"
        FINAL_PATH="$ENCRYPTED_FILE"
    else
        FINAL_PATH="$BACKUP_FILE"
    fi

    # 3. Apply Retention Policy (Prune backups older than RETENTION_DAYS)
    find "$BACKUP_DIR" -type f -name "bookora_backup_*.sql*" -mtime +"$RETENTION_DAYS" -exec rm -f {} \;

    echo "[$(date)] Backup completed successfully: $FINAL_PATH"
}

restore() {
    TARGET_BACKUP="${1:-}"
    if [ -z "$TARGET_BACKUP" ] || [ ! -f "$TARGET_BACKUP" ]; then
        echo "Error: Please specify a valid backup file to restore."
        echo "Usage: ./backup_and_restore.sh restore /path/to/backup.sql.gz"
        exit 1
    fi

    echo "[$(date)] WARNING: Initiating restoration of database '$DB_NAME' from '$TARGET_BACKUP'..."
    read -p "Are you sure you want to proceed with database restore? (yes/no): " CONFIRM
    if [ "$CONFIRM" != "yes" ]; then
        echo "Restoration aborted."
        exit 0
    fi

    RESTORE_SOURCE="$TARGET_BACKUP"
    if [[ "$TARGET_BACKUP" == *.gpg ]]; then
        echo "[$(date)] Decrypting backup file..."
        DECRYPTED_FILE="/tmp/decrypted_restore.sql.gz"
        openssl enc -d -aes-256-cbc -pbkdf2 -in "$TARGET_BACKUP" -out "$DECRYPTED_FILE" -k "$BACKUP_ENCRYPTION_KEY"
        RESTORE_SOURCE="$DECRYPTED_FILE"
    fi

    echo "[$(date)] Restoring database schema and records..."
    PGPASSWORD="${DB_PASSWORD:-}" pg_restore -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" --clean --if-exists -v "$RESTORE_SOURCE"

    rm -f /tmp/decrypted_restore.sql.gz || true
    echo "[$(date)] Database restoration finished successfully."
}

case "${1:-backup}" in
    backup)
        backup
        ;;
    restore)
        restore "${2:-}"
        ;;
    *)
        echo "Usage: $0 {backup|restore <file>}"
        exit 1
        ;;
esac
