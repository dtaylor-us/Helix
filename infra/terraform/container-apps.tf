# ADR-022: Consumption-only, VNet-integrated Container Apps environment running the Spring Boot API.
# Scale-to-zero when idle is the single biggest cost lever in this whole deployment -- see ADR-022.

resource "azurerm_container_app_environment" "main" {
  name                       = "cae-${var.project}-${var.environment}"
  resource_group_name        = azurerm_resource_group.main.name
  location                   = azurerm_resource_group.main.location
  log_analytics_workspace_id = azurerm_log_analytics_workspace.main.id
  infrastructure_subnet_id   = azurerm_subnet.container_apps.id
  tags                       = var.tags

  lifecycle {
    # `infrastructure_resource_group_name` is ForceNew in the provider, but Azure auto-generates a
    # value for it (the "ME_<env>_<rg>_<region>" resource group backing a VNet-integrated
    # environment) whenever it's left unset here -- without this, every subsequent `terraform plan`
    # sees "config wants null, real state has Azure's generated value" and forces a full
    # destroy/recreate of the environment (and everything attached to it) on every single apply.
    ignore_changes = [infrastructure_resource_group_name]
  }
}

resource "azurerm_container_app" "api" {
  name                         = "ca-${var.project}-api-${var.environment}"
  resource_group_name          = azurerm_resource_group.main.name
  container_app_environment_id = azurerm_container_app_environment.main.id
  revision_mode                = "Single"
  tags                         = var.tags

  registry {
    server               = "ghcr.io"
    username              = var.ghcr_username
    password_secret_name = "ghcr-read-token"
  }

  secret {
    name  = "ghcr-read-token"
    value = var.ghcr_read_token
  }
  secret {
    name  = "db-url"
    # Postgres VM's static private IP (10.20.1.4, see vm-postgres.tf) -- never traverses the public
    # internet, since the Container Apps environment is VNet-integrated into the same VNet.
    value = "jdbc:postgresql://${azurerm_network_interface.postgres_vm.private_ip_address}:5432/${var.postgres_db_name}"
  }
  secret {
    name  = "db-user"
    value = var.postgres_admin_user
  }
  secret {
    name  = "db-password"
    value = var.postgres_admin_password
  }
  secret {
    name  = "google-client-id"
    value = var.google_oauth_client_id
  }
  secret {
    name  = "google-client-secret"
    value = var.google_oauth_client_secret
  }

  template {
    min_replicas = 0 # scale-to-zero
    max_replicas = 1 # a single user doesn't need concurrent replicas; also bounds worst-case Consumption plan cost

    container {
      name   = "helix-api"
      image  = var.api_container_image
      cpu    = 0.5
      memory = "1Gi" # Spring Boot's JVM footprint needs more headroom than Container Apps' smallest allocation (0.25 vCPU/0.5Gi) comfortably allows

      env {
        name        = "HELIX_DB_URL"
        secret_name = "db-url"
      }
      env {
        name        = "HELIX_DB_USER"
        secret_name = "db-user"
      }
      env {
        name        = "HELIX_DB_PASSWORD"
        secret_name = "db-password"
      }
      env {
        name        = "HELIX_GOOGLE_CLIENT_ID"
        secret_name = "google-client-id"
      }
      env {
        name        = "HELIX_GOOGLE_CLIENT_SECRET"
        secret_name = "google-client-secret"
      }
      env {
        name  = "HELIX_WEB_APP_URL"
        value = var.web_app_url
      }
      env {
        name  = "HELIX_WEB_ALLOWED_ORIGINS"
        value = var.web_app_url
      }
      env {
        name  = "HELIX_SESSION_COOKIE_SECURE"
        value = "true"
      }
      env {
        name  = "HELIX_SESSION_COOKIE_SAMESITE"
        # ADR-022: Static Web Apps and Container Apps land on different domains -- this is genuinely
        # cross-site, so SameSite=None (paired with Secure=true above, which browsers require
        # alongside it) is mandatory here, unlike local dev's same-site default of "lax".
        value = "none"
      }

      liveness_probe {
        transport = "HTTP"
        path      = "/api/v1/health"
        port      = 8080
      }
      readiness_probe {
        transport = "HTTP"
        path      = "/api/v1/health"
        port      = 8080
      }
    }
  }

  ingress {
    external_enabled = true
    target_port       = 8080
    transport          = "auto"

    traffic_weight {
      latest_revision = true
      percentage      = 100
    }
  }

  lifecycle {
    # The API image is updated by deploy-api.yml via `az containerapp update --image ...` on every
    # merge to main, not by `terraform apply` -- without this, the next unrelated `terraform apply`
    # (e.g. a network change) would silently roll the running image back to whatever
    # var.api_container_image happened to be set to at the time, undoing the latest deploy.
    ignore_changes = [
      template[0].container[0].image,
    ]
  }
}
