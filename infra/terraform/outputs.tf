output "resource_group_name" {
  description = "Name of the resource group holding everything this configuration creates."
  value       = azurerm_resource_group.main.name
}

output "api_url" {
  description = "Public HTTPS URL of the deployed API (Container Apps' auto-generated ingress FQDN)."
  value       = "https://${azurerm_container_app.api.ingress[0].fqdn}"
}

output "static_web_app_default_hostname" {
  description = "Default hostname of the Static Web App (e.g. https://<name>.azurestaticapps.net). NOTE the two-phase apply this implies: this value isn't known until after the first `apply`, but is needed as `web_app_url` for the API's CORS/OAuth-redirect configuration -- see the runbook's bootstrap steps."
  value       = "https://${azurerm_static_web_app.web.default_host_name}"
}

output "static_web_app_deployment_token" {
  description = "Deployment token for deploy-web.yml to authenticate with (set this as the AZURE_STATIC_WEB_APPS_API_TOKEN GitHub secret)."
  value       = azurerm_static_web_app.web.api_key
  sensitive   = true
}

output "postgres_vm_public_ip" {
  description = "Public IP of the Postgres VM, for SSH access (restricted to admin_source_cidr by the VM's NSG)."
  value       = azurerm_public_ip.postgres_vm.ip_address
}

output "postgres_vm_private_ip" {
  description = "Private IP of the Postgres VM inside the VNet -- this is what the API actually connects to."
  value       = azurerm_network_interface.postgres_vm.private_ip_address
}

output "backup_storage_account_name" {
  description = "Name of the storage account holding Postgres backups."
  value       = azurerm_storage_account.backups.name
}

output "container_app_name" {
  description = "Name of the Container App running the API (used by deploy-api.yml to target `az containerapp update`)."
  value       = azurerm_container_app.api.name
}
