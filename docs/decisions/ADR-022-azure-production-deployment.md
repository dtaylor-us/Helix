# ADR-022 Azure production deployment architecture

- Status: Accepted
- Date: 2026-08-02
- Amends: ADR-021 (resolves its "cookie behavior under split-origin hosting is not yet exercised"
  risk and its first Reconsideration Trigger — hosting has now settled on a split-origin topology)
- Related: ADR-011 (this ADR is the concrete deployment target ADR-011 anticipated -- still no
  Kubernetes, consistent with ADR-011's reasoning); ADR-013 (superseded by ADR-021, which deferred
  the hosting decision this ADR now makes)

## Context
Helix is ready to deploy for its first real users ("me plus a few invited people," per ADR-021). The
requirements for this decision, gathered directly from the user: deploy on Azure, optimize for cost
(hard ceiling of $15/month), single user to start with headroom for a few more, and cold starts (a
few seconds' delay after the app has been idle) are acceptable — which is the single biggest lever
available for cost, since it unlocks scale-to-zero compute instead of paying for always-on capacity
nobody is using most of the time.

Pricing was checked directly against current Azure documentation and pricing pages (not assumed from
training data, since cloud pricing changes) as of August 2026:
- **Azure Database for PostgreSQL Flexible Server**, Burstable B1ms (the cheapest managed tier): ~$13/mo
  compute alone, before storage (~$4/mo for 32GB) or backups — roughly **$17+/mo**, over budget by
  itself. A 12-month free-tier grant exists (750 Burstable hours + 32GB storage/mo) but only applies
  to a subscription still within its first 12 months as a Free/new account; the user's subscription is
  an older pay-as-you-go one, so this doesn't apply.
- **Azure Container Apps**, Consumption plan: 180,000 vCPU-seconds + 360,000 GiB-seconds + 2,000,000
  requests free per subscription per month, scale-to-zero when idle. For a single intermittent user,
  this should land at **$0–3/mo**.
- **Azure Static Web Apps**, Free tier: **$0/mo** — 100GB bandwidth, free-managed SSL, custom domain
  support if/when needed later.
- **A B1s Linux VM** (1 vCPU/1GB RAM): ~$7.59/mo pay-as-you-go compute, plus a small managed disk
  (roughly $2–3/mo for 32GB Standard SSD, exact rate is region-specific).

The managed database is the one component that doesn't fit the budget on its own. Everything else
follows from resolving that.

## Decision
**Frontend**: Azure Static Web Apps (Free tier), built and deployed by GitHub Actions from
`apps/web`. $0/mo.

**API**: Azure Container Apps, Consumption plan, scale-to-zero, pulling the Spring Boot API's image
from GitHub Container Registry (`ghcr.io`) rather than Azure Container Registry (ACR's Basic tier
costs ~$5/mo on its own — a third of the entire budget — for something GitHub already provides free).
Deployed by GitHub Actions building and pushing the image, then updating the Container App's revision.

**Database**: PostgreSQL self-hosted in Docker on a dedicated B1s Linux VM, rather than the managed
Flexible Server. This is the one place this ADR trades operational simplicity for cost — see
Consequences and Risks. The VM and the Container Apps environment share one Azure VNet; Postgres is
bound to its private IP only, with no public internet exposure of port 5432 at all (a Network Security
Group permits inbound 5432 only from the VNet's address space). Backups are a scheduled `pg_dump` to
Azure Blob Storage (Cool tier), not a managed backup service.

**Infrastructure as code**: all of the above is defined in Terraform (`infra/terraform/`), with remote
state in an Azure Storage account (bootstrapped once via a plain script, since Terraform can't create
the backend it stores its own state in). GitHub Actions runs `terraform plan` on pull requests that
touch `infra/terraform/` and `terraform apply` on manual dispatch — apply is intentionally not
automatic on merge, since infrastructure changes here have direct cost and availability consequences
for a database VM, and a single user reviewing their own infra diff before it lands is a reasonable
bar for a project this size.

**Split-origin authentication.** Static Web Apps and Container Apps land on different default Azure
domains (e.g. `*.azurestaticapps.net` and `*.azurecontainerapps.io`) — this is genuinely cross-site,
not just cross-port like local dev. ADR-021 flagged this exact scenario as an untested risk requiring
`SameSite=None; Secure` on the session cookie plus an explicit CORS allow-credentials origin allowlist
(already implemented). This ADR resolves that: `HELIX_SESSION_COOKIE_SAMESITE` is now an environment
variable (default `lax` for local dev, set to `none` in the Container App's configuration), and
`SecurityConfig`'s CSRF-disabled rationale has been updated to rely on the CORS + JSON-content-type
preflight rejection as the load-bearing control rather than `SameSite`, since the latter is no longer
constant across environments.

## Alternatives
- **Managed Azure Database for PostgreSQL Flexible Server.** Rejected for now on cost alone (~$17+/mo
  against a $15/mo ceiling) — not because it's the wrong long-term choice. See Reconsideration
  Triggers: this is the first thing to revisit if the budget or user count changes.
- **External managed Postgres (e.g. Neon, Supabase) free tier.** Would hit $0/mo and include managed
  backups/autosuspend, genuinely competitive with the chosen approach. Rejected because it moves the
  data store outside Azure's trust/network boundary and adds a second cloud vendor to reason about
  for a marginal cost savings (roughly $10/mo) that the VM approach already achieves while keeping
  everything in one place. Revisit if self-managing the VM turns out to be more ops burden than
  expected (see Risks).
- **Everything on one VM** (API + Postgres together via Docker Compose, skip Container Apps
  entirely). Considered because it's marginally cheaper (no Container Apps usage at all) and simpler
  (one host to manage). Rejected because it gives up scale-to-zero on the API entirely — the API
  would run 24/7 on the VM whether anyone's using it or not, which is a worse cost/idle-time trade for
  a single intermittent user than paying Container Apps' near-zero consumption cost while keeping the
  VM's footprint minimal (database only).
- **Automatic `terraform apply` on merge to main.** Rejected in favor of manual `workflow_dispatch`
  for apply specifically (plan still runs automatically on PRs) — infra changes here can affect a
  stateful database VM, and cost/availability mistakes are more expensive to discover after the fact
  than a deliberate second step to trigger them.

## Consequences
- Total steady-state cost: roughly **$10–13/mo**, under the $15/mo ceiling with headroom.
- The user (not Azure) owns Postgres patching, version upgrades, and backup verification. This is a
  real, ongoing obligation, not a one-time setup cost — see Risks.
- Moving to managed Postgres later is a data-migration exercise (`pg_dump`/`pg_restore` against the
  existing backup mechanism), not a from-scratch rebuild — the backup script already produces a
  standard `pg_dump` artifact that's directly restorable to a managed instance.
- `HELIX_SESSION_COOKIE_SAMESITE` becomes a required production environment variable; forgetting to
  set it to `none` in a split-origin deployment doesn't fail loudly — it silently breaks login (the
  session cookie simply never gets attached to API calls), which is exactly the kind of bug this ADR
  exists to prevent by writing it down explicitly and wiring it through Terraform rather than a manual
  step.
- Terraform and the GitHub Actions workflows in this change are **not verified by an actual `apply`**
  — this sandbox has neither Azure credentials nor a working `terraform` binary (no network access to
  download it). Every resource definition was checked carefully against the `azurerm` provider's
  documented schema, but a real `terraform plan` against a live subscription is the first thing that
  should happen before `apply`, not skipped on the assumption this review was sufficient.

## Risks
- **Self-managed Postgres is the biggest operational risk in this decision.** No managed automatic
  patching, no managed point-in-time recovery, no managed high availability. Mitigated by:
  `unattended-upgrades` for OS-level security patches, a daily `pg_dump` to Blob Storage (Cool tier)
  for backups, and keeping the VM's only job as "run Postgres" (nothing else competes for its 1GB of
  RAM). Not mitigated: multi-day data loss window if backups silently fail and nobody notices — no
  backup-verification alerting exists yet (see Reconsideration Triggers).
- **VM is a single point of failure with no automatic failover.** If the VM goes down, the whole app
  is down until it's manually restarted or restored. Acceptable for a single user's personal app;
  would not be acceptable if this needed any real uptime guarantee.
- **1GB RAM is tight for Postgres in production**, even for one user's journal-scale data.
  `shared_buffers`/`work_mem` need conservative tuning (documented in the cloud-init config) to avoid
  OOM kills under load spikes like a full-history export or knowledge graph rebuild.
- **`terraform apply` via `workflow_dispatch` still requires a human to remember to run it** — there's
  no enforced review gate beyond "the plan output looks right to whoever clicks the button." For a
  single-operator project this is proportionate; it would not be for a team.

## Reconsideration Triggers
- Monthly Azure spend approaches or exceeds the $15 ceiling (e.g. Container Apps usage grows past the
  free grant) — first thing to check before adding budget is whether usage patterns changed
  meaningfully, not just raising the ceiling.
- A backup restore is ever attempted and fails, or a scheduled backup silently stops running —
  either should trigger moving to managed Postgres (accepting the ~$17+/mo cost) rather than
  investing further in self-managed backup tooling.
- User count grows meaningfully beyond "a few invited people," at which point the VM's single point
  of failure and 1GB RAM ceiling both become real constraints, not theoretical ones.
- Azure's Postgres Flexible Server free-tier terms change, or the subscription becomes eligible for
  the 12-month grant (e.g. a fresh subscription is created) — would remove the entire reason this ADR
  chose self-hosting over the managed service.

## Related Requirements
HELIX-SEC-002 (carried over from ADR-021 — this ADR is about where that requirement is deployed, not
a change to the requirement itself)
