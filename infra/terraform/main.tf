# ADR-022: single resource group for all of Helix's production infrastructure. A small enough
# footprint (one VM, one Container Apps environment, one storage account, one Static Web App) that
# splitting across multiple resource groups would add organizational overhead without a real benefit
# at this scale.

resource "azurerm_resource_group" "main" {
  name     = "rg-${var.project}-${var.environment}"
  location = var.location
  tags     = var.tags
}

# Azure Storage account names must be globally unique, 3-24 lowercase alphanumeric characters -- this
# suffix keeps `terraform apply` idempotent (doesn't collide with anyone else's account) without
# requiring the operator to hand-pick a unique name.
resource "random_string" "suffix" {
  length  = 6
  special = false
  upper   = false
  numeric = true
}
