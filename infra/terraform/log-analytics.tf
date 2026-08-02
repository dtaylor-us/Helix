# Azure Container Apps environments require a Log Analytics workspace for platform/application logs.
# `daily_quota_gb` is set low deliberately -- it's a safety cap against a runaway logging bug (e.g. an
# accidental tight-loop log statement), not the expected steady-state cost. A single low-traffic user
# should generate at most a few MB/day, well under this cap, so it shouldn't be hit in normal
# operation; if it is hit routinely, that's worth investigating rather than raising the cap first.

resource "azurerm_log_analytics_workspace" "main" {
  name                = "log-${var.project}-${var.environment}"
  resource_group_name = azurerm_resource_group.main.name
  location            = azurerm_resource_group.main.location
  sku                 = "PerGB2018"
  retention_in_days   = 30
  daily_quota_gb      = 0.5
  tags                = var.tags
}
