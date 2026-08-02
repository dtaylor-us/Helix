# ADR-022: Postgres backups (see infra/scripts/backup-postgres.sh, run by cron on the VM via
# cloud-init) land here as plain `pg_dump` artifacts. LRS (locally redundant storage) is the cheapest
# redundancy tier -- acceptable for a single user's backups given the primary copy still exists on the
# VM's disk; upgrade to ZRS/GRS if that stops being true (e.g. once this is the only copy that matters).

resource "azurerm_storage_account" "backups" {
  name                     = "st${var.project}bkp${random_string.suffix.result}"
  resource_group_name      = azurerm_resource_group.main.name
  location                 = azurerm_resource_group.main.location
  account_tier             = "Standard"
  account_replication_type = "LRS"
  account_kind             = "StorageV2"
  access_tier              = "Cool"

  min_tls_version                 = "TLS1_2"
  public_network_access_enabled   = true # backup upload happens from the VM over the internet, authenticated via the VM's managed identity (no account key ever leaves Azure AD) -- see Risks in ADR-022 re: tightening this to a private endpoint later
  allow_nested_items_to_be_public = false

  tags = var.tags
}

resource "azurerm_storage_container" "postgres_backups" {
  name = "postgres-backups"
  # NOTE: azurerm provider < 4.x only supports storage_account_name here (storage_account_id was
  # added in 4.x, alongside deprecating this field) -- versions.tf pins "~> 3.116", so this must stay
  # storage_account_name. If the provider is ever bumped to 4.x, switch this to storage_account_id
  # (and check every other resource in this directory for the same 3.x/4.x argument drift).
  storage_account_name = azurerm_storage_account.backups.name
  container_access_type = "private"
}

# Automatically delete backups older than the retention window so storage cost doesn't grow
# unbounded -- at this data volume the cost impact is negligible either way, but unbounded growth of
# anything is worth avoiding as a matter of course.
resource "azurerm_storage_management_policy" "backup_retention" {
  storage_account_id = azurerm_storage_account.backups.id

  rule {
    name    = "expire-old-backups"
    enabled = true

    filters {
      prefix_match = ["${azurerm_storage_container.postgres_backups.name}/"]
      blob_types   = ["blockBlob"]
    }

    actions {
      base_blob {
        delete_after_days_since_modification_greater_than = var.backup_retention_days
      }
    }
  }
}
