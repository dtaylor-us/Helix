#!/usr/bin/env bash
# ADR-022: runs on the Postgres VM via cron (installed by cloud-init, see
# infra/cloud-init/postgres-vm.yaml). Dumps the database and uploads it to the backups storage
# account using the VM's system-assigned managed identity -- no storage account key or connection
# string is ever stored on this VM.
#
# Configuration comes from /etc/helix/backup.env (written by cloud-init, not this script) rather
# than being templated into this file directly, so this script can be edited, tested, and
# shellchecked as plain bash independent of Terraform.
set -euo pipefail

ENV_FILE="/etc/helix/backup.env"
LOG_FILE="/var/log/helix-backup.log"

log() {
    echo "$(date -u '+%Y-%m-%dT%H:%M:%SZ') $*" | tee -a "$LOG_FILE"
}

if [[ ! -f "$ENV_FILE" ]]; then
    log "ERROR: $ENV_FILE not found -- cloud-init did not complete, or was edited incorrectly."
    exit 1
fi
# shellcheck source=/dev/null
source "$ENV_FILE"

: "${POSTGRES_DB:?POSTGRES_DB must be set in $ENV_FILE}"
: "${BACKUP_STORAGE_ACCOUNT:?BACKUP_STORAGE_ACCOUNT must be set in $ENV_FILE}"
: "${BACKUP_CONTAINER_NAME:?BACKUP_CONTAINER_NAME must be set in $ENV_FILE}"

TIMESTAMP="$(date -u '+%Y%m%dT%H%M%SZ')"
DUMP_FILE="/tmp/helix-backup-${TIMESTAMP}.dump"
BLOB_NAME="helix-backup-${TIMESTAMP}.dump"

cleanup() {
    rm -f "$DUMP_FILE"
}
trap cleanup EXIT

log "Starting backup of database '${POSTGRES_DB}'..."

# Custom format (-Fc) -- compressed by default and restorable selectively with pg_restore, unlike a
# plain SQL dump. Runs as the postgres OS user inside the container over the container's local Unix
# socket, which the official postgres image trusts without a password for local connections -- no
# credential needs to live in this script or in $ENV_FILE.
if ! docker exec -u postgres helix-postgres pg_dump -Fc "${POSTGRES_DB}" > "$DUMP_FILE"; then
    log "ERROR: pg_dump failed."
    exit 1
fi

DUMP_SIZE="$(stat -c%s "$DUMP_FILE")"
if [[ "$DUMP_SIZE" -lt 1024 ]]; then
    # A near-empty dump almost always means something went wrong upstream (e.g. the container
    # wasn't actually running, or the database was empty when it shouldn't be) -- refuse to upload
    # it silently as if it were a real backup.
    log "ERROR: dump file is suspiciously small (${DUMP_SIZE} bytes) -- refusing to upload. Investigate before the next scheduled run."
    exit 1
fi

log "Dump complete (${DUMP_SIZE} bytes). Authenticating to Azure via managed identity..."
if ! az login --identity --output none; then
    log "ERROR: managed identity login failed."
    exit 1
fi

log "Uploading ${BLOB_NAME} to ${BACKUP_STORAGE_ACCOUNT}/${BACKUP_CONTAINER_NAME}..."
if ! az storage blob upload \
    --auth-mode login \
    --account-name "${BACKUP_STORAGE_ACCOUNT}" \
    --container-name "${BACKUP_CONTAINER_NAME}" \
    --name "${BLOB_NAME}" \
    --file "${DUMP_FILE}" \
    --output none; then
    log "ERROR: upload failed. The dump was NOT deleted from /tmp so it isn't lost -- check ${DUMP_FILE} manually before this script (or its trap) removes it on next run."
    trap - EXIT # keep the local file around for manual recovery
    exit 1
fi

log "Backup ${BLOB_NAME} uploaded successfully."
