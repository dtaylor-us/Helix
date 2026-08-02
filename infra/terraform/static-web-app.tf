# ADR-022: Free tier -- $0/mo, 100GB bandwidth, managed SSL, custom domain support later if wanted.
# Deployed by deploy-web.yml using the Azure/static-web-apps-deploy-action, authenticated with the
# deployment token exposed in outputs.tf (set as a GitHub secret, not stored in this repo).

resource "azurerm_static_web_app" "web" {
  name                = "swa-${var.project}-${var.environment}"
  resource_group_name = azurerm_resource_group.main.name
  location            = var.static_web_app_location
  sku_tier            = "Free"
  sku_size            = "Free"
  tags                = var.tags
}
