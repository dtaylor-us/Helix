# Containers

Container assets for local development and production packaging.

- Local development: `infra/local/docker-compose.yml` (Postgres) + an optional local Ollama endpoint
  (user-managed).
- Production: `apps/api/Dockerfile` builds the API image, pushed to `ghcr.io` and deployed to Azure
  Container Apps by `.github/workflows/deploy-api.yml`. See
  [ADR-022](../../docs/decisions/ADR-022-azure-production-deployment.md) and
  [docs/deployment/azure-runbook.md](../../docs/deployment/azure-runbook.md).
