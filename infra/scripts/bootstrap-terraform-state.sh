#!/usr/bin/env bash
# ADR-022: one-time setup, run manually (not from CI) before the very first `terraform init`.
# Terraform's remote state has to live somewhere Terraform doesn't manage itself -- this script
# creates that "somewhere" with plain `az` commands.
#
# NOT safely re-runnable as-is: the storage account name includes a random suffix, so running this
# twice creates two separate storage accounts rather than reusing the first. Run it once per
# environment and keep the printed values -- if you need to re-run it (e.g. you lost the output),
# either reuse an existing storage account manually or accept that you'll have an orphaned one to
# clean up (`az storage account delete`).
#
# Usage:
#   az login
#   ./bootstrap-terraform-state.sh <resource-group-name> <location>
#
# Example:
#   ./bootstrap-terraform-state.sh rg-helix-tfstate eastus
set -euo pipefail

RESOURCE_GROUP="${1:?Usage: bootstrap-terraform-state.sh <resource-group-name> <location>}"
LOCATION="${2:?Usage: bootstrap-terraform-state.sh <resource-group-name> <location>}"
CONTAINER_NAME="tfstate"

# Storage account names must be globally unique, 3-24 lowercase alphanumeric characters.
# Generate a short lowercase hex suffix from ASCII output to avoid locale issues on macOS.
SUFFIX="$(od -An -N16 -tx1 /dev/urandom | tr -d ' \n' | cut -c1-6)"
STORAGE_ACCOUNT_NAME="sttfstate${SUFFIX}"

echo "Creating resource group '${RESOURCE_GROUP}' in ${LOCATION}..."
az group create --name "${RESOURCE_GROUP}" --location "${LOCATION}" --output none

echo "Creating storage account '${STORAGE_ACCOUNT_NAME}' for Terraform state..."
az storage account create \
    --name "${STORAGE_ACCOUNT_NAME}" \
    --resource-group "${RESOURCE_GROUP}" \
    --location "${LOCATION}" \
    --sku Standard_LRS \
    --kind StorageV2 \
    --min-tls-version TLS1_2 \
    --allow-blob-public-access false \
    --output none

echo "Enabling blob versioning (protects state from accidental corruption/overwrite)..."
az storage account blob-service-properties update \
    --account-name "${STORAGE_ACCOUNT_NAME}" \
    --resource-group "${RESOURCE_GROUP}" \
    --enable-versioning true \
    --output none

echo "Creating blob container '${CONTAINER_NAME}'..."
az storage container create \
    --name "${CONTAINER_NAME}" \
    --account-name "${STORAGE_ACCOUNT_NAME}" \
    --auth-mode login \
    --output none

cat <<EOF

Done. Use these values to initialize Terraform:

  terraform init \\
    -backend-config="resource_group_name=${RESOURCE_GROUP}" \\
    -backend-config="storage_account_name=${STORAGE_ACCOUNT_NAME}" \\
    -backend-config="container_name=${CONTAINER_NAME}" \\
    -backend-config="key=helix.tfstate"

Also set these as GitHub repository variables (Settings -> Secrets and variables -> Actions ->
Variables) so terraform-plan.yml / terraform-apply.yml can use the same backend:

  TF_STATE_RESOURCE_GROUP = ${RESOURCE_GROUP}
  TF_STATE_STORAGE_ACCOUNT = ${STORAGE_ACCOUNT_NAME}
  TF_STATE_CONTAINER = ${CONTAINER_NAME}

This script only needs to be run once, ever, per environment -- save this output somewhere durable
(e.g. a password manager note), since re-running the script will create a second, separate storage
account rather than reusing this one.
EOF
