# ADR-022: pinned provider/Terraform versions. Bump deliberately, not automatically -- this
# environment could not run `terraform init`/`plan` (no network access to the provider registry, no
# terraform binary available), so there has been no live validation against these exact versions.
# Run `terraform init` and `terraform plan` locally (or in CI's plan job) before the first `apply`.

terraform {
  required_version = ">= 1.7.0, < 2.0.0"

  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "~> 3.116"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.6"
    }
    tls = {
      source  = "hashicorp/tls"
      version = "~> 4.0"
    }
  }
}
