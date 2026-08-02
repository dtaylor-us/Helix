# Azure Production Deployment Runbook

Companion to [ADR-022](../decisions/ADR-022-azure-production-deployment.md). This is the step-by-step
sequence to go from nothing to a running production deployment, plus the day-2 operations you'll need
periodically. Read the ADR first if you haven't -- this document assumes you know *why* each piece
exists, not just *how* to run it.

**Before you start:** the Terraform and GitHub Actions in this repo have not been validated against a
real Azure subscription (the environment that built them had neither Azure credentials nor a working
`terraform` binary). Run `terraform validate` and read the `terraform plan` output carefully before
the first `apply` -- treat this runbook as a well-reasoned first draft, not a guarantee.

## Prerequisites

- Azure CLI (`az`) installed and `az login` run against the subscription you're deploying into.
- An SSH key pair (`ssh-keygen -t ed25519` if you don't have one).
- This repo pushed to GitHub, with Actions enabled.
- The Google OAuth client from the earlier setup conversation (client ID/secret) -- you'll update its
  redirect URI partway through this runbook once the API's URL is known.

## 1. Bootstrap Terraform's remote state

Run once, ever:

```bash
az login
cd infra/scripts
./bootstrap-terraform-state.sh rg-helix-tfstate eastus
```

Save the printed output (resource group, storage account, container names) -- you'll need it for
step 4 and for the GitHub repository variables in step 3.

## 2. Set up GitHub OIDC federated authentication

This lets GitHub Actions authenticate to Azure without a long-lived stored secret. Two federated
credentials are needed: one for anything running against the `main` branch (deploys, manual
`terraform apply`), one for pull requests (`terraform plan`).

```bash
# Create the app registration and service principal
az ad app create --display-name "helix-github-actions"
APP_ID=$(az ad app list --display-name "helix-github-actions" --query "[0].appId" -o tsv)
az ad sp create --id "$APP_ID"

# Grant it Contributor on the subscription (or scope this down to just the rg-helix-prod resource
# group once it exists, for tighter least-privilege -- Contributor at the subscription level is the
# simpler starting point since the resource group doesn't exist yet on first run)
SUBSCRIPTION_ID=$(az account show --query id -o tsv)
az role assignment create --assignee "$APP_ID" --role Contributor --scope "/subscriptions/$SUBSCRIPTION_ID"

# Federated credential for main-branch runs (deploy-api.yml, deploy-web.yml, terraform-apply.yml)
az ad app federated-credential create --id "$APP_ID" --parameters '{
  "name": "helix-main-branch",
  "issuer": "https://token.actions.githubusercontent.com",
  "subject": "repo:dtaylor-us/helix:ref:refs/heads/main",
  "audiences": ["api://AzureADTokenExchange"]
}'

# Federated credential for pull requests (terraform-plan.yml)
az ad app federated-credential create --id "$APP_ID" --parameters '{
  "name": "helix-pull-requests",
  "issuer": "https://token.actions.githubusercontent.com",
  "subject": "repo:dtaylor-us/helix:pull_request",
  "audiences": ["api://AzureADTokenExchange"]
}'
```

Replace `dtaylor-us/helix` with your actual `owner/repo`. Note the `appId`, your Azure AD
tenant id (`az account show --query tenantId -o tsv`), and the subscription id -- these become GitHub
secrets in the next step.

## 3. Configure GitHub secrets and variables

**Secrets** (Settings -> Secrets and variables -> Actions -> Secrets):

| Name | Value |
|---|---|
| `AZURE_CLIENT_ID` | the `appId` from step 2 |
| `AZURE_TENANT_ID` | your Azure AD tenant id |
| `AZURE_SUBSCRIPTION_ID` | your Azure subscription id |
| `POSTGRES_ADMIN_PASSWORD` | a strong, generated password (16+ chars) -- this becomes the actual Postgres password |
| `GHCR_READ_TOKEN` | a GitHub Personal Access Token (classic, `read:packages` scope, or fine-grained equivalent) -- used by the Container App to pull images, separate from the ephemeral token Actions uses to push them |
| `GOOGLE_OAUTH_CLIENT_ID` | from the Google Cloud Console credentials set up earlier |
| `GOOGLE_OAUTH_CLIENT_SECRET` | same |
| `AZURE_STATIC_WEB_APPS_API_TOKEN` | set after step 4 (see below) |

**Variables** (same page, Variables tab):

| Name | Value |
|---|---|
| `TF_STATE_RESOURCE_GROUP` | from step 1's output |
| `TF_STATE_STORAGE_ACCOUNT` | from step 1's output |
| `TF_STATE_CONTAINER` | `tfstate` |
| `ADMIN_SOURCE_CIDR` | your own IP as a /32 (`curl -s https://ifconfig.me`) |
| `VM_ADMIN_USERNAME` | `helixadmin` (or your preference) |
| `VM_SSH_PUBLIC_KEY` | contents of your `.pub` key file |
| `AZURE_RESOURCE_GROUP` | `rg-helix-prod` (matches `variables.tf`'s naming: `rg-${project}-${environment}`) |
| `CONTAINER_APP_NAME` | set after step 4 (Terraform output `container_app_name`) |
| `WEB_APP_URL` | a placeholder for now (e.g. `https://placeholder.azurestaticapps.net`) -- updated in step 5 |
| `API_BASE_URL` | set after step 4 (see below) |

## 4. First `terraform apply`

This is the one genuinely two-phase step in this whole setup: the Static Web App's hostname and the
Container App's URL aren't known until after they're created, but the Container App needs the Static
Web App's URL (for CORS/OAuth redirects) and the frontend needs the Container App's URL (to know
where to send API calls). A placeholder gets you through the first apply; you fix both URLs for real
immediately after.

```bash
cd infra/terraform
terraform init \
  -backend-config="resource_group_name=rg-helix-tfstate" \
  -backend-config="storage_account_name=sttfstateca5260" \
  -backend-config="container_name=tfstate" \
  -backend-config="key=helix.tfstate"

terraform plan   # read this carefully -- it should show only new resources being created
terraform apply
```

You'll be prompted for the variables not set via `TF_VAR_*` env vars -- or set them all first:

```bash
export TF_VAR_azure_subscription_id="<your subscription id>"
export TF_VAR_admin_source_cidr="<your IP>/32"
export TF_VAR_vm_admin_username="helixadmin"
export TF_VAR_vm_ssh_public_key="$(cat ~/.ssh/id_rsa_helix_vm.pub)"  # RSA, not ed25519 -- some Azure subscriptions reject ed25519 VM keys outright; see the SSH key troubleshooting note below if you generated ed25519 already
export TF_VAR_postgres_admin_password="<a strong generated password, 16+ chars>"
export TF_VAR_ghcr_username="<your-github-username>"
export TF_VAR_ghcr_read_token="<a GitHub PAT with read:packages scope>"
export TF_VAR_google_oauth_client_id="<from Google Cloud Console>"
export TF_VAR_google_oauth_client_secret="<from Google Cloud Console>"
export TF_VAR_web_app_url="https://placeholder.azurestaticapps.net"
```

Deliberately **not** setting `TF_VAR_api_container_image` here -- leave it unset so it falls back to
its default (a public Microsoft placeholder image; see `variables.tf`). Azure Container Apps
validates that an image reference actually exists (it does a manifest GET against the registry) at
resource-creation time, not just at container startup -- pointing this at your real
`ghcr.io/<you>/helix-api:latest` before you've ever pushed anything there fails the **entire
`terraform apply`**, not just the container's startup, with a `MANIFEST_UNKNOWN` error. Once you've
pushed a real image (step 7, or manually), you never need to set this variable again -- the running
image is updated by `az containerapp update` from then on, which Terraform is configured to ignore
(see `lifecycle.ignore_changes` in `container-apps.tf`).

**Never commit real values for any of the above.** Set them as actual shell exports in your terminal
(not saved into this file, a script, or a `.tfvars` file you might forget is gitignored) -- or better,
keep them in a password manager and paste them into the export commands each session. If a real
secret is ever pasted into this file by mistake, treat it as compromised: rotate it (regenerate the
GitHub PAT / Google client secret / Postgres password / re-run `az ad app credential reset` for the
Azure client) rather than just deleting the text, since `git status` not showing the file as committed
only means it hasn't reached GitHub yet -- it doesn't undo the secret having existed in plaintext on
disk.

**SSH key troubleshooting:** if `terraform apply` fails with a key-decoding or "Only RSA SSH keys are
supported" error, the key you generated was `ed25519` and this subscription/region rejects it.
Generate an RSA key instead and re-export the variable:

```bash
ssh-keygen -t rsa -b 4096 -f ~/.ssh/id_rsa_helix_vm -C "helix-postgres-vm"
export TF_VAR_vm_ssh_public_key="$(cat ~/.ssh/id_rsa_helix_vm.pub)"
```

## 5. Wire up the real URLs

```bash
terraform output api_url                       # -> API_BASE_URL GitHub variable
terraform output static_web_app_default_hostname # -> WEB_APP_URL GitHub variable
terraform output -raw static_web_app_deployment_token # -> AZURE_STATIC_WEB_APPS_API_TOKEN GitHub secret
terraform output container_app_name             # -> CONTAINER_APP_NAME GitHub variable
```

Update those four GitHub secrets/variables with the real values, then:

```bash
export TF_VAR_web_app_url="$(terraform output -raw static_web_app_default_hostname)"
terraform apply   # updates the Container App's HELIX_WEB_APP_URL / HELIX_WEB_ALLOWED_ORIGINS env vars
```

## 6. Update the Google OAuth client's redirect URI

In Google Cloud Console (Clients -> your OAuth client -> Authorized redirect URIs), add:

```
<api_url from step 5>/login/oauth2/code/google
```

(This is the same step from the earlier Google OAuth setup conversation -- you're adding the
production redirect URI alongside the existing localhost one, not replacing it.)

## 7. First real deploy

The Container App is currently running Microsoft's placeholder "hello world" image (see step 4) --
nothing resembling Helix is live yet. Push to `main` (or merge a PR). This triggers, in order: `CI`
-> `Deploy API` (builds the real image, pushes it to ghcr.io, updates the Container App to use it) ->
`Deploy Web` (builds the SPA with the real `VITE_API_BASE_URL`, deploys to Static Web Apps). Flyway
migrations run automatically when the API starts against the fresh database -- no separate migration
step.

If you'd rather not wait on CI for the very first image, you can build and push it yourself:

```bash
echo "<your GHCR_READ_TOKEN or a PAT with write:packages>" | docker login ghcr.io -u <your-github-username> --password-stdin
docker build -t ghcr.io/<your-github-username>/helix-api:latest apps/api
docker push ghcr.io/<your-github-username>/helix-api:latest
az containerapp update --name ca-helix-api-prod --resource-group rg-helix-prod \
  --image ghcr.io/<your-github-username>/helix-api:latest
```

## 8. Invite yourself and others

The database has no rows in `authorized_users` yet except whatever your Helix migration seeds (check
`V12__authentication_and_ownership.sql` -- it pre-authorizes the email used during development).  To
invite someone else:

```bash
ssh helixadmin@<postgres_vm_public_ip>   # from terraform output
docker exec -it helix-postgres psql -U helix -d helix
```

```sql
insert into authorized_users (email, invited_at, note)
values ('someone@example.com', now(), 'Invited by Derek');
```

They also need to be a Google **test user** if the OAuth consent screen is still in Testing status
(see the earlier Google OAuth setup conversation) -- Google rejects the login before it ever reaches
Helix's own allowlist otherwise.

## 9. Verify

Visit the Static Web App URL, sign in with Google, confirm the session persists across page reloads
(this is the split-origin cookie behavior ADR-022 specifically addresses -- if login appears to
succeed but you're immediately logged out, `HELIX_SESSION_COOKIE_SAMESITE` not being `none` on the
Container App is the first thing to check).

---

## Day-2 operations

### Restoring from backup

```bash
ssh helixadmin@<postgres_vm_public_ip>
az storage blob list --auth-mode login --account-name <backup_storage_account_name> \
  --container-name postgres-backups --output table
sudo /opt/helix/restore-postgres.sh <blob-name-from-above>
```

The script requires typing the database name to confirm before it does anything destructive. See
`infra/scripts/restore-postgres.sh` for exactly what it does.

### Rotating the Postgres password

1. `terraform apply` with a new `TF_VAR_postgres_admin_password` updates the Container App's secret,
   but does **not** change the actual password inside the running Postgres container (Terraform only
   controls the VM's initial cloud-init, which only runs once at VM creation).
2. Change it directly: `docker exec -it helix-postgres psql -U helix -c "ALTER USER helix WITH PASSWORD 'new-password'"`.
3. Update `POSTGRES_ADMIN_PASSWORD` in GitHub secrets and re-run `terraform apply` so the Container
   App's secret matches, then restart the Container App revision so it picks up the new secret value.

### Checking Azure spend

Azure Portal -> Cost Management -> Cost analysis, scoped to `rg-helix-prod`. Worth checking monthly
given the $15/mo ceiling this whole architecture was designed around (see ADR-022's Reconsideration
Triggers) -- Container Apps usage growing past the free grant is the most likely thing to move the
number, since the VM and Static Web App costs are fixed.

### VM patching beyond automatic security updates

`unattended-upgrades` handles security patches automatically. For OS version upgrades or anything it
doesn't cover:

```bash
ssh helixadmin@<postgres_vm_public_ip>
sudo apt update && sudo apt upgrade -y
sudo reboot   # if the kernel or Docker itself was updated
```

### Destroying everything

```bash
cd infra/terraform
terraform destroy
```

This does **not** delete the Terraform state storage account from step 1 (that's outside Terraform's
own management, by design) or any backups already in Blob Storage if you want to keep them --
delete those manually if you're tearing this down for good.
