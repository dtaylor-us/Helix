provider "azurerm" {
  features {
    resource_group {
      # Allow `terraform destroy` to actually remove the resource group even if it still contains
      # resources Terraform doesn't know about (e.g. something created manually for a one-off test) --
      # deliberate for a small single-operator project; reconsider if this is ever shared with others.
      prevent_deletion_if_contains_resources = false
    }
  }

  # Auth: this project uses OIDC federated credentials from GitHub Actions (no long-lived client
  # secret stored anywhere -- see .github/workflows/terraform-apply.yml and the runbook's "Configure
  # GitHub OIDC" step). When running Terraform locally, `az login` (Azure CLI auth) is used instead;
  # the azurerm provider auto-detects both, so no explicit credentials are configured here.
  subscription_id = var.azure_subscription_id
}
