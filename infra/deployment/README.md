# Deployment

Production deployment target: Azure (Static Web Apps + Container Apps + a self-hosted Postgres VM),
cost-optimized for a single user plus a few invited people. See:

- [ADR-022](../../docs/decisions/ADR-022-azure-production-deployment.md) for the architecture and
  the tradeoffs behind it.
- [docs/deployment/azure-runbook.md](../../docs/deployment/azure-runbook.md) for the step-by-step
  bootstrap and day-2 operations.
- `infra/terraform/` for the infrastructure-as-code, `infra/cloud-init/` and `infra/scripts/` for the
  Postgres VM's provisioning and backup/restore tooling.
