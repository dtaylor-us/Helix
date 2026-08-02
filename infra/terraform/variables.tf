variable "azure_subscription_id" {
  description = "Azure subscription id to deploy into."
  type        = string
}

variable "project" {
  description = "Short project name, used as a prefix for resource names."
  type        = string
  default     = "helix"
}

variable "environment" {
  description = "Deployment environment name (e.g. prod). Kept as a variable rather than hardcoded so a second environment (e.g. staging) can reuse this same configuration later."
  type        = string
  default     = "prod"
}

variable "location" {
  description = "Azure region for most resources (VM, VNet, Container Apps environment, storage, Log Analytics). Must support Azure Container Apps."
  type        = string
  default     = "eastus"
}

variable "static_web_app_location" {
  description = "Azure region for the Static Web App. Static Web Apps are only available in a subset of regions (e.g. eastus2, westus2, centralus, westeurope, eastasia) -- kept as a separate variable from `location` since it will not always match."
  type        = string
  default     = "eastus2"
}

# --- Access control -------------------------------------------------------------------------------

variable "admin_source_cidr" {
  description = "CIDR block allowed to reach the Postgres VM over SSH (port 22). Set this to your own IP address as a /32, e.g. \"203.0.113.4/32\" -- never leave this open to 0.0.0.0/0. Find your current IP with `curl -s https://ifconfig.me`."
  type        = string

  validation {
    condition     = var.admin_source_cidr != "0.0.0.0/0"
    error_message = "admin_source_cidr must not be open to the entire internet. Use your own IP address as a /32."
  }
}

variable "vm_admin_username" {
  description = "Login username for the Postgres VM."
  type        = string
  default     = "helixadmin"
}

variable "vm_ssh_public_key" {
  description = "SSH public key (contents of e.g. ~/.ssh/id_ed25519.pub) authorized to log into the Postgres VM. Password auth is disabled entirely -- see vm-postgres.tf."
  type        = string
}

# --- Database ---------------------------------------------------------------------------------------

variable "postgres_db_name" {
  description = "Database name created inside the self-hosted Postgres container."
  type        = string
  default     = "helix"
}

variable "postgres_admin_user" {
  description = "Postgres superuser/application user name."
  type        = string
  default     = "helix"
}

variable "postgres_admin_password" {
  description = "Postgres password. Must be supplied via TF_VAR_postgres_admin_password or a .auto.tfvars file that is gitignored -- never commit this."
  type        = string
  sensitive   = true

  validation {
    condition     = length(var.postgres_admin_password) >= 16
    error_message = "postgres_admin_password must be at least 16 characters."
  }
}

# --- API container -----------------------------------------------------------------------------------

variable "api_container_image" {
  description = "Full image reference for the API (e.g. ghcr.io/<owner>/helix-api:latest). Azure Container Apps validates that this image actually exists (it does a manifest GET) at resource-creation time -- pointing this at a tag that hasn't been pushed yet fails the whole `terraform apply`, not just the container's startup. Defaults to Microsoft's public placeholder image for exactly this reason: it lets the first apply succeed before any real image has been built, and the Container App's image is deliberately excluded from Terraform's change tracking after that first apply (see lifecycle.ignore_changes in container-apps.tf) -- day-to-day deploys update the running image via `az containerapp update` from deploy-api.yml, never via `terraform apply` again."
  type        = string
  default     = "mcr.microsoft.com/azuredocs/containerapps-helloworld:latest"
}

variable "google_oauth_client_id" {
  description = "Google OAuth2 client id (from the Google Cloud Console credentials created for Helix)."
  type        = string
  sensitive   = true
}

variable "google_oauth_client_secret" {
  description = "Google OAuth2 client secret."
  type        = string
  sensitive   = true
}

variable "web_app_url" {
  description = "The production frontend URL (the Static Web App's hostname, e.g. https://helix-prod.azurestaticapps.net, or a custom domain if one is configured later). Used for OAuth2 redirect targets and the CORS allowed-origins list -- set after the Static Web App is first created, since its default hostname isn't known until then (see the runbook's two-phase apply note)."
  type        = string
}

variable "ghcr_username" {
  description = "GitHub username or org that owns the ghcr.io package (e.g. \"dtaylor-us\"). Used so the Container App can authenticate to pull a private image."
  type        = string
}

variable "ghcr_read_token" {
  description = "GitHub Personal Access Token (classic or fine-grained) with read:packages scope, used by the Container App at runtime to pull the API image from ghcr.io. This is a separate, long-lived credential from the GITHUB_TOKEN the deploy workflow uses to push images -- that token is ephemeral (scoped to a single workflow run) and can't be used for the Container App's own pulls. Rotate periodically; see the runbook."
  type        = string
  sensitive   = true
}

variable "backup_retention_days" {
  description = "How many days of Postgres backups to retain in Blob Storage before lifecycle-managed deletion."
  type        = number
  default     = 14
}

variable "tags" {
  description = "Common tags applied to all resources."
  type        = map(string)
  default = {
    project    = "helix"
    managed-by = "terraform"
  }
}
