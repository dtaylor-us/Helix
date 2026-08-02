#!/usr/bin/env bash
# ADR-022: manual restore procedure -- deliberately NOT automated/scheduled. Restoring is a
# destructive, rare operation that should always involve a human deciding "yes, restore from this
# specific backup, right now" rather than running unattended.
#
# Usage (run on the Postgres VM, over SSH):
#   sudo ./restore-postgres.sh <blob-name>
#
# List available backups first with:
#   az storage blob list --auth-mode login --account-name <storage-account> \
#     --container-name postgres-backups --output table
set -euo pipefail

ENV_FILE="/etc/helix/backup.env"
BLOB_NAME="${1:?Usage: restore-postgres.sh <blob-name>  (list backups with: az storage blob list --auth-mode login --account-name <storage-account> --container-name postgres-backups --output table)}"

if [[ ! -f "$ENV_FILE" ]]; then
    echo "ERROR: $ENV_FILE not found." >&2
    exit 1
fi
# shellcheck source=/dev/null
source "$ENV_FILE"

: "${POSTGRES_DB:?POSTGRES_DB must be set in $ENV_FILE}"
: "${BACKUP_STORAGE_ACCOUNT:?BACKUP_STORAGE_ACCOUNT must be set in $ENV_FILE}"
: "${BACKUP_CONTAINER_NAME:?BACKUP_CONTAINER_NAME must be set in $ENV_FILE}"

RESTORE_FILE="/tmp/${BLOB_NAME}"

echo "This will DROP and recreate the '${POSTGRES_DB}' database, replacing ALL current data with the"
echo "contents of backup '${BLOB_NAME}'. This cannot be undone unless you have a more recent backup."
read -r -p "Type the database name (${POSTGRES_DB}) to confirm: " CONFIRMATION
if [[ "$CONFIRMATION" != "$POSTGRES_DB" ]]; then
    echo "Confirmation did not match. Aborting -- no changes made."
    exit 1
fi

echo "Authenticating to Azure via managed identity..."
az login --identity --output none

echo "Downloading ${BLOB_NAME}..."
az storage blob download \
    --auth-mode login \
    --account-name "${BACKUP_STORAGE_ACCOUNT}" \
    --container-name "${BACKUP_CONTAINER_NAME}" \
    --name "${BLOB_NAME}" \
    --file "${RESTORE_FILE}" \
    --output none

echo "Stopping the API's ability to write during restore is out of scope of this script -- if the"
echo "Container App is running, either scale it to 0 first (az containerapp update --min-replicas 0"
echo "--max-replicas 0) or accept a short window of inconsistent reads/writes during restore."
read -r -p "Press Enter to continue with the restore, or Ctrl+C to abort..."

echo "Dropping and recreating '${POSTGRES_DB}'..."
docker exec -u postgres helix-postgres dropdb --if-exists "${POSTGRES_DB}"
docker exec -u postgres helix-postgres createdb "${POSTGRES_DB}"

echo "Restoring from ${RESTORE_FILE}..."
docker exec -i -u postgres helix-postgres pg_restore --no-owner --role=postgres -d "${POSTGRES_DB}" < "${RESTORE_FILE}"

rm -f "${RESTORE_FILE}"
echo "Restore complete. If you scaled the Container App to 0 above, scale it back up now:"
echo "  az containerapp update --name <container-app-name> --resource-group <rg-name> --min-replicas 0 --max-replicas 1"
