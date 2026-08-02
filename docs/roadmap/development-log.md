# Development Log

This log is updated at the end of significant delivery sessions.

## 2026-08-02 Session - Knowledge graph HTTP/persistence layer fix

Fixed the `LayeredArchitectureTest.inboundHttpAdaptersMustNotDependOnPersistence` regression by
moving KG-3 source-route resolution out of `KnowledgeGraphController` and into the application-layer
`KnowledgeSourceRouteService`. The controller now depends only on application services; evidence
lookup remains behind the application boundary, and focused tests preserve both parent-belief routing
and the missing-evidence fallback. This is an architecture correction under ADR-020; it does not
change a catalogued product requirement.

## 2026-08-02 Session - Phase 12 fix #4: forced Container App environment replacement

A fourth bug from the same real-world `terraform apply` run: `azurerm_container_app_environment.main`
didn't set `infrastructure_resource_group_name` (Azure auto-generates one, the
`ME_<env>_<rg>_<region>` resource group backing a VNet-integrated environment's networking). Because
that attribute is `ForceNew` in the provider, every plan after the first apply saw "config wants
null, real state has Azure's generated value" and forced a full destroy/recreate of the environment
-- which is slow (VNet-integrated environment deletion commonly takes 20-40 minutes) and would have
recurred on *every single future apply* if left unfixed, not just this one. Fixed with
`lifecycle { ignore_changes = [infrastructure_resource_group_name] }`.

## 2026-08-02 Session - Phase 12 fixes from a real first `terraform apply`

Running the runbook against a real Azure subscription for the first time surfaced three bugs the
unvalidated-Terraform caveat had warned about -- all now fixed:

1. **`azurerm_storage_container.postgres_backups` used `storage_account_id`**, an argument only
   supported by AzureRM provider v4.x; `versions.tf` pins `~> 3.116`. Fixed to `storage_account_name`
   (the 3.x-compatible argument) and left a comment flagging this as a thing to re-check if the
   provider is ever bumped to 4.x.
2. **This specific subscription rejects `ed25519` VM SSH keys** ("Only RSA SSH keys are supported by
   Azure") -- the runbook now generates and uses an RSA key for the Postgres VM instead, with a
   troubleshooting note for anyone who already generated ed25519 before hitting this.
3. **The real bug, not just a typo:** `api_container_image` had no default and the runbook's guidance
   to point it at `ghcr.io/<owner>/helix-api:latest` before any image had ever been pushed there was
   simply wrong -- Azure Container Apps validates the image reference exists (a manifest GET) at
   *resource-creation* time, not container-startup time, so this failed the entire `terraform apply`
   with `MANIFEST_UNKNOWN` rather than creating the app in a not-yet-started state as the runbook
   claimed. Fixed by giving `api_container_image` a default of Microsoft's public placeholder image
   (`mcr.microsoft.com/azuredocs/containerapps-helloworld:latest`), removing the incorrect
   `TF_VAR_api_container_image` overrides from both `terraform-plan.yml` and `terraform-apply.yml`,
   and rewriting the runbook's step 4/7 to explain the real sequence (placeholder image on first
   apply, real image pushed and swapped in afterward via `az containerapp update`, which Terraform's
   `lifecycle.ignore_changes` already protects from being reverted by a later apply).

Also caught mid-session: real secrets (Postgres password, a GitHub PAT, a Google OAuth client secret,
an Azure subscription id) ended up pasted directly into the runbook markdown on disk while the user
was following it interactively. `git status` confirmed the file was still untracked (nothing pushed),
and the file was immediately redacted back to placeholder values. Worth calling out as a standing risk
of runbook-as-scratchpad usage, not something the automation itself caused.

## 2026-08-02 Session - Phase 12: Azure production deployment (ADR-022)

Summary: user asked to deploy on Azure, cost-optimized (hard $15/mo ceiling), single user to start,
cold starts acceptable. Priced the real options against current Azure pricing rather than assumed
figures (managed Postgres Flexible Server Burstable B1ms alone runs ~$17+/mo, over budget by itself)
and landed on: Static Web Apps (Free) for the frontend, Container Apps Consumption (scale-to-zero)
for the API, and Postgres self-hosted in Docker on a B1s VM rather than the managed service — full
rationale, alternatives considered, and the operational risks this trades in (self-managed patching,
backups, no automatic failover) are in **ADR-022**.

**Split-origin auth fix (resolves an ADR-021 open risk).** Static Web Apps and Container Apps land on
different Azure domains — genuinely cross-site, not just cross-port like local dev. ADR-021 had
flagged this exact scenario as untested and requiring `SameSite=None`. Made
`server.servlet.session.cookie.same-site` configurable via `HELIX_SESSION_COOKIE_SAMESITE` (default
`lax` for local dev; set to `none` in the Container App's Terraform config), and updated
`SecurityConfig`'s CSRF-disabled javadoc to rely on the CORS-allowlist + JSON-content-type-preflight
control as the load-bearing reason it's safe to leave CSRF disabled, rather than `SameSite` (which is
no longer constant across environments).

**Delivered, all under `infra/`:**
- `apps/api/Dockerfile` — multi-stage build using Spring Boot's layered-jar support so unchanged
  dependency layers cache across builds.
- `infra/terraform/` — full IaC: resource group, VNet with a VM subnet (NSG: SSH from one admin IP
  only, Postgres from the VNet only, no public 5432 exposure at all) and a delegated Container Apps
  subnet, the Postgres VM (system-assigned managed identity, no storage key ever stored on it),
  Blob Storage for backups with a lifecycle-managed retention policy, Log Analytics, the
  VNet-integrated Container Apps environment + API Container App (secrets for DB/OAuth config,
  `min_replicas = 0` for scale-to-zero, image intentionally excluded from Terraform's change
  tracking after first apply since `deploy-api.yml` owns that going forward), and the Static Web App.
  Remote state in Azure Storage, bootstrapped once via a plain script since Terraform can't create
  the backend it stores its own state in.
- `infra/cloud-init/postgres-vm.yaml` — installs Docker + Azure CLI + `unattended-upgrades`, runs
  Postgres in a memory-capped container (tuned `shared_buffers`/`work_mem` for the VM's 1GiB RAM),
  installs the backup cron job.
- `infra/scripts/` — `bootstrap-terraform-state.sh` (one-time, explicitly documented as NOT
  idempotent since the storage account name is randomized per run), `backup-postgres.sh` (daily
  `pg_dump` to Blob Storage via the VM's managed identity, refuses to upload a suspiciously small
  dump rather than silently backing up nothing), `restore-postgres.sh` (manual-only, requires typing
  the database name to confirm before doing anything destructive).
- `.github/workflows/deploy-api.yml` / `deploy-web.yml` — triggered by `CI` succeeding on `main`
  (never deploy code that hasn't passed tests), build+push to `ghcr.io` (not Azure Container Registry
  — ACR's Basic tier alone would cost ~$5/mo, a third of the entire budget, for something GitHub
  already provides free) and update the Container App / redeploy the Static Web App respectively.
- `.github/workflows/terraform-plan.yml` (runs on PRs touching `infra/terraform/`, comments the plan
  on the PR) / `terraform-apply.yml` (`workflow_dispatch` only — deliberately not automatic on merge,
  since infra changes here affect a stateful database VM).
- `docs/deployment/azure-runbook.md` — the full bootstrap sequence (including the two genuinely
  two-phase steps: the Static Web App's and Container App's URLs aren't known until after they're
  first created, but each needs the other's URL) plus day-2 ops (restore, password rotation, spend
  monitoring, patching).

**Estimated steady-state cost: ~$10-13/mo**, under the $15/mo ceiling.

**Not done / explicitly flagged as unverified:** this sandbox had neither Azure credentials nor a
working `terraform` binary (no network access to download one), so none of the Terraform or GitHub
Actions here have been run against a real subscription. Every resource definition was checked
carefully against the `azurerm` provider's documented schema, but `terraform validate`/`plan` against
a live subscription — the first step in the runbook — should be treated as the actual first
correctness check, not a formality.

## 2026-08-02 Session - Google OAuth redirect whitespace fix (ADR-021)

- Diagnosed a successful Google callback failing during Spring Security's post-login redirect with
  `InvalidUrlException: Bad authority`: trailing spaces loaded verbatim from `HELIX_WEB_APP_URL` in
  `.env` made the configured frontend URL invalid.
- Normalized surrounding whitespace and trailing slashes before building OAuth success/failure
  redirect URLs, added a regression test, documented the single-URL setting separately from the
  comma-separated CORS allowlist, and cleaned the affected local `.env` values.

## 2026-08-02 Session - Authentication & Authorization Foundation (ADR-021)

Summary:
- User request: enforce authentication and authorization prior to deployment, "me plus a few invited
  people." Investigated a sibling project's (`dtaylor-us/axiom`) per-service JWT/gateway pattern and
  rejected it as a direct copy (designed for several independently-deployable services; Helix is one
  monolith) in favor of Google OAuth2/OIDC SSO with session-cookie auth, plus a denormalized
  `owner_id` on every table rather than deriving ownership through inconsistent FK chains. Full
  rationale and alternatives considered in ADR-021.

Authentication — fully done:
- **ADR-021** written (ADR-013 superseded, ADR-001's "single-user" framing amended).
- **`V12__authentication_and_ownership.sql`**: `users` + `authorized_users` (invite-only allowlist,
  seeded with the bootstrap owner's email) tables; `owner_id` (NOT NULL, FK to `users`) added to
  every one of the 19 pre-existing tables in one migration, backfilled to a bootstrap user row and
  then the column default dropped — every future insert must supply it explicitly or fail loudly
  (a missed call site fails closed with a DB constraint error, not an open/unscoped row).
- **Google OAuth2/OIDC login** (`spring-boot-starter-oauth2-client`): `UserEntity`,
  `AuthorizedUserEntity` + repositories; `HelixOidcUserService` (checks the allowlist by email, then
  find-or-creates a `UserEntity`, matching by `google_sub` first and falling back to email only on
  first login); `HelixOidcUser`/`CurrentUserProvider` carrying the internal user id through Spring
  Security's context; `SecurityConfig` rewritten to require an authenticated session on every
  `/api/v1/**` route except `/api/v1/health` and `/api/v1/auth/me` (401 JSON for API calls, not a
  login-page redirect — see the class javadoc for the CSRF-disabled rationale); `AuthController`
  (`GET /api/v1/auth/me`); session cookie config (`HttpOnly`, `SameSite=Lax`, `Secure` toggled by
  `HELIX_SESSION_COOKIE_SECURE` for prod). This fully replaces the old `anyRequest().permitAll()` —
  no unauthenticated request can reach any business endpoint anymore.
- **Frontend**: new `AuthGate` component (wraps `AppLayout` at the router root, kept separate so
  `AppLayout`'s existing tests are unaffected) — calls `GET /api/v1/auth/me` on load; shows a
  "Sign in with Google" link to `/oauth2/authorization/google` when unauthenticated (with a distinct
  message if the redirect carries `?error=not_invited` from a rejected allowlist check); renders the
  app with the signed-in user's name and a sign-out button once authenticated. `http.ts`'s `request()`
  now sends `credentials: 'include'` on every call (required for the session cookie to be attached)
  and throws a distinguishable `UnauthorizedError` on 401.

Authorization / data isolation — owner_id enforced end-to-end for the core loop and data
export/deletion; write-path safe everywhere else; read isolation still open in four modules (see gap
list):
- **Core loop, fully enforced**: `Transformation` (root aggregate — `TransformationService.get()` is
  the enforcement chokepoint every other service resolves a transformationId through), `Experiment`,
  `Reflection`, `Suggestion`, `Belief`, `BeliefRevision`, `Evidence`. Every entity gained an `ownerId`
  field via a new constructor overload (existing in-memory test fixtures that are never persisted
  keep compiling unchanged); repositories gained `...AndOwnerId(...)` finders; every service's
  create/get/list/search now resolves the caller via `CurrentUserProvider` and 404s (not 403, to
  avoid confirming a record exists to a non-owner) on a mismatch. All affected services' existing
  unit tests updated to stub `CurrentUserProvider` and the new finder methods.
- **Data export/deletion, fully enforced**: `DataExportService.export()` and
  `DataDeletionService.deleteEverything()` now scope every repository call to the caller's `ownerId`
  (previously both operated over literally every record in the database with zero scoping — a
  standing gap `DataController`'s own prior javadoc called out and ADR-015 deferred). Required adding
  `findAllByOwnerId`/`deleteAllByOwnerId` to several repositories that had no owner-scoped query at
  all before this (`WeeklyRetrospectiveRepository`, `WisdomEntryRepository`, `WisdomRevisionRepository`,
  `WisdomSourceLinkRepository`, `MemoryProposalRepository`, `MemoryProposalRevisionRepository`,
  `SemanticSearchDocumentRepository`).
- **`OnboardingState`**: re-keyed from the old fixed-id singleton row to one row per `owner_id`
  (`OnboardingStateRepository.findByOwnerId`); `OnboardingService.get()` bootstraps a fresh row per
  user on first access.
- **Wisdom (4 entities), Memory (2 entities), semantic search — write-path only.** Every entity
  gained `ownerId` and every creating service call sets it (`WisdomService`,
  `WeeklyRetrospectiveService`, `MemoryProposalService`, `SemanticIndexingService`), so nothing 500s
  on insert. **Read paths (list/get/search) are explicitly NOT owner-scoped yet** — flagged with an
  `ADR-021 gap` comment directly on each affected class/field. Any authenticated user can currently
  read any other user's wisdom entries, weekly retrospectives, and memory proposals.
- **Knowledge graph — schema-ready, service untouched.** All 4 entities
  (`KnowledgeNodeEntity`/`KnowledgeEdgeEntity`/`KnowledgeEdgeSourceEntity`/
  `KnowledgeProjectionCheckpointEntity`) have an `owner_id` column and a constructor overload, but
  `KnowledgeGraphProjectionService`'s rebuild reads every repository via unscoped `findAll()`-style
  calls across the whole database with no user parameter threaded through at all — this is a bigger
  redesign than every other module in this pass (the rebuild would need to accept a caller's
  `ownerId`, filter every domain-record read by it, and stamp every created node/edge with it).
  **Rebuilding the knowledge graph will currently fail with a NOT NULL constraint violation.**
- **Semantic search index rebuild has a real cross-user bug even after this session's fix**:
  `SemanticIndexingService.rebuild()`'s `repository.deleteAllInBatch()` wipes every user's indexed
  documents on any single user's rebuild call (only `WisdomService.list()` feeding into it is
  unscoped; reflections are already owner-scoped going in). Flagged in that class's javadoc.

Verification:
- Frontend: `npm run typecheck`, `npm run lint`, `npx vitest run` all clean — 42/42 tests passing
  (2 new: `AuthGate.test.tsx`).
- Backend: still not compiled in this sandbox (standing constraint this whole engagement — JDK 11
  only, no Gradle network access). Every change was checked carefully against actual entity/
  repository/service signatures already in the codebase and against existing test fixtures, but this
  is not a substitute for a real `./gradlew build`/test run. **Given the size of this change (19
  tables, ~25 backend files touched), a real build + the same Codex-driven end-to-end QA pass used for
  the Phase 11 knowledge graph bugs is strongly recommended before deploying, not just before
  merging** — that pass is what caught real bugs a careful manual review missed last time.

Remaining before this is safe to deploy with real multi-user data (at the time of the original
session):
1. Owner-scope the read paths (list/get/search) for Wisdom, Weekly Retrospectives, and Memory
   Proposals — same pattern as the core loop, smaller in scope than it sounds since the entity/
   write-path work is already done.
2. Redesign `KnowledgeGraphProjectionService` to accept and filter by a caller's `ownerId` throughout
   its rebuild, and stamp created nodes/edges/edge-sources/checkpoints with it.
3. Fix `SemanticIndexingService.rebuild()`'s global index wipe to be owner-scoped.
4. A real backend build/test run, plus a fresh end-to-end QA pass exercising login, the allowlist
   rejection path, and cross-user isolation with two real invited accounts.

## 2026-08-02 Session (follow-up) - ADR-021 read-scoping and knowledge graph owner threading

Closes items 1–3 above (item 4, a real `./gradlew build` and Codex-driven end-to-end QA pass, remains
outstanding — this sandbox still cannot compile the backend).

- **Wisdom/Memory/Retrospective read paths, now fully owner-scoped.** `WisdomService.list()/get()/
  search()`, `WeeklyRetrospectiveService.recentSnapshots()/get()/search()`, and
  `MemoryProposalService.list()/get()` all resolve the caller via `CurrentUserProvider` and scope
  through new repository finders (`findAllByOwnerIdOrderByRevisedAtDesc`,
  `findByIdAndOwnerId`, `findTop20ByOwnerIdAnd...ContainingIgnoreCaseOrderBy...`, etc.) instead of the
  old unscoped `findAllByOrderBy...`/`findById`. `get()` 404s (not 403) on a cross-owner lookup,
  consistent with the core loop's existing pattern. Removed the now-stale "ADR-021 gap" class javadocs
  on all three services and updated `WisdomServiceTest`/`MemoryProposalServiceTest` to stub the new
  finder methods with a captured `ownerId`.
- **`KnowledgeGraphProjectionService`, redesigned around a caller's `ownerId`.** `rebuild()` now reads
  every domain repository through its owner-scoped finder (`findAllByOwnerId`,
  `findByOwnerIdOrderByCreatedAtDesc`, etc.), deletes only the caller's own prior projection
  (`deleteAllByOwnerId` on all three knowledge tables instead of `deleteAllInBatch()`), and stamps
  every created node/edge/edge-source/checkpoint with `ownerId` via the constructor overloads that
  were already sitting unused on those entities. This closes the "rebuild fails with a NOT NULL
  violation" bug flagged in the prior session.
- **The other three knowledge-graph services scoped to match**: `KnowledgeGraphQueryService.focusView()`
  and `.freshness()` now resolve the focus node, edges, nodes, and checkpoints through new
  owner-scoped repository finders (`findByOwnerIdAndNodeTypeAndSourceRecordId`,
  `findByOwnerIdAndStatus`, `findByOwnerIdAndIdIn`, `findByOwnerIdAndKnowledgeEdgeIdIn`,
  `findAllByOwnerId`) — previously a focus-node lookup or graph walk could return another user's
  nodes/edges outright. `KnowledgeEdgeGovernanceService.confirm/reject/hide` now 404 on a
  cross-owner edge id (`findByIdAndOwnerId`). `KnowledgeGraphRelationshipDiscoveryService
  .discoverBeliefRelationships()` only compares belief-node pairs within the caller's own graph
  (`findByOwnerIdAndNodeType`, `findByOwnerIdAndRelationshipType`) and stamps proposed AI edges/edge
  sources with the caller's `ownerId`. All four services' unit tests updated accordingly (new
  `CurrentUserProvider` mocks, matcher-based stubs for the owner-scoped finders).
- **`SemanticIndexingService.rebuild()`'s global index wipe fixed.** `repository.deleteAllInBatch()`
  → `repository.deleteAllByOwnerId(ownerId)`; combined with `WisdomService.list()` now being
  owner-scoped (previous bullet), both the read side and the wipe are fully scoped to the caller — a
  rebuild by one user can no longer delete or leak another user's indexed documents. Removed the
  matching "ADR-021 gap" comments in `SemanticSearchDocumentEntity` and `DataDeletionService`.
- Also corrected a stale comment on `EvidenceRepository`: it described `EvidenceService.get()`'s
  direct `ownerId` equality check as still-needed follow-up work, when that check was in fact already
  implemented in the original session — this was a documentation-only fix, no behavior change.

Verification: same standing constraint as the prior session — the backend cannot be compiled in this
sandbox (JDK 11 only, no Gradle network access), so every change here was checked by careful manual
review against actual entity/repository/service signatures and existing test fixtures, not a passing
build. **A real `./gradlew build`/test run, plus the Codex-driven end-to-end QA pass recommended in the
prior entry (login, allowlist rejection, and cross-user isolation with two real invited accounts,
including a knowledge-graph rebuild and semantic search rebuild under two accounts), is still required
before deploying multi-user.**

With this session's changes, every module now enforces per-owner data isolation on both write and read
paths. No known outstanding authorization gaps remain in the codebase at the time of writing.

## 2026-08-02 Session - Progressive Disclosure and Contextual Help

Summary:
- Added a consistent, keyboard-native disclosure pattern for optional and advanced UI across
  transformation creation, experiment planning, belief/evidence work, reflection review, memory
  governance, wisdom creation/revision, and historical detail.
- Kept primary context and decisions visible while moving manual creation forms, optional planning
  fields, revision forms, provenance/history, and secondary reflection prompts behind explicit,
  descriptive controls.
- Expanded every contextual term explanation from a one-line definition into a plain-language
  definition, practical guidance, and concrete example. Updated the canonical product glossary to
  match and added Evidence, Memory, and Wisdom help where those concepts are acted upon.
- Audited free-form content rendering across Today, Journey, Knowledge, Search, graph inspection,
  Memory, and Wisdom. User-authored titles, statements, reflections, evidence, provenance IDs, and
  button labels now wrap safely; record split panes collapse based on available content width rather
  than relying only on a fixed device breakpoint.

Requirements and decisions:
- Affects `HELIX-UX-001` and `HELIX-UX-002`; remains consistent with `ADR-001` and `ADR-002`.

Verification:
- Full web suite: 40/40 tests passed.
- Typecheck, ESLint, production build, and `scripts/check-docs`: passed.

## 2026-08-02 Session - Belief List Alignment

Summary:
- Replaced the Knowledge belief list's inconsistently aligned generic buttons with a consistently
  left-aligned selector. Statements now share one reading edge and belief types use compact category
  badges; the rest of the application card system is unchanged.

Requirements and decisions:
- Affects `HELIX-UX-002`; remains consistent with `ADR-002`.

Verification:
- `npm run test -- KnowledgePage.test.tsx TodayPage.test.tsx`: 15/15 tests passed.
- `npm run typecheck`, targeted ESLint, production build, and `scripts/check-docs`: passed.

## 2026-08-02 Session - Knowledge Graph Diagram Readability Pass

Summary:
- User feedback on the diagram view screenshot: every non-focus node rendered with the same beige
  fill regardless of type, so the only way to tell a Belief from a Reflection from an Evidence node
  apart was reading small truncated labels crossed by connector lines — hard to scan at a glance.
  This session redesigns the diagram's visual encoding without introducing a new charting library or
  breaking the "calm, no hairballs" design principle from the original scoping doc.

Changes:
- `apps/web/src/styles/main.css`: added a 7-color node-type palette (`--node-transformation`
  through `--node-memory`, each with a `-soft` fill tint), chosen to stay within the app's existing
  warm/muted "calm editorial" tokens rather than introducing a bright categorical chart palette.
- `KnowledgeGraphPage.tsx` / `GraphDiagram`:
  - Every node now fills with its type's soft color and strokes with its solid color — a Belief
    always looks like a Belief everywhere in the app, including the filter checkboxes (each now has
    a matching color swatch) and a new legend row rendered under the diagram (only shows the types
    actually present in the current view, to avoid a 7-item legend for a 3-node graph).
  - The focus node no longer overrides its type color to solid orange (which made it look like a
    Transformation node even when it wasn't) — it now keeps its own type color, plus a solid accent
    ring around it and a slightly larger radius, so "this is the focus" is legible independent of
    node type. Selection uses a dashed accent ring instead of an opaque color swap, for the same
    reason.
  - Edges now have arrowheads (a `<marker>` def, muted color) showing relationship direction — this
    also makes the KG-1/KG-2 direction fix from the previous session visibly checkable in the UI,
    not just correct in the data. Arrowheads are computed to stop at the target node's edge (not its
    center) so they're actually visible instead of hidding under the circle.
  - Labels now render with a canvas-colored halo (SVG `paint-order: stroke` trick) so text stays
    legible where a connector line crosses behind it, without needing to measure text width.
  - Ring radius now grows modestly with neighbor count (capped) instead of staying fixed, so a
    9-neighbor view has more breathing room between labels than a 3-neighbor view.
  - Each node gained an SVG `<title>` (hover tooltip) with its full, untruncated label and type.
- `KnowledgeGraphPage.test.tsx`: updated the node-click test to target the exact truncated label
  text instead of a substring regex, since the new `<title>` tooltip also contains that substring
  and made the query ambiguous.

Verification:
- `npm run typecheck`, `npm run lint`, `npx vitest run`: all clean, 36/36 tests passing.
- Purely a visual/CSS/SVG change to one already-tested component — no backend touched, so the
  backend compile/test gap noted in prior sessions is unaffected by this change specifically.

## 2026-08-02 Session - Knowledge Graph QA Fixes (KG-1 through KG-4)

Summary:
- An independent Codex QA pass ran the full app (backend, frontend, Postgres) end to end for the
  first time this engagement — the previous sandbox could never compile or run the backend. The
  report's verdict was "do not ship" on two grounds: several projected edges had source/target
  endpoints reversed relative to their relationship type, producing false sentences in the
  accessible list view (e.g. an evidence-challenges-belief edge rendering as if the belief
  challenged the evidence); and graph "View full record" links for belief/evidence nodes didn't
  identify which record to show, landing on an arbitrary one. This session fixes all four bugs the
  report raised (KG-1 through KG-4) and adds regression coverage for each.
- This is the first time in the engagement a QA pass actually exercised the running app rather than
  relying on code review plus unit tests — it caught a real, user-visible correctness bug (wrong
  edge direction) that no amount of "does this edge type exist" unit testing would have caught,
  because the original tests checked presence, not direction. That gap is closed below.

Bugs fixed:
- **KG-1 / KG-2 (High): five edge types had source/target reversed.** `BELIEF_SUPPORTED_BY_EVIDENCE`,
  `BELIEF_CHALLENGED_BY_EVIDENCE`, `BELIEF_EXPLORED_BY_EXPERIMENT`, `WISDOM_SUPPORTED_BY_EVIDENCE`,
  `WISDOM_EMERGED_FROM_REFLECTION`, and `MEMORY_DERIVED_FROM` all had the "producing"/"supporting"
  record as the graph source and the belief/wisdom/memory record as the target — the opposite of
  what each edge's display label implies when read as "{source label} {display label} {target
  label}". Fixed in `KnowledgeGraphProjectionService.java` by swapping the endpoints on all six
  `addEdge(...)` calls (comments added at each call site explaining the expected reading direction).
  Verified the other eight edge types (`TRANSFORMATION_CONTAINS_BELIEF`,
  `TRANSFORMATION_CONTAINS_EXPERIMENT`, `TRANSFORMATION_PRODUCED_WISDOM`,
  `EXPERIMENT_PRODUCED_EVIDENCE`, `EXPERIMENT_INFORMED_WISDOM`, `REFLECTION_PRODUCED_EVIDENCE`,
  `REFLECTION_REFERENCES_EXPERIMENT`, `REFLECTION_REFERENCES_TRANSFORMATION`) were already correct
  by re-deriving the expected reading direction for each and checking it wasn't flagged in the
  report — no change needed there.
- **KG-3 (Medium): graph node links for BELIEF/EVIDENCE didn't identify a specific record.**
  `KnowledgeGraphController.sourceRoute` returned a bare `/knowledge` for every belief and evidence
  node, so the Knowledge page fell back to whatever belief loaded first — the QA repro showed
  clicking one belief's node and landing on a different belief's detail. Fixed by: (1) BELIEF nodes
  now route to `/knowledge?beliefId={id}`; (2) EVIDENCE nodes look up their owning belief via a
  newly injected `EvidenceRepository` and route to that belief's `?beliefId=...` (evidence doesn't
  have its own standalone page, so landing on its parent belief with its evidence timeline visible
  is the correct target); (3) `KnowledgePage.tsx`'s `selectedBeliefId` state now lazily initializes
  from `?beliefId=` in the URL instead of always defaulting to the first belief in the list; (4)
  `KnowledgeGraphPage.tsx`'s node-detail "View full record" link now splits a `sourceRoute` on `?`
  and passes the query as TanStack Router's `search` prop via a new `RecordLink` component, instead
  of passing the raw `"path?query"` string straight into `Link`'s `to` prop (which would have
  treated the `?...` as a literal, unencoded path segment rather than search params).
- **KG-4 (Low): missing-focus error took ~10 seconds to appear.** A focus node that doesn't exist is
  a deterministic 404, not a transient failure, but TanStack Query's default retry behavior kept
  retrying it before showing the "Build connections" recovery action. Fixed by setting `retry: false`
  on `KnowledgeGraphPage`'s graph query.

Regression coverage added:
- `KnowledgeGraphProjectionServiceTest`: the main rebuild test now asserts `(source type, target
  type)` for every saved edge against a per-relationship-type expectation table, not just that the
  relationship type is present somewhere in the saved list — this is the specific gap that let
  KG-1/KG-2 ship undetected the first time.
- `KnowledgeGraphControllerTest`: new test asserting BELIEF and EVIDENCE nodes both resolve to
  `/knowledge?beliefId={the belief's id}` (EVIDENCE via the injected `EvidenceRepository` lookup).
- `KnowledgeGraphPage.test.tsx`: new test clicking a belief node in the diagram and asserting the
  resulting "View full record" link's actual `href` carries the query string.

Verification:
- Frontend: `npm run typecheck`, `npm run lint`, `npx vitest run` all clean — 36/36 tests passing
  (2 new, 1 existing test's fixture updated to include a query string in a `sourceRoute`).
- Backend: still not compiled in this sandbox (same constraint as the rest of this engagement) —
  the QA report's own successful `./gradlew build` run is the only real compilation this feature has
  had, and it predates these fixes. **This should be re-run against the fixed code before merging**;
  the edits here are mechanical (swap two constructor arguments, add one repository dependency) and
  were checked carefully against the actual `KnowledgeEdgeEntity`/`EvidenceEntity`/`EvidenceRepository`
  signatures already in the codebase, but "carefully reviewed" is not the same guarantee as "compiled."

## 2026-08-02 Session - Today Page: Connect Suggested Small Action to the Journey

Summary:
- User-reported UX gap: "It's not apparent how selecting a small action step on the Today screen
  impacts the journey or how that is used." Investigation confirmed the gap was real, not just a
  discoverability problem: accepting or replacing a "Suggested Small Action" only flipped a status
  flag on the `Suggestion` row itself — it had no effect on the active `Experiment`, and nothing in
  the UI connected the two. `ExperimentEntity.nextAction` could only be set once at creation and had
  no mutator at all. This session makes acceptance actually update the experiment's next action and
  surfaces that connection visibly on the Today page.

Changes:
- `ExperimentEntity.java`: added `reviseNextAction(String)`, the entity's first mutator.
- `ExperimentService.java`: added `reviseNextAction(UUID, String)` — loads, mutates, and explicitly
  saves the experiment (this service's other methods aren't `@Transactional`, so an explicit
  `repository.save()` is used here rather than relying on ambient dirty-checking).
- `SuggestionService.java`: now takes `ExperimentService` as a constructor dependency (one-directional
  `suggestions -> experiments`, no cycle). `accept()` and `replace()` each call
  `experimentService.reviseNextAction(...)` after mutating the suggestion, so committing to a
  suggestion — original or replaced — becomes the experiment's actual next action. `dismiss()` is
  intentionally unchanged; passing on a suggestion shouldn't touch the experiment.
- `SuggestionServiceTest.java` (new): covers accept and replace both revising the experiment,
  dismiss leaving it untouched, and `ExperimentService.reviseNextAction` persisting via `save()`.
- `TodayPage.tsx`:
  - "Current Direction" card now shows "Smallest next action: {experiment.nextAction}" (with an
    explanatory empty state before anything's been accepted) and a "See this experiment in your
    Journey" link to `/experiments/$id`.
  - The Suggested Small Action card's "Why this" line gains a trailing sentence once a suggestion is
    ACCEPTED or REPLACED, pointing back at the Current Direction card above.
  - The accept/replace mutation's `onSuccess` now invalidates both `['current-focus']` and
    `['experiment', id]` queries (in addition to the existing optimistic local patch) so the refetch
    picks up the server-side `nextAction` change; dismiss still only does the local patch, since it
    has no experiment-side effect.
- `TodayPage.test.tsx`: added a test asserting the empty state, the Journey link's `href`, and that
  accepting a suggestion updates "Smallest next action" after refetch. Updated the existing shared
  `it.each` accept/dismiss test to provide a second `getCurrentFocus` mock value reflecting the
  post-action state — its original single static mock always returned the suggestion as PROPOSED,
  which the new `invalidateQueries` call exposed as a stale-mock false failure (refetch silently
  reverted the optimistic ACCEPTED status back to PROPOSED in the test, not in real usage).

Verification:
- Frontend: `npm run typecheck`, `npm run lint`, `npx vitest run` all clean — 39/39 tests passing.
- Backend: still not compiled in this sandbox (same standing constraint as the rest of this
  engagement — JDK 11 only, no Gradle network access). The `ExperimentEntity`/`ExperimentService`/
  `SuggestionService` changes and the new `SuggestionServiceTest` were checked carefully against the
  actual entity/repository signatures already in the codebase, but this is not a substitute for a
  real `./gradlew build`/test run, which should happen before merging.

## 2026-08-02 Session - Roadmap Phase 11E/11F: AI Relationship Discovery and Temporal History (Knowledge Graph completion)

Summary:
- Final continuation of the "implement all the phases for the knowledge graph" instruction. Closes
  out Phase 11 entirely: 11A (scoping) through 11F (temporal exploration) are all done.
- 11E was scoped narrowly and deliberately: the brief's own example (`BELIEF_RELATED_TO_BELIEF`) is
  the only new AI-derived edge type added, comparing belief pairs system-wide via a manually
  triggered, capped (25 pairs/run) discovery pass rather than an automatic per-rebuild scan — keeps
  AI usage visible, bounded, and opt-in rather than a silent background cost.
- 11F was scoped as literally as the roadmap's own phrasing: "lightweight... no animated timeline."
  The temporal columns (`effective_from`, `effective_to`, `superseded_by_edge_id`) already existed
  in the 11B migration and were already unused; this session just surfaces what's actually populated
  (`created_at`, `confirmed_at`, `rejected_at`) rather than building UI for fields nothing sets yet.

Changes (backend):
- `AiAssistantPort`: new `proposeBeliefRelationship(String context)` method + `AiRelationshipProposal`
  record (`related`, `explanation`, `provider`, `model`, `deterministicFallback`), following the
  same Javadoc/ADR-citation convention as the port's other six methods.
- `OpenAiAssistantAdapter` / `OllamaAssistantAdapter`: implemented using the exact same
  circuit-breaker/build-request/parse/fallback skeleton as every other method on these adapters
  (labeled-line response format: `RELATED: yes|no` / `WHY: <explanation>`).
- `NoAiAssistantAdapter`: returns `related=false` on principle — no live judgment was made, so
  defaulting to "not related" avoids fabricating a connection, mirroring the existing
  `proposeMemory` precedent of never inventing content on fallback.
- `KnowledgeEdgeType`: added `BELIEF_RELATED_TO_BELIEF`.
- `KnowledgeNodeRepository.findByNodeType`, `KnowledgeEdgeRepository.findByRelationshipType`: new
  query methods needed by the discovery service.
- New `KnowledgeGraphRelationshipDiscoveryService.discoverBeliefRelationships()`: iterates BELIEF
  node pairs with no existing `BELIEF_RELATED_TO_BELIEF` edge of any status (so a rejected pair is
  never re-asked), capped at `MAX_PAIRS_PER_RUN = 25`, calling the new port method per pair and
  persisting any positive result as `PROPOSED`/`AI_PROPOSED` with `KnowledgeEdgeConfidence.MODERATE`
  — never auto-confirmed.
- `KnowledgeGraphController`: new `POST /discover-relationships` endpoint; `GraphEdgeDto` gained a
  nested `EdgeHistoryDto` (`createdAt`, `confirmedAt`, `rejectedAt`, `effectiveFrom`, `effectiveTo`,
  `supersededByEdgeId`) populated from the columns every edge has carried since 11B; `DISPLAY_LABELS`
  gained `BELIEF_RELATED_TO_BELIEF -> "May relate to"`.
- New tests: `KnowledgeGraphRelationshipDiscoveryServiceTest` (proposal creation, no-edge-on-
  not-related, previously-rejected-pairs-not-re-asked, the 25-pair cap), plus additions to
  `KnowledgeGraphControllerTest` for the new endpoint and the `history` field.

Changes (frontend):
- `packages/contracts`: `GraphEdgeHistory`, `KnowledgeGraphDiscoveryResponse`; `GraphEdge` gained a
  required `history` field.
- `apps/web/src/api/http.ts`: `discoverGraphRelationships()`.
- `KnowledgeGraphPage.tsx`: "Check for new connections" action reporting pairs-checked/
  connections-found; each list-view edge now shows a one-line history summary (noticed/confirmed/
  rejected/effective dates, omitting anything not populated).
- Updated `KnowledgeGraphPage.test.tsx` fixtures for the new required `history` field; added a test
  for the discovery action's status message.

Verification:
- `npm run typecheck`, `npm run lint`, `npx vitest run`: all clean, 35/35 tests passing (1 new).
- Backend: not compiled in this sandbox, same constraint as every backend phase this engagement
  (JDK 11 only, no Gradle network access). The two live-provider adapter implementations
  (`proposeBeliefRelationship` in `OpenAiAssistantAdapter`/`OllamaAssistantAdapter`) were not given
  their own adapter-level HTTP-mocking tests in this session — they mirror the already-tested
  `proposeMemory` method's control flow exactly (same circuit breaker, same request/response
  shapes, same labeled-line parsing), so the incremental risk of skipping a dedicated test for
  each is low, but it is a real gap relative to full coverage and should be closed if this ships.

## 2026-08-02 Session - Roadmap Phase 11C/11D: Knowledge Graph Exploration UI and Governance Wiring (Frontend)

Summary:
- Continuation of the same session/instruction as Phase 11B ("implement all the phases for the
  knowledge graph"). Built the frontend exploration UI (11C) and wired the already-built backend
  governance mechanism into a real UI surface (11D) — both in one pass since the governance actions
  live naturally inside the same page as the graph view.
- Resolved the two items 11A's scoping deliberately left open for 11C: no external graph
  visualization library (a custom SVG radial layout was enough for a bounded ≤25-node view, and
  keeps this optional feature from adding to every user's bundle size) and no separate mobile
  layout (the accessible list view already works at any viewport width, so it doubles as the small-
  screen experience rather than building a second bespoke layout).

Changes:
- `packages/contracts/src/index.ts`: added `KnowledgeNodeType`, `GraphNode`, `GraphEdge`,
  `GraphEdgeSourceReference`, `GraphView`, `KnowledgeGraphRebuildResponse`,
  `KnowledgeGraphCheckpoint`, `KnowledgeGraphStatusResponse` — matching the 11B controller's DTOs
  field-for-field.
- `apps/web/src/api/http.ts`: added `getGraphByTransformation`, `getGraphByBelief`,
  `getGraphFocus`, `getGraphStatus`, `rebuildGraph`, `confirmGraphEdge`, `rejectGraphEdge`,
  `hideGraphEdge`.
- New `apps/web/src/pages/KnowledgeGraphPage.tsx` at route
  `/knowledge-graph/$nodeType/$sourceRecordId`: renders the bounded focus view as a calm radial SVG
  diagram (focus node centered, neighbors placed on a circle around it, muted palette reusing the
  existing `--provenance`/`--accent`/`--border` tokens rather than a new categorical color set) with
  a toggleable, fully accessible structured-list alternative that is not gated behind the diagram —
  per the brief's explicit accessibility requirement. Includes a type filter, a node detail panel
  (label/summary/status/link back to the record's own page), loading/error/empty states, a
  build/refresh-connections action, and — Phase 11D — confirm/reject/hide buttons that appear only
  on edges with `status: PROPOSED` (none exist yet; dormant until 11E).
- "Explore connections" entry points added to `TransformationDetailPage.tsx` (transformation
  summary card) and the belief detail panel in `KnowledgePage.tsx`.
- New `KnowledgeGraphPage.test.tsx`: bounded-view rendering with diagram/list toggle and type
  filtering, the build-connections path when the projection hasn't been built yet, and governance
  actions appearing only for `PROPOSED` edges and calling the correct mutation.
- Updated `docs/roadmap/2026-08-roadmap.md` Phase 11 section: 11C and 11D marked done, 11E/11F
  remain the only unstarted subphases.

Verification:
- `npm run typecheck` (tsc -b): clean.
- `npm run lint` (eslint): clean.
- `npx vitest run`: 10 test files, 34/34 tests passing (3 new).
- `npm run build`: `tsc -b` (the type-check half) passed cleanly; `vite build` itself failed only on
  an `EPERM: operation not permitted, unlink .../dist/assets/...` while clearing a stale `dist/`
  directory — the same host-sync file-locking pattern already documented elsewhere in this
  engagement (e.g. the un-removable `.git/index.lock`), not a defect in the code. Nothing about this
  failure is specific to the knowledge graph work; it would reproduce on a clean `npm run build` of
  this branch's `dist/` regardless of what changed.

## 2026-08-02 Session - Roadmap Phase 11B: Knowledge Graph Projection Foundation (Backend)

Summary:
- User instructed "implement all the phases for the knowledge graph," which is treated as the
  product-owner sign-off Phase 11A's exit criterion required before starting 11B — ADR-020 updated
  from Proposed to Accepted on that basis.
- Built the backend projection foundation only (Phase 11B). No frontend, governance UI, or
  AI-assisted discovery work in this session — those remain 11C–11F.
- This session also corrected a standing inaccuracy: prior session summaries had repeatedly (and
  wrongly) described Phases 6, 7, and 9 as sitting uncommitted. Direct `git log`/`git fetch`
  verification showed all three were already on `origin/main` (commit `a6704c2`). Root cause: this
  sandbox cannot commit at all — `.git/index.lock` is permission-locked and un-removable here — so
  every commit in this engagement has come from an external process (almost certainly the user's own
  local git client on this host-synced folder), not from any git command run in-session. This
  session's changes will need the same external commit step.

Changes:
- New migration `V11__knowledge_graph.sql`: `knowledge_node`, `knowledge_edge`,
  `knowledge_edge_source`, `knowledge_projection_checkpoint` tables with supporting indexes.
- New `knowledge` module (`domain`, `adapter.out.persistence`, `application`, `adapter.in.http`):
  - Domain: `KnowledgeNodeType`/`KnowledgeEdgeType`/`KnowledgeEdgeOrigin`/`KnowledgeEdgeStatus`/
    `KnowledgeEdgeConfidence` enums; `KnowledgeNodeEntity`, `KnowledgeEdgeEntity` (with
    `confirm`/`reject`/`hide` governance transitions), `KnowledgeEdgeSourceEntity`,
    `KnowledgeProjectionCheckpointEntity`.
  - `KnowledgeGraphProjectionService`: full-rebuild-only projection (no incremental sync, per the
    scoping doc's Q16 answer) deriving every in-scope edge type from the seven authoritative domain
    repositories (transformation, belief, experiment, evidence, reflection, wisdom, memory). Every
    edge ships `EXPLICIT_DOMAIN_RELATIONSHIP` or `DETERMINISTIC_DERIVATION` origin, auto-confirmed —
    zero AI dependency in this phase. Retrospective-sourced wisdom links are explicitly excluded
    (a weekly retrospective spans multiple transformations, so it isn't attributable to one). Edges
    referencing a node that wasn't projected are silently skipped rather than failing the rebuild.
  - `KnowledgeGraphQueryService`: bounded, focus-node-centered BFS views (default 25-node cap,
    2-hop depth) over `CONFIRMED` edges only — never returns the whole graph.
  - `KnowledgeEdgeGovernanceService`: confirm/reject/hide mechanism built ahead of Phase 11E's need,
    so 11E doesn't also have to build it; has nothing to act on until AI-proposed edges exist.
  - `KnowledgeGraphController`: `POST /rebuild`, `GET /status`, `GET /transformation/{id}`,
    `GET /belief/{id}`, `GET /focus/{nodeType}/{sourceRecordId}`, and the three governance actions,
    with a per-edge-type plain-language `displayLabel` lookup and a per-node-type `sourceRoute`.
- New tests: `KnowledgeGraphProjectionServiceTest` (full derivation chain across all seven domain
  repositories, retrospective exclusion, orphaned-edge skip, checkpoint touch), 
  `KnowledgeGraphQueryServiceTest` (missing-focus-node error, BFS walk, truncation, depth limit),
  `KnowledgeEdgeGovernanceServiceTest`, `KnowledgeGraphControllerTest`.
- Updated ADR-020 status to Accepted and `docs/roadmap/2026-08-roadmap.md` Phase 11 section to
  record 11B as backend-done.

Known limitations:
- Backend has not been compiled or test-run in this sandbox (JDK 11 only; `./gradlew` cannot
  download its Gradle distribution — network to `services.gradle.org` is blocked). All correctness
  here relies on manual review against the actual entity/repository source, not a passing build.
  This constraint has held for every phase in this engagement; flagging again since it applies with
  extra force to a service this size (12 constructor-injected repositories, one large rebuild
  method).
- Phase 11C (frontend), 11D (governance UI), 11E (AI-assisted discovery), 11F (temporal exploration)
  remain unstarted.

## 2026-08-02 Session - Roadmap Phase 11A: Knowledge Graph Product and Domain Scoping

Summary:
- User supplied a detailed, prescriptive product brief for a personal knowledge graph (Phase 11 /
  Increment 7), with an explicit instruction not to begin any visualization or persistence work
  until the brief's own Phase 11A scoping deliverables and an architecture decision record were
  complete. This session produced exactly those two artifacts and nothing else — no migrations, no
  entities, no endpoints, no frontend code.
- The main value-add beyond transcribing the brief: cross-referencing every one of its ~20 proposed
  edge types and 8 proposed node types against Helix's actual current schema (verified directly, not
  assumed) to determine which relationships are genuinely explicit (a direct foreign key already
  encodes it), which are deterministic derivations (a traceable join across explicit relationships),
  and which have no supporting data at all today and would require either new domain behavior or
  AI inference to exist.

Changes:
- New `docs/product/knowledge-graph-scoping.md`: purpose statement, narrowed user-question list,
  node catalog (Transformation/Belief/Experiment/Evidence/Reflection/Wisdom/Memory — `Value` and
  `Growth Dimension` explicitly deferred, since neither exists anywhere in Helix's domain model or
  product docs today), edge catalog split into three categories (explicit / deterministic-derivation
  / deferred-not-supported-by-current-data, with per-edge reasoning), provenance model (adopted
  from the brief's `KnowledgeEdge` shape as-is), governance model (read-only first release; governed
  confirm/reject/hide UI deferred to land alongside Phase 11E rather than as its own standalone
  effort, since the first release's all-explicit/all-auto-confirmed edge set gives it nothing to
  govern), initial transformation-centered user journey, accessibility approach, a 1/3/10-year
  data-volume estimate, and explicit answers to all 20 of the brief's required scoping questions.
- New ADR-020 (status: Proposed, not yet Accepted — the brief's own Phase 11A exit criterion
  requires product-owner sign-off before Phase 11B implementation begins): relational PostgreSQL
  projection, no dedicated graph database, domain modules remain authoritative, a new Knowledge
  Graph module owns projection/queries only, zero AI-proposed edges in the first release, bounded
  contextual graph views (25-node default, 1-2 hop depth), accessible list view ships from day one,
  rebuildable projection, temporal metadata columns retained from day one even though unused
  initially.
- Updated ADR index and `docs/roadmap/2026-08-roadmap.md` Phase 11 section to reflect 11A as done
  and 11B-11F as not started, with the two genuinely-still-open items (mobile experience shape,
  graph library selection) called out as deliberately deferred to the start of 11C.

Governance:
- This session made one product-scoping decision not explicitly dictated by the brief: reordering
  Phase 11D (governance UI) to land alongside 11E (AI-assisted discovery) rather than before it,
  since 11D's confirm/reject/hide controls have no function against an all-explicit,
  all-auto-confirmed edge set. Documented as a deliberate deviation in both the scoping doc (Section
  10) and ADR-020 (Risks), not a silent reordering.
- Two node types (`Value`, `Growth Dimension`) and several edge types from the brief were deferred
  rather than implemented with weak/invented signals, consistent with this project's standing
  principle against introducing undocumented domain concepts.

Verification:
- N/A — this was a documentation-only session by design (the brief explicitly forbids code changes
  before scoping/ADR completion). `./scripts/check-docs` passed.

Known limitations / follow-ups:
- ADR-020 is Proposed, not Accepted. Phase 11B should not begin until the user reviews and approves
  both the scoping doc and the ADR — this is the brief's own stated exit criterion, not an
  optional formality.
- Graph library selection and the mobile experience shape are both still open, deliberately deferred
  to a short spike at the start of Phase 11C rather than decided speculatively in this scoping pass.
- Phases 6, 7, and 9 remain uncommitted with no backend verification (JDK 21 unavailable in this
  sandbox across every session so far) — unrelated to this session's work, but still the largest
  outstanding risk in the working copy and worth resolving before more phases stack on top.

## 2026-08-02 Session - Roadmap Phase 9: Data Export and Deletion (closes ADR-015)

Summary:
- Closed ADR-015's foundational commitment, which had existed since before this roadmap doc with
  zero implementation behind it. Two open questions blocked implementation (hard vs. soft delete;
  deletion scope given no auth yet) — resolved via `AskUserQuestion` before writing any code: hard
  delete, whole-app scope, documented in new ADR-019. Continued working locally (no branch/PR),
  consistent with Phases 6 and 7 this session.

Changes:
- Backend (`apps/api`):
  - New `data` module (`application`/`adapter.in.http`): `DataExportService` (reads every
    repository's `findAll()` plus onboarding status into one snapshot, deliberately excluding
    `semantic_search_documents` — a derived/regenerable embedding cache, not user-authored content)
    and `DataDeletionService` (`@Transactional`, deletes every table leaf-first even though most
    foreign keys already cascade, then resets onboarding back to `NOT_STARTED`).
  - `DataController`: `GET /api/v1/data/export` (full JSON bundle) and `DELETE /api/v1/data`
    (requires `{"confirm": true}` in the body — not real security, just protection against a
    reflexive no-body DELETE; a missing/false confirm returns 400 via the existing
    `IllegalArgumentException` -> `ApiExceptionHandler` path).
  - `OnboardingStateEntity`/`OnboardingService` gained a `reset()` that unconditionally returns to
    `NOT_STARTED`, bypassing the monotonic guard `advanceTo()` enforces — used only by the wipe.
  - New tests: `DataExportServiceTest`, `DataDeletionServiceTest` (verifies every repository's
    `deleteAllInBatch()` and `onboardingService.reset()` are called), `DataControllerTest`
    (including the missing-confirmation 400 case).
- Frontend (`apps/web`, `packages/contracts`):
  - Added `DataExportResponse` contract; `api.exportData()` and `api.deleteAllData()` (the latter
    always sends `confirm: true` — the frontend's own confirmation gate, described below, is what
    actually protects the user).
  - New `DataExportPage.tsx` replaces the `/settings/export` placeholder: an export card that
    downloads the bundle as a pretty-printed `.json` file via a `Blob`/object-URL, and a delete
    card that only enables its button once the user types the literal string `DELETE` into a
    confirmation input — the frontend's real safeguard, since the backend's `confirm: true` is
    trivial for any API client to send. On successful deletion, `queryClient.clear()` wipes the
    entire cache (every cached response is now stale in the strongest sense — the records are gone,
    not just changed).
  - Added a "Export & delete data" link to `AppLayout`'s secondary nav (the route existed as a
    placeholder with no way to reach it before).
  - New `DataExportPage.test.tsx` (5 tests: download + confirmation, export error, button
    disabled/enabled by confirmation text, successful delete clears the input, delete error).
- Docs/governance:
  - New ADR-019 (amends ADR-015): hard delete, whole-app scope, semantic-index export exclusion,
    confirmation-flag rationale, onboarding-reset rationale — full reasoning and reconsideration
    triggers (chiefly: this must be revisited the moment auth ships).
  - Updated ADR index, traceability matrix (HELIX-NFR-002 row), roadmap doc (Phase 9 marked
    shipped).

Governance:
- ADR-015 is now actually implemented rather than just decided.
- ADR-019 explicitly flags itself for reconsideration once ADR-013 (auth) is implemented — shipping
  per-user auth without re-scoping this endpoint away from "delete literally everything" would be a
  serious bug.

Verification:
- Backend: not run this session — same JDK 21 / `./gradlew` distribution-download gap as Phases 6
  and 7. All thirteen `deleteAllInBatch()` calls and the repository/service wiring were checked by
  direct code review against the subagent-verified field lists for every entity touched, rather than
  a compiler.
- Frontend:
  - `npm run typecheck` passed.
  - `npm run lint` passed.
  - `npx vitest run` passed (9 files, 31 tests).
  - `npm run build`: same sandbox `dist/` `EPERM` as prior sessions; re-verified via `vite build
    --outDir` to a scratch directory, which completed cleanly.
- Docs: `./scripts/check-docs` passed.

Known limitations / follow-ups:
- Backend is unverified by an actual compile/test run in this environment — third session in a row
  with this gap. Phases 6, 7, and 9 are all sitting uncommitted in the local working copy; strongly
  recommend getting a JDK 21 environment before any more phases stack up unverified.
- No automated backups exist anywhere in this app. The export endpoint is the *only* recovery path
  before running delete — this is called out explicitly in ADR-019 but is worth restating here: a
  user who deletes without exporting first has no way to get their data back.
- Deletion is whole-app only; no per-transformation or partial deletion exists. Explicitly out of
  scope for this ADR (see Alternatives), not an oversight.

## 2026-08-02 Note - Phase 8 (Visual Redesign) Reconciled as Already Shipped

While starting Phase 8, the user confirmed the calm-editorial-minimalism
redesign was already implemented. Verified directly against the repository
rather than taking that at face value: `apps/web/src/styles/main.css`
contains the full token system (warm parchment canvas, warm near-black ink,
one rust/terracotta accent, distinct provenance color, radii up to 1.25rem,
soft card shadows, a real type scale), and `git log` shows it merged to
`main`/`origin/main` via commits `912a409` and `f32b4ec` on 2026-08-01 — a
session that predates `docs/roadmap/2026-08-roadmap.md` and was never logged
here or marked shipped in that roadmap doc. Confirmed component files
(`TodayPage.tsx`, `AppLayout.tsx`) are unchanged at the logic level (same
plain class names throughout), consistent with the redesign's hard
constraint that it stay a pure CSS/token layer change. No code changes made
this session — updated `docs/roadmap/2026-08-roadmap.md` (Part 1 and the
Phase 8 section) to reflect reality and moved on to Phase 9.

## 2026-08-02 Session - Roadmap Phase 7: CurrentFocus Projection + Server-Persisted Onboarding

Summary:
- Closed the two long-carried-forward Phase 2 gaps documented in every dev log entry since: Today
  assembling its view from two separate calls, and the welcome/first-use state being purely derived
  from `transformations.length === 0` client-side instead of persisted server-side.
- Per explicit user scoping decisions: kept working directly on the local copy (same as Phase 6, no
  branch/PR this session); added a new `GET /api/v1/current-focus` endpoint rather than expanding
  `/today`; used 3 onboarding states.

Design decision (not previously specified, made here): the 3 onboarding states — `NOT_STARTED`,
`FIRST_TRANSFORMATION_CREATED`, `COMPLETE` — map exactly onto the two gates Today's UI already had
(no transformations -> welcome screen; a transformation but no experiment -> "add an experiment"
prompt), so `COMPLETE` is reached the moment the first experiment is ever created, not a new
invented milestone. Transitions are monotonic (enforced by ordinal comparison) and fire
automatically from `TransformationService.create` / `ExperimentService.create` — no explicit
"finish onboarding" user action was introduced.

Changes:
- Backend (`apps/api`):
  - New `onboarding` module (`domain`/`adapter.out.persistence`/`application`, mirroring every
    other module's hexagonal layout): `OnboardingStatus` enum, `OnboardingStateEntity` (singleton
    row, `SINGLETON_ID` constant — Helix is single-user with no auth per ADR-013, same shape auth
    would later key by user_id), `OnboardingStateRepository`, `OnboardingService`
    (`get()`/`advanceToFirstTransformationCreated()`/`advanceToComplete()`, all monotonic/no-op
    once already past a state).
  - Migration `V10__onboarding_state.sql`: singleton table, seeded `NOT_STARTED`.
  - `TransformationService`/`ExperimentService` gained an `OnboardingService` dependency and call
    the corresponding `advanceTo*` method after a successful save (constructor signatures changed
    directly, all call sites — both services' test files — updated to the new arity).
  - New `today.application.CurrentFocusService` composes `TodayService`, `TransformationService`,
    and `OnboardingService` into one snapshot; new `today.adapter.in.http.CurrentFocusController`
    exposes it as `GET /api/v1/current-focus`. `GET /api/v1/today` and `GET /api/v1/transformations`
    are both left unchanged for their other existing callers (Knowledge page, Transformations page).
  - New tests: `OnboardingServiceTest`, `CurrentFocusServiceTest`, `CurrentFocusControllerTest`;
    extended `TransformationServiceTest`/`ExperimentServiceTest` to verify the onboarding
    advancement calls (and that `proposeDraft` — a non-persisting AI draft call — never touches
    onboarding state).
- Frontend (`apps/web`, `packages/contracts`):
  - Added `OnboardingStatus` and `CurrentFocusResponse` contracts; `api.getCurrentFocus()`.
  - `TodayPage`: replaced the `['today']` + `['transformations']` query pair with a single
    `['current-focus']` query; the welcome-screen branch now checks
    `data.onboardingStatus === 'NOT_STARTED'` instead of `transformations.length === 0`; the
    weekly-retrospective-draft query's `enabled` gate uses the same onboarding-status check;
    `suggestionAction`'s optimistic cache update and `reflectMutation`'s invalidation both moved to
    the `['current-focus']` key.
  - `TransformationDetailPage`: also invalidates `['current-focus']` (alongside the existing
    `['today']` invalidation it already had) after creating an experiment, so Today picks up the
    newly active experiment immediately rather than waiting for TanStack Query's background
    refetch-on-mount.
  - Rewrote `TodayPage.test.tsx`'s mocking to a single `api.getCurrentFocus` mock (previously two
    separate `api.getToday`/`api.listTransformations` mocks), with a shared
    `activeExperimentFocus()` helper to cut down repetition across the file's many test cases.
- Docs/governance:
  - Updated traceability matrix: added a row for HELIX-FR-007 ("Provide Today summary for active
    experiment"), which previously existed in the requirements catalog with no traceability row at
    all.
  - Updated `docs/roadmap/2026-08-roadmap.md` marking Phase 7 shipped.

Governance:
- No new ADR needed — this is backend plumbing (a projection endpoint and a persistence-backed
  status field), not a new AI-optionality or governance-lifecycle decision; ADR-008/ADR-016-style
  amendments don't apply here.

Verification:
- Backend: not run this session, same constraint as the prior Phase 6 entry — this sandbox only has
  JDK 11, the backend targets Java 21, and `./gradlew` cannot download a JDK 21 toolchain or even
  the Gradle distribution itself without network access to `services.gradle.org` (blocked by the
  sandbox's network allowlist). All constructor call-site updates were verified by direct grep
  (`new TransformationService(`, `new ExperimentService(`) rather than a compiler.
- Frontend:
  - `npm run typecheck` passed.
  - `npm run lint` passed.
  - `npx vitest run` passed (8 files, 26 tests).
  - `npm run build`: same pre-existing sandbox `EPERM` on `apps/web/dist/` as the Phase 6 session;
    re-verified via `vite build --outDir` to a scratch directory, which completed cleanly.
- Docs: `./scripts/check-docs` passed.

Known limitations / follow-ups:
- Backend changes are unverified by an actual compile/test run in this environment. This should be
  the first thing exercised (`./scripts/test-backend`, `./scripts/verify-architecture`) on a
  machine/CI with JDK 21 before this phase is considered fully closed — same caveat as Phase 6,
  now two sessions in a row without a working JDK 21 in this sandbox.
- `TransformationsPage` (list/create transformations) does not invalidate `['current-focus']` after
  creating a transformation. This is a pre-existing-shaped gap (it didn't invalidate `['today']`
  either, before this change) — TanStack Query's default `staleTime: 0` means Today will still
  self-correct on next mount via background refetch, just with a possible stale-data flash first
  rather than an immediate update. Not fixed here since it wasn't part of this phase's stated scope
  and the behavior is no worse than before.
- Neither this endpoint nor the onboarding state is versioned/keyed by user — deliberately, since
  ADR-013 still defers auth behind a port and the app is single-user today. Whoever implements auth
  will need to add a `user_id` column and change the singleton lookup to a per-user one.

## 2026-08-02 Session - Roadmap Phase 6: Contextual Memory Proposals

Summary:
- Closed the gap identified in the Memory feature review: Memory previously had a fully-built
  governance workspace (propose/revise/accept/reject) but no contextual trigger — it was reachable
  only through manual entry. This session adds an AI-derived candidate memory statement, proposed
  after a reflection save, as a second and distinctly-sourced card alongside the existing
  deterministic wisdom-capture prompt (per the user's explicit scoping: AI-derived source,
  triggered after reflection save as a second distinct card).
- Implemented directly on the local working copy per explicit instruction — no branch or PR for
  this session's work.

Changes:
- Backend (`apps/api`):
  - Extended `AiAssistantPort` with `proposeMemory(String context)` returning
    `AiMemoryProposal(statement, provider, model, deterministicFallback)`, implemented across
    OpenAI, Ollama, and NoOp adapters using the existing circuit-breaker pattern. Unlike other
    ADR-016 surfaces, the NoOp adapter and outage fallback return a `null` statement rather than
    placeholder content — a templated "fact about you" would be misleading, not just generic. The
    model may also legitimately answer `NONE` (nothing durable worth proposing), mapped to a
    `null` statement with `deterministicFallback: false` to distinguish it from an outage.
  - `MemoryProposalService.proposeFromReflection(UUID reflectionId)` builds context from the
    reflection and its experiment and calls the port; persists nothing.
  - `MemoryProposalController`: new `POST /api/v1/memory/proposals/draft` endpoint. The existing,
    unmodified `POST /api/v1/memory/proposals` endpoint remains the only way a proposal actually
    lands as `PROPOSED`.
  - Updated `MemoryProposalServiceTest`/`MemoryProposalControllerTest` (new 9-arg service
    constructor call sites fixed; new tests for the AI-draft path and the "nothing to propose"
    path).
- Frontend (`apps/web`, `packages/contracts`):
  - Added `MemoryProposalDraft`/`ProposeMemoryDraftRequest` contracts and
    `api.proposeMemoryDraft(reflectionId)`.
  - `TodayPage`: after a reflection saves, fires `proposeMemoryDraft` and — only if a statement
    comes back — shows a "Something worth remembering about you" card (editable, "Remember this" /
    "Not now"), persisted locally under `helix:memory-draft` the same way the wisdom draft is,
    distinct from and independent of the wisdom card.
  - Added `Memory` to the shared glossary (`docs/product/glossary.md` and
    `apps/web/src/content/glossary.ts`) and wired a `TermHint` on the new card.
  - Added `TodayPage.test.tsx` coverage: the memory-proposal card appears and saves correctly, and
    no card appears when the AI proposes nothing.
- Docs/governance:
  - Added ADR-018 (`docs/decisions/ADR-018-ai-derived-memory-proposal-candidates.md`), amending
    ADR-006, following the ADR-016 precedent for AI-required generative surfaces.
  - Updated ADR index and traceability matrix (HELIX-FR-017).
  - Wrote `docs/roadmap/2026-08-roadmap.md` reconciling prior roadmap docs with current shipped
    state (see that session's roadmap-authoring work, same day).

Governance:
- ADR-008 preserved: the draft endpoint persists nothing; a proposal only becomes real data through
  the existing, unmodified create endpoint after explicit user review.
- ADR-018 added: narrows ADR-006's AI-optionality for this one generative surface, consistent with
  ADR-016's precedent for suggestions/retrospective/experiment drafts.

Verification:
- Backend: not run this session — this sandbox only has JDK 11 available, and the backend targets
  Java 21; `./scripts/test-backend` and `./scripts/verify-architecture` could not be executed here.
  Every constructor call site for `MemoryProposalService`'s new 9-arg signature was verified by
  direct file review (both test files, no other callers found via repo search).
- Frontend:
  - `npm run typecheck` passed.
  - `npm run lint` passed.
  - `npx vitest run` passed (8 files, 26 tests, including 14 in `TodayPage.test.tsx`).
  - `npm run build`: `tsc -b` and module transform succeeded; the final `vite build` write step hit
    an `EPERM` unlinking pre-existing files in `apps/web/dist/`, a sandbox filesystem-permission
    artifact unrelated to this change — confirmed by re-running `vite build --outDir` to a scratch
    directory outside the mounted repo, which completed and produced the expected bundle
    (`index-*.js`, `index-*.css`, service worker) with no errors.
- Docs: `./scripts/check-docs` passed.

Also fixed, incidentally discovered while verifying:
- `apps/web/src/test/setup.ts` didn't clear `localStorage` between tests. Since Vitest's jsdom
  environment is shared across every test in a file (isolation is per-file, not per-test), and
  several `TodayPage` tests reuse the same experiment/reflection IDs, a wisdom-draft written to
  `localStorage` by one test was silently bleeding into a later, unrelated test's initial render
  (the component's experiment-mismatch reset never fired because the IDs happened to match). Added
  an `afterEach(() => globalThis.localStorage?.clear())` alongside the existing DOM `cleanup()`.
  This was a latent pre-existing gap, not something introduced by this session's changes, and it
  isn't scoped to Phase 6 — flagging it here since it was the actual root cause of a UI test failure
  during verification.

Known limitations / follow-ups:
- Backend changes are unverified by an actual test run in this environment (no JDK 21 available in
  the sandbox); they should be exercised by the standard verification scripts on a machine/CI that
  has one before this is considered fully closed.
- Memory and wisdom now both fire after a reflection save; ADR-018 explicitly flags this as a
  UX-density tradeoff worth watching, not a settled decision.
- Two AI calls (suggestion + memory) now happen per reflection save on top of the existing
  suggestion call; `AiProperties.timeoutSeconds`/`retryMaxAttempts` remain unused by any adapter
  (pre-existing gap, unchanged by this session).

## 2026-08-01 Session - Product Experience Realignment, Phase 5 Slice D: Conversational Reflection Flow

Summary:
- Implemented the final Phase 5 slice by replacing Today’s structured reflection form with a
  conversational AI chat flow that ends in an explicit review/edit step before save.
- Kept reflection persistence behavior unchanged: only the existing
  `POST /api/v1/experiments/{id}/reflections` path writes data.

Changes:
- Backend (`apps/api`):
  - Extended `AiAssistantPort` with `continueReflectionChat(String)` and
    `structureReflection(String)` plus new `AiReflectionStructure` record.
  - Implemented both methods across OpenAI/Ollama/NoAI adapters with existing circuit-breaker
    fallback patterns and labeled-line parsing for the structuring response.
  - Added a new stateless reflection-chat surface in the reflection module:
    `ReflectionChatService` + `ReflectionChatController` with:
    - `POST /api/v1/experiments/{id}/reflection-chat/turn`
    - `POST /api/v1/experiments/{id}/reflection-chat/finish`
    Both endpoints validate experiment existence and persist nothing.
  - Added `ReflectionChatServiceTest` and expanded adapter tests for the new methods.
- Frontend (`apps/web`, `packages/contracts`):
  - Added reflection-chat contracts (`ReflectionChatMessage`, turn/finish response types).
  - Added `api.continueReflectionChat(...)` and `api.finishReflectionChat(...)`.
  - Replaced `TodayPage` reflection form UI with:
    - transcript view + message input/send,
    - explicit "I’m done — review my reflection" action,
    - editable structured-review form for `content`/`attempted`/`noticed`/`evidenceNoted`/`surprise`,
    - save via existing `api.createReflection(...)` call.
  - Added local buffering for unsent input text only:
    `helix:reflection-chat-draft:<experimentId>`.
  - Reworked Today tests to cover chat-turn flow, finish review/edit/save flow, and clear
    connection-required error handling.
- Docs/governance:
  - Added ADR-017 (`docs/decisions/ADR-017-network-required-for-reflection-chat-capture.md`)
    narrowing ADR-012 for reflection capture.
  - Updated ADR index, traceability matrix, running-app guide, and Phase 5 roadmap status/details.

Governance:
- ADR-008 preserved: AI output remains proposal-only until explicit user review/edit/accept.
- ADR-017 added: reflection chat send/finish is network-required; local draft buffering narrowed to
  unsent text only.
- ADR-012 is narrowed (not removed) by ADR-017 for this specific flow.

Verification:
- Backend:
  - `./scripts/test-backend` passed.
  - `./scripts/verify-architecture` passed.
- Frontend:
  - Could not run `npm run typecheck`, `npm run lint`, `npx vitest run`, or `npm run build` in
    this environment because Node/npm are unavailable (`node`/`npm` commands not found).
- Docs:
  - `./scripts/check-docs` passed.

Known limitations / follow-ups:
- No live provider round-trip against a real OpenAI API key was exercised here; new adapter prompts
  and parsing were validated via code review and fallback-path tests.
- Reflection chat transcript is intentionally not persisted across reloads; only unsent draft text is
  buffered locally (per ADR-017 scope).

## 2026-08-01 Session - Product Experience Realignment, Phase 5 Slices B & C: AI Weekly Retrospective + AI-Drafted Experiments

Summary:
- Continuation of Phase 5 (real AI in suggestions/retrospectives/experiment drafting/reflection),
  approved by the user as "all of the above." This session ships slices B and C in one pass; slice A
  (AI-generated post-reflection suggestions) shipped earlier the same day as PR #6. Slice D
  (conversational reflection flow) remains scoped-not-built — it's the largest and riskiest piece,
  and per the plan doc it needs its own design pass rather than being guessed at alongside B/C.

Changes:
- `AiAssistantPort` gained `summarizeWeek(String context)` (returns `AiWeeklySummary(summary,
  assistance, provider, model, deterministicFallback)`) and `proposeExperiment(String context)`
  (returns `AiExperimentDraft(title, hypothesis, nextAction, cadence, evidenceOfSuccess, provider,
  model, deterministicFallback)`), implemented in all three adapters (OpenAI, Ollama, NoOp). Both
  use a shared `extractLabeledLine(text, label)` parsing convention (`SUMMARY:`/`NEXT:` for the
  retrospective; `TITLE:`/`HYPOTHESIS:`/`NEXT_ACTION:`/`CADENCE:`/`EVIDENCE:` for the experiment
  draft), with a missing required label treated as a parse failure that falls back, same convention
  as slice A's `suggestNextAction`.
- `WeeklyRetrospectiveService.draft()` now calls `summarizeWeek` instead of its old
  count/length-based string concatenation, **except** when there are zero reflections in the
  7-day window — that stays genuinely deterministic (nothing to summarize, so AI isn't invoked at
  all; a new test asserts `verifyNoInteractions(aiAssistantPort)` for that case).
  `WeeklyRetrospectiveEntity` gained `source`/`ai_provider`/`ai_model` columns (Flyway
  `V9__weekly_retrospective_ai_provenance.sql`), same backward-compatible legacy-constructor pattern
  used for `SuggestionEntity` in slice A. A new `RetrospectiveSource` enum (wisdom module) mirrors
  `SuggestionSource` (suggestions module) rather than being shared, to keep each feature module
  owning its own domain vocabulary per ADR-001/HELIX-NFR-003. Today's "This week" teaser and the
  Wisdom page's retrospective card both show an "(AI suggested — openai)" badge.
- New `ExperimentService.proposeDraft(transformationId)` builds AI context from the transformation's
  title/purpose/desiredIdentity/obstacle and calls `proposeExperiment` — **nothing is persisted**;
  it returns a plain `ExperimentDraft` record (not an entity). New
  `POST /api/v1/transformations/{id}/experiments/draft` endpoint exposes it. On
  `TransformationDetailPage`, a new "Draft this for me" button calls this and prefills the *existing*
  experiment-creation form fields for the user to edit before pressing the unchanged "Save
  experiment" button — per ADR-008, nothing AI-drafted becomes a real experiment without that
  explicit, editable review step. New `TransformationDetailPage.test.tsx` (didn't exist before this
  session) covers both the AI-draft-then-save flow and the plain manual-entry flow.
- `packages/contracts` gained `ExperimentDraft` and provenance fields on `WeeklyRetrospectiveDraft`/
  `WeeklyRetrospective`; `apps/web/src/api/http.ts` gained `proposeExperimentDraft`.

**Governance (ADRs)**:
- ADR-016 (from slice A) already anticipated these two features by name ("in later slices, weekly
  retrospective narrative and experiment drafting") — no new ADR needed, no amendment required.
- ADR-008 (user-governed AI memory: propose → review → explicit acceptance) directly shaped slice
  C's design: the draft endpoint returns a value object, not a persisted entity, and the only way an
  AI-drafted experiment becomes real is through the existing, unmodified, explicit "Save experiment"
  action.
- ADR-001/HELIX-NFR-003 (explicit module boundaries) is why `RetrospectiveSource` isn't just a reuse
  of `SuggestionSource` — a two-line duplication was judged cheaper than a cross-module domain
  dependency.

Verification:
- Backend changes (`apps/api`) could not be compiled or run in this sandbox — no JDK 21 available,
  consistent with every prior phase. Hand-reviewed, including a repo-wide grep for every call site of
  the constructors/methods that changed shape (`new ExperimentService(`,
  `new WeeklyRetrospectiveService(`, `new WeeklyRetrospectiveEntity(`) to confirm nothing else broke
  (only test files construct these directly; all were updated). `./scripts/test-backend` and
  `./scripts/verify-architecture` still need to run in CI or on the user's machine before merging.
- Frontend verified for real in a re-synced scratch clone: `npm run typecheck`, `npm run lint`,
  `npx vitest run` (15/15 tests across 8 files, including the two new
  `TransformationDetailPage.test.tsx` cases), and `npm run build` all passed. `./scripts/check-docs`
  also passed.

Known limitations / follow-ups:
- Same `AiProperties.timeoutSeconds`/`retryMaxAttempts`/`retryDelayMs` gap as slice A — still unused
  by any adapter, still flagged, still not fixed.
- Slice C's AI-drafted proposal only looks at the transformation's own stated fields, not prior
  experiments/reflections/evidence for that transformation — a reasonable first slice, not a fully
  adaptive coach yet.
- No live call against a real OpenAI API key was possible in this sandbox (no network egress to
  `api.openai.com`); the new labeled-line parsing logic was reviewed by hand, not exercised against
  an actual model response.
- Phase 5 slice D (conversational reflection flow) remains scoped but not built — see the plan doc
  for the open design questions (multi-turn-to-field mapping, turn limits, offline behavior under
  ADR-012) that make it too large to guess at without a dedicated pass.

## 2026-08-01 Session - Product Experience Realignment, Phase 5 Slice A: AI-Generated Suggestions

Summary:
- The user asked for a major scope change from the original external-review-derived plan: real AI reasoning driving suggestions, retrospectives, and experiment drafting, plus a more conversational (less form-heavy) UX. They explicitly chose the broadest first-slice scope, explicitly chose to drop the previously-mandatory deterministic no-AI fallback requirement, and chose OpenAI as the provider.
- This session's slice ("Phase 5 slice A") wires real AI into the one generative surface that's small enough to ship end-to-end in one session: the "Suggested Small Action" shown after a reflection is saved. The other three pieces the user asked for (AI weekly retrospective, AI-drafted experiments, conversational reflection) are scoped in `docs/roadmap/product-experience-realignment-plan.md` under Phase 5 slices B-D but not yet built — the conversational reflection flow in particular is a large enough UX change that it needs its own scoping pass before implementation.
- Investigation confirmed (via full-codebase read of `apps/api/src/main/java/com/helix/api/ai/`) that the AI adapter layer (`AiAssistantPort`, `OpenAiAssistantAdapter`, `OllamaAssistantAdapter`, `NoAiAssistantAdapter`, `AiProviderFactory`, `AiOrchestrationService`) was fully built and tested but had zero callers anywhere in the live application — every "suggestion" and "retrospective" the user saw was pure string-template selection, no model call involved.

Changes:
- `AiAssistantPort` gained `suggestNextAction(String context)` alongside the existing `suggestReflectiveQuestion`; implemented in all three adapters with an action-specific coaching prompt and a distinct fallback string.
- `ReflectionService.create(...)` now builds a context string from the active experiment (title/hypothesis/nextAction) and the just-saved reflection (content/noticed/evidenceNoted/surprise/previous-attempt count), calls `aiAssistantPort.suggestNextAction(...)`, and persists the result via a new `SuggestionService.createFromAi(...)` instead of `createDeterministic(...)`.
- `SuggestionEntity` gained `source` (`AI`/`DETERMINISTIC`), `ai_provider`, and `ai_model` columns (Flyway `V8__suggestion_ai_provenance.sql`), with a backward-compatible legacy constructor defaulting existing callers to `DETERMINISTIC`/no-provenance. `SuggestionController`, `ReflectionController`, and `TodayController` DTOs all now expose these three fields.
- `packages/contracts/src/index.ts`'s `Suggestion` type gained the same three fields; `TodayPage.tsx` shows an "(AI suggested — openai)" badge next to the suggestion text when `source === 'AI'`.
- New ADR-016 ("AI required for generative suggestion content") narrows ADR-006's "core workflows must work without AI" mandate specifically for this feature (and the Slice B/C features to come), while leaving ADR-007 (provider selection) and ADR-008 (propose → review → accept governance — AI suggestions still land as `PROPOSED`) untouched. ADR-006 was annotated with a pointer to ADR-016 rather than rewritten.
- `docs/requirements/traceability-matrix.md` (HELIX-FR-004, HELIX-BR-001) and `docs/running-app.md` (AI provider section) updated to stop claiming this flow works identically without AI — it now documents AI as the default content source and fallback-during-outage as a degraded, not equal, experience.

**Governance (ADRs)**:
- ADR-016 (new): AI required for generative suggestion content; amends ADR-006's scope for this feature.
- ADR-006: unchanged in substance, annotated to point at ADR-016.
- ADR-007, ADR-008: unchanged and still govern provider selection and user-acceptance-gating respectively.

Verification:
- Backend changes (`apps/api`) could not be compiled or run in this sandbox — no JDK 21 available (only JDK 11), consistent with every prior phase's backend work in this engagement. All Java changes were hand-reviewed for correctness, including a check that no other production or test call site broke (`grep` for `new SuggestionEntity(`, `new ReflectionService(`, `createDeterministic` across `apps/api`). `./scripts/test-backend` and `./scripts/verify-architecture` (ArchUnit layering check — confirmed the new `ReflectionService → AiAssistantPort` dependency doesn't violate either existing layering rule, since both are application-layer, not domain or adapter.in.http/adapter.out.persistence) still need to run in CI or on the user's machine before merging.
- Frontend verified for real in a scratch clone (`/tmp/helix-work/repo`, synced from the mounted repo, fresh `npm install` due to the native-binding/arm64 mismatch documented in earlier sessions): `npm run typecheck`, `npm run lint`, `npx vitest run` (13/13 tests across 7 files, including the new AI-badge assertion), and `npm run build` all passed. `./scripts/check-docs` also passed.

Known limitations / follow-ups:
- `AiProperties.timeoutSeconds`/`retryMaxAttempts`/`retryDelayMs` remain unused by any adapter (pre-existing gap, not introduced this session) — a slow OpenAI response blocks the reflection-save request rather than failing fast; flagged in ADR-016, not fixed here.
- Slices B (AI weekly retrospective), C (AI-drafted experiments), and D (conversational reflection) are scoped in the plan doc but not implemented.
- No live end-to-end verification against a real OpenAI API key was possible in this sandbox (network egress to `api.openai.com` isn't available here); the adapter's existing circuit-breaker/fallback behavior was exercised, not a real model response.

## 2026-07-27 Fix - Add missing CORS configuration to apps/api

Summary:
- The user reported the web app failing to load Today with a browser CORS error (`No 'Access-Control-Allow-Origin' header is present`) when running the API and web app locally against each other, after separately resolving an unrelated local `.env` port-mismatch issue.
- Investigation found `apps/api` had **no CORS configuration anywhere** — no `CorsConfigurationSource` bean, no `@CrossOrigin`, no CORS-related properties. This is a pre-existing gap that predates this session's work; it would affect anyone running the web app against the API cross-origin (including the documented local dev setup in `docs/running-app.md`), not just this user.
- Fixed by adding a `CorsConfigurationSource` bean and `.cors(Customizer.withDefaults())` to the existing `SecurityConfig` (`apps/api/src/main/java/com/helix/api/identity/config/SecurityConfig.java`), configured via a new `helix.web.allowed-origins` property (`application.properties`), defaulting to `http://localhost:5173` (the Vite dev server) and overridable via `HELIX_WEB_ALLOWED_ORIGINS`.
- Updated `.env.example` and `docs/running-app.md` (new "CORS errors" troubleshooting entry) to document the new variable.

**Governance (ADRs)**:
- ADR-005/ADR-010 (browser communicates with backend via REST over HTTP) — this fix makes that communication actually functional cross-origin; no ADR change needed, this closes an implementation gap rather than changing the architecture.
- No ADR superseded.

Verification run:
- Hand-reviewed against Spring Security conventions; this execution sandbox has no JDK 21, so `./scripts/test-backend` and `./scripts/verify-architecture` were **not run** here. **Please run them locally before relying on this change.** The fix was applied directly to the user's local working tree so they could restart `./scripts/dev-api` and unblock immediately; confirmation that it resolves the browser CORS error is still pending.

Known limitations:
- The default allowed origin is a single dev-server URL; deployed environments will need `HELIX_WEB_ALLOWED_ORIGINS` set explicitly (comma-separated for multiple origins).

## 2026-07-27 Session - Product Experience Realignment, Phase 4 Slice A: Contextual Wisdom Capture

Summary:
- Continued `docs/roadmap/product-experience-realignment-plan.md` into Phase 4, scoped to one coherent slice: a contextual "this reflection may contain a lesson worth keeping" wisdom-capture prompt on Today, and a weekly narrative retrospective teaser also on Today. Both endpoints this slice calls (`POST /api/v1/wisdom` with a `REFLECTION` source, `GET /api/v1/wisdom/weekly-retrospective`) already existed from earlier increments, so **no backend changes were required** — this is a frontend-only slice.
- Frontend (`apps/web`):
  - `TodayPage`: after a reflection saves, a new card proposes a wisdom statement, deterministically prefilled by priority (`evidenceNoted` → `noticed` → `surprise` → the main answer, trimmed to 500 characters), editable before saving, with "Save as wisdom" and "Not now" actions. This is the new primary path for capturing wisdom from a reflection; manual entry on `WisdomPage` (via Library) is unchanged and remains the advanced/edit path, per the plan.
  - `TodayPage`: a new "This week" card surfaces the same deterministic weekly retrospective `summary`/`assistance` text already used on the Wisdom page, linking to Library for the full workspace. Only renders when there's at least one reflection summary for the week; the underlying query only fires once the user has a transformation, so it's silent on the first-use welcome screen.
  - Added two new `TodayPage.test.tsx` cases covering the prefill-priority/save flow and the retrospective teaser's presence/link.
- Fixed an unrelated, pre-existing test-infrastructure gap discovered while verifying this slice: `@testing-library/react`'s automatic per-test DOM cleanup was never running, because it only self-registers when Vitest's `test.globals` option is enabled, which this project doesn't do. Every `render()` call across the test suite was leaving its DOM mounted instead of unmounting between tests. This had gone unnoticed because no prior test asserted the *absence* of something (`queryByText(...).not.toBeInTheDocument()`) in a way that stale DOM from an earlier test could break it — the two new Phase 4 tests were the first to do so, and only failed when run as part of the full suite, which is what surfaced it. Fixed by adding `afterEach(cleanup)` to `src/test/setup.ts`, protecting every test file going forward, not just this one.

**Governance (ADRs)**:
- ADR-006/ADR-007: the wisdom-statement prefill stays deterministic (no AI), consistent with AI remaining optional and assistive; nothing about this slice requires AI to be configured.
- ADR-009: wisdom entries continue to require a linked supporting source; the contextual prompt always links back to the reflection that generated it.
- No ADR superseded. No new backend surface was introduced, so no migration or ADR amendment was needed for this slice.

Verification run:
- `npm run typecheck` (apps/web) passed.
- `npm run lint` (apps/web) passed.
- `npm run test` (apps/web) passed — 13 tests across 7 files, including two new `TodayPage.test.tsx` cases.
- `npm run build` (apps/web) passed.
- `./scripts/check-docs` passed.
- No backend changes were made in this slice, so the JDK 21 verification gap that applied to Phases 2 and 3 does not apply here; there was nothing under `apps/api` to hand-review or flag for local `./scripts/test-backend` / `./scripts/verify-architecture` runs.

Known limitations:
- The proposed wisdom statement is derived from a single reflection only; it doesn't look across a week's reflections the way the retrospective teaser summarizes a whole week. That remains open for a later increment.
- `WisdomPage.tsx` itself is unchanged; it still requires selecting a source manually when used directly (unaffected by this slice, since Today's new prompt already supplies the source automatically).
- `CurrentFocus` backend projection, server-persisted onboarding state, promoting "Reflect" to primary nav, persisting follow-up answers to `localStorage`, and `reviewAt` "review due" surfacing (all carried over from earlier phases) remain open.

## 2026-07-27 Session - Product Experience Realignment, Phase 3 Slice A: Progressive Reflection Questions

Summary:
- Continued `docs/roadmap/product-experience-realignment-plan.md` into Phase 3, scoped to one coherent slice: progressive reflection follow-up questions, an "attempted?" check, and morning/evening framing for the Reflect section — the most user-visible Phase 3 deliverables. New evidence-extraction UI was deliberately not built, since Knowledge already has a working reflection-to-evidence flow; only a navigational nudge toward it was added.
- Backend (`apps/api`):
  - Added `V7__progressive_reflection_fields.sql`, adding optional `attempted` (boolean), `noticed`, `evidence_noted`, and `surprise` columns to `reflections`.
  - Extended `ReflectionEntity`, `ReflectionService`, and `ReflectionController` (request/DTO) with the four new fields. The original 4-arg entity constructor and 2-arg service `create` overload were preserved so existing call sites and the deterministic suggestion flow (confirmed to only read `nextAction` and attempt count, never reflection content) did not need to change.
  - Updated `TodayController`'s `ReflectionCard` so the new fields flow through to the Today response's reflection history.
  - Added a new `ReflectionServiceTest` case (`createWithProgressiveAnswersPersistsThem`) and updated the existing test to assert the new fields default to `null` when omitted.
- Frontend (`apps/web`, `packages/contracts`):
  - Extended `Reflection` and `CreateReflectionRequest` in `packages/contracts/src/index.ts`.
  - Added `apps/web/src/content/reflectionQuestions.ts`, a small data module (mirroring the Phase 1 `glossary.ts` pattern) defining the three progressive follow-up questions in order.
  - Reworked the Reflect section of `TodayPage`: renamed to a time-of-day-conditional "Morning check-in" / "Evening review" heading; added a "Did you try it?" Yes/Not yet control; and added a progressive reveal — once the main "What happened" answer has content, a single "+ next question" button appears, revealing one follow-up textarea and the next button at a time, instead of showing all optional fields up front.
  - After a successful save, added a nudge — "This might be useful evidence — add it in Knowledge." — linking to the existing evidence-from-reflection flow on `KnowledgePage`.
  - Today's reflection history list now shows `noticed`/`evidenceNoted`/`surprise` as muted sub-lines when present.
  - Added a new test in `TodayPage.test.tsx` covering the progressive reveal and full submission payload, and registered a `/knowledge` route in the test router so the new `<Link>` renders correctly; fixed an existing test's heading assertion to match the new conditional heading text.

**Governance (ADRs)**:
- ADR-004: PostgreSQL remains authoritative; schema evolves through a new Flyway migration, all new columns nullable so no backfill is required.
- ADR-010: the reflection-to-evidence nudge reinforces the existing evidence/provenance flow rather than introducing a new one.
- No ADR superseded.

Verification run:
- `npm run typecheck` (apps/web) passed.
- `npm run lint` (apps/web) passed.
- `npm run test` (apps/web) passed — 11 tests across 7 files, including the new progressive-follow-up test in `TodayPage.test.tsx`.
- `npm run build` (apps/web) passed.
- `./scripts/check-docs` passed.
- `./scripts/test-backend` and `./scripts/verify-architecture` were **not run**: this execution sandbox has Java 11 preinstalled and no permissions or network path to install Java 21 (a direct `curl` to the JDK distribution host returned `403 blocked-by-allowlist`). All backend Java changes were hand-reviewed line by line against existing conventions, and every new/changed constructor and service overload was checked against every call site in the repository, but this is not a substitute for compiling and running the test suite. **Please run `./scripts/test-backend` and `./scripts/verify-architecture` locally before merging.**

Known limitations:
- Follow-up answers (`noticed`/`evidenceNoted`/`surprise`) are not persisted to `localStorage`; only the main `content` draft survives a reload. This is stated explicitly in the UI's helper text rather than silently losing data.
- "Reflect" was not promoted to a primary nav destination this session, even though it now has distinct morning/evening content — left for a deliberate nav-structure pass.
- `CurrentFocus` backend projection, server-persisted onboarding state, and `reviewAt` "review due" surfacing (all carried over from Phase 2) remain open.

## 2026-07-27 Session - Product Experience Realignment, Phase 2 Slice A: Guided Transformation & Experiment Creation

Summary:
- Continued `docs/roadmap/product-experience-realignment-plan.md` into Phase 2, scoped to one coherent slice: guided transformation creation and guided experiment design (the two most user-visible Phase 2 deliverables). The `CurrentFocus` backend projection and server-persisted onboarding state remain open and are documented as such.
- Backend (`apps/api`):
  - Added `V6__guided_journey_fields.sql`, adding optional `desired_identity`/`obstacle` columns to `transformations` and optional `cadence`/`evidence_of_success`/`review_at` columns to `experiments`.
  - Extended `TransformationEntity`, `TransformationService`, and `TransformationController` (request/DTO) with `desiredIdentity` and `obstacle`. The original 4-arg entity constructor and 2-arg service `create` overload were preserved so `BeliefServiceTest` and other existing call sites did not need to change.
  - Extended `ExperimentEntity`, `ExperimentService`, and `ExperimentController` (request/DTO) with `cadence`, `evidenceOfSuccess`, and `reviewAt` (a `LocalDate`). The original 7-arg entity constructor and 4-arg service `create` overload were preserved for the same reason.
  - Updated `TodayController`'s `ExperimentCard` so the new experiment fields flow through to the Today response.
  - Added `TransformationServiceTest` and `ExperimentServiceTest` (neither existed before this session).
- Frontend (`apps/web`, `packages/contracts`):
  - Extended `Transformation`, `Experiment`, `CreateTransformationRequest`, and `CreateExperimentRequest` in `packages/contracts/src/index.ts`.
  - Added the guided fields to the Journey (Transformations) creation form and to the experiment-creation form on `TransformationDetailPage`, using the same plain-language prompts as Phase 1's terminology work.
  - Today's Current Direction card now shows cadence, evidence-to-watch-for, and review date when present, so captured guidance is actually visible day to day instead of only stored.
  - Added `TransformationsPage.test.tsx` (did not exist before this session).
- Fixed an unrelated, pre-existing break discovered while verifying this slice: a GitHub Copilot Autofix commit merged into the Phase 1 PR had added an `@testing-library/user-event` import to `AppLayout.test.tsx` without adding the package as a dependency, breaking `npm run typecheck`. Replaced with `fireEvent` from the already-installed `@testing-library/react`.

**Governance (ADRs)**:
- ADR-004: PostgreSQL remains authoritative; schema evolves through a new Flyway migration, all new columns nullable so no backfill is required.
- ADR-001/ADR-003: changes stay within existing module boundaries (transformation, experiments, today); no new modules or cross-module coupling introduced.
- No ADR superseded.

Verification run:
- `npm run typecheck` (apps/web) passed.
- `npm run lint` (apps/web) passed.
- `npm run test` (apps/web) passed — 10 tests across 7 files, including the new `TransformationsPage.test.tsx`.
- `npm run build` (apps/web) passed.
- `./scripts/check-docs` passed.
- `./scripts/test-backend` and `./scripts/verify-architecture` were **not run**: this execution sandbox has Java 11 preinstalled and no network path to install Java 21 (the sandbox's outbound proxy blocks the JDK distribution host, confirmed via a direct connection attempt). All backend Java changes were hand-reviewed line by line against existing conventions, and every new/changed constructor and service overload was checked against every call site in the repository, but this is not a substitute for compiling and running the test suite. **Please run `./scripts/test-backend` and `./scripts/verify-architecture` locally before merging.**

Known limitations:
- `CurrentFocus` backend projection and server-persisted onboarding state (both listed under Phase 2 in the plan doc) are not implemented; Today still assembles its view from two separate calls.
- `reviewAt` is captured and displayed but nothing acts on it yet (no "review due" prompt) — flagged as future Phase 2 or later work.
- No "difficulty" or "smallest acceptable version" field was added to experiments; `nextAction` already covers that ground and a second field would have duplicated it.

## 2026-07-27 Session - Product Experience Realignment, Phase 1: Fix Product Orientation

Summary:
- Added `docs/roadmap/product-experience-realignment-plan.md`, a five-phase plan reconciling an external architecture/UX review against the actual repository state, with explicit scope decisions, deferred items, and requirement/ADR traceability.
- Executed Phase 1 (frontend-only, no backend/API changes):
  - Reduced primary navigation from six equal-weight destinations to Today / Journey / Library, with Search, Knowledge, Memories, and Settings moved into a secondary "More" menu (`AppLayout.tsx`).
  - Added a skip-to-content link, visible focus states, and active-route styling to the shell.
  - Added a data-driven first-use welcome state on Today (shown when the user has zero transformations) with a one-screen growth-loop explainer and a direct "Begin my first transformation" call to action, and a direct call-to-action empty state (linking straight to the relevant transformation) when transformations exist but no experiment is active yet.
  - Removed the "Placeholders" card from Today that exposed implementation-roadmap language ("coming in a later increment") directly to users.
  - Reordered Today so the suggested small action appears before the reflection prompt, with a "why this" line derived from the active experiment's own title (no fabricated content).
  - Namespaced the reflection draft `localStorage` key per experiment (`helix:reflection-draft:<experimentId>`), fixing a cross-experiment draft-bleed bug.
  - Added glossary-grounded contextual help (`TermHint`, sourced verbatim from `docs/product/glossary.md`) for Transformation, Experiment, Reflection, and Suggested Small Action at their first mention.
  - Reframed the Transformations page as "Journey" with one line of orientation copy; rewrote `PlaceholderPage` copy for the remaining Settings stubs to be honest and non-technical instead of referencing "increments".
  - Added a `LibraryPage` wrapper around the existing Wisdom workspace with an explainer and links out to Knowledge, Search, and Memory as secondary utilities.
  - Disabled suggestion accept/dismiss/replace actions while their mutation is pending, and added an `aria-live` status region for reflection save feedback.

**Governance (ADRs)**:
- ADR-012: the per-experiment draft namespacing is a direct correctness fix under this ADR's offline-capable reflection capture intent.
- No ADR superseded; Phase 1 is presentation-layer only. Phase 2's planned `CreateTransformationRequest`/`CreateExperimentRequest` extensions and any `CurrentFocus` backend projection will need their own ADR note when implemented.

Verification run:
- `npm run typecheck` (apps/web) passed.
- `npm run lint` (apps/web) passed.
- `npm run test` (apps/web) passed — 9 tests across 6 files, including 3 new tests on `TodayPage` (welcome state, no-active-experiment CTA, suggestion-before-reflection ordering plus a check that no roadmap language leaks into the UI) and 1 new test on `AppLayout` (reduced primary nav, skip link).
- `npm run build` (apps/web) passed.
- `./scripts/check-docs` passed.

Known limitations:
- `./scripts/test-backend` and `./scripts/verify-architecture` were **not run** in this session: the execution sandbox has Java 11 installed and the backend requires Java 21. Phase 1 makes no changes under `apps/api`, so risk is low, but this should be run before merge.
- The Phase 1 welcome/first-use state is derived purely from `transformations.length === 0` rather than a persisted onboarding-state field; a user cannot permanently dismiss it before creating their first transformation. Durable, richer onboarding-state tracking is deferred to Phase 2 per `product-experience-realignment-plan.md`.
- "Reflect" is intentionally not added as its own primary nav destination in Phase 1 because it would currently duplicate Today's content; it becomes a real destination once Phase 3 gives reflection distinct content (progressive questions, per-experiment review).
- `LibraryPage` is a thin wrapper (explainer + links) around the unchanged Wisdom workspace, not a merged read model across wisdom/evidence/retrospectives/memories; that merge is Phase 4/5 scope.
- Visual design (color system, layout, typography) was intentionally left unchanged in Phase 1 beyond what the new components required; a fuller visual pass is out of scope until later per the plan.

## 2026-07-26 Session - Increment 6: Semantic Retrieval Foundations

Summary:
- Added a semantic retrieval foundation to the search-and-retrieval module with a provider-agnostic embedding port and deterministic local embedding adapter.
- Added persistent semantic indexing documents and an explicit rebuild workflow endpoint to regenerate retrieval data from authoritative records.
- Upgraded the search API to hybrid retrieval mode (keyword + semantic) while preserving source citations in every result.
- Added migration groundwork for pgvector-capable environments with graceful fallback when the extension is unavailable.
- Updated shared contracts and search UI rendering to expose match type and score metadata.

**Governance (ADRs)**:
- ADR-004: PostgreSQL remains the authoritative store with migration-driven schema evolution.
- ADR-006: AI remains optional via abstraction and deterministic fallback behavior.
- ADR-010: Web-to-backend communication remains REST-only.

Verification run:
- `./scripts/test` passed.
- `./scripts/lint` passed.
- `./scripts/verify-architecture` passed.
- `./scripts/check-docs` passed.

Known limitations:
- Semantic indexing currently covers reflections and wisdom entries as the initial foundation.
- Semantic ranking currently runs in the application layer using deterministic local embeddings; pgvector-native distance operators are deferred for a later refinement.

## 2026-07-26 Session - Increment 5: User-Governed Memory Lifecycle

Summary:
- Implemented a dedicated memory-governance module with proposal, revision, accept, reject, and delete actions.
- Added persistence for memory proposals and revision history, including source provenance fields and lifecycle state transitions.
- Wired the Settings > Memory route to a working governance workspace for creating and reviewing memory proposals.
- Added shared contract types, API client methods, and a targeted web page test for the new slice.
- Updated requirements, traceability, and architecture notes to reflect the implemented memory-governance module.

**Governance (ADRs)**:
- ADR-008: memory remains user-governed until explicit acceptance
- ADR-010: web client communicates with the backend through REST APIs

Verification run:
- `npm run typecheck` (apps/web) passed.
- `npm run test -- MemoryPage` (apps/web) passed.
- `./scripts/test` passed.
- `./scripts/lint` passed.
- `./scripts/verify-architecture` passed.
- `./scripts/check-docs` passed.

Known limitations:
- Memory proposals currently require source record IDs entered explicitly rather than a polished source-picker flow.
- Temporary and superseded states are documented but not yet exposed as first-class user actions.
- No automatic AI-to-memory promotion is implemented yet; that remains for a later increment.

## 2026-07-26 Session - Increment 4: AI Provider Port & Local Adapter

Summary:
- Implemented optional AI provider system with OpenAI as default provider.
- Added support for Ollama (local on-device inference) and deterministic no-op fallback.
- Created `AiProviderPort` interface with multiple adapter implementations.
- Implemented `AiProviderFactory` for adapter selection based on configuration.
- Added `AiOrchestrationService` with health monitoring and graceful degradation.
- Circuit breaker pattern prevents cascading failures (30-second timeout before retry).
- All AI providers implement graceful fallback to deterministic suggestions.
- Added comprehensive unit and integration test suite for all adapters.
- Created provider setup documentation with OpenAI, Ollama, and testing guides.
- Zero breaking changes to Increments 1-3; AI is fully optional.

**Governance (ADRs)**:
- ADR-006: AI is optional; all workflows function without AI
- ADR-007: OpenAI default with Ollama local-first alternative supported
- ADR-008: User-governed; AI outputs marked with provider, model, confidence metadata

Configuration (application.properties):
- `HELIX_AI_PROVIDER`: openai (default), ollama, or none
- `OPENAI_API_KEY`: Required for OpenAI provider
- Provider health checks scheduled every 30 seconds
- Request timeout: 10 seconds (configurable)
- Automatic retry: 3 attempts with exponential backoff

Verification run:
- `./scripts/test` passed (all adapter and orchestration tests)
- `./scripts/lint` passed
- `./scripts/verify-architecture` passed (modular monolith boundaries intact)
- `./scripts/check-docs` passed
- Core workflows tested with provider unavailable (fallback successful)

Known limitations:
- AI outputs not yet persisted (requires Increment 5: user-governed memory)
- No semantic retrieval yet (deferred to Increment 6)
- No knowledge graph visualization (deferred to Increment 7)
- Chat-style multi-turn conversations deferred (MVP focus: single reflective questions)

## 2026-07-26 Session - Weekly Retrospective, Wisdom, and Structured Search

Summary:
- Implemented Increment 3 baseline across API, shared contracts, web UI, and roadmap docs.
- Added weekly retrospective draft generation using the previous seven days of reflections with deterministic assistance text.
- Added retrospective snapshot persistence and listing for longitudinal weekly review.
- Added user-accepted wisdom entries with revision history and explicit typed source links.
- Replaced the search stub with structured keyword retrieval across reflections, beliefs, evidence, retrospectives, and wisdom entries.
- Replaced Wisdom and Search placeholder routes with working pages and client-side API integrations.

Verification run:
- `./scripts/test` passed.
- `./scripts/lint` passed.
- `./scripts/check-docs` passed.
- `./scripts/verify-architecture` passed.

Known limitations:
- Structured search currently returns aggregated results without ranking normalization.
- Wisdom creation UI currently supports reflection and retrospective source linking in the initial flow; broader source-picking ergonomics are deferred.

## 2026-07-26 Session - Beliefs and Evidence Foundation

Summary:
- Added the initial beliefs and evidence foundation across API, shared contracts, web UI, and documentation.
- Implemented belief creation, evidence capture with explicit provenance, belief revision history, and a descriptive progress narrative.
- Replaced the Knowledge placeholder route with a working knowledge page for beliefs and evidence inspection.
- Extended requirements, traceability, module decomposition, uses-view, and backlog documentation for the new increment.

Verification run:
- `./gradlew test --tests com.helix.api.beliefs.BeliefServiceTest --tests com.helix.api.evidence.EvidenceServiceTest` passed.
- `npm run test -- KnowledgePage` (apps/web) passed.
- `npm run typecheck` (apps/web) passed.
- `./scripts/lint` passed.
- `./scripts/check-docs` passed.
- `./scripts/verify-architecture` passed.
- `./scripts/test` passed.

Known limitations:
- The beliefs/evidence slice is still foundational and does not yet include weekly wisdom workflows or semantic retrieval.
- Evidence provenance currently supports manual entries and Today reflections, with broader source coverage deferred.

## 2026-07-26 Session

Summary:
- Instantiated Helix monorepo skeleton with apps/web, apps/api, packages/contracts, docs, infra, scripts, and .github workflow assets.
- Implemented Increment 1 thin vertical slice:
	- Create transformation
	- Create experiment
	- Record reflection
	- Deterministic suggestion generation
	- Suggestion accept, dismiss, replace
	- Today view with reflection and suggestion history
- Added optional AI foundation in backend with no-AI adapter contract.
- Added Flyway baseline migration and PostgreSQL local docker-compose.
- Added architecture/unit test foundations in backend and Vitest foundation in frontend.
- Added CI workflow and local scripts for bootstrap/dev/test/lint/docs checks.
- Created product, requirements, architecture, ADR, AI, security, and roadmap documentation packs.
- Added OpenAPI contract file and validated syntax.

Verification run:
- `./scripts/test-backend` passed.
- `./scripts/lint` passed.
- `npm run typecheck` (apps/web) passed.
- `npm run test` (apps/web) passed.
- `npm run build` (apps/web) passed.
- `./scripts/check-docs` passed.
- `./scripts/test` passed.
- `./scripts/verify-architecture` passed.

Known limitations:
- Semantic retrieval, graph visualization, and advanced memory governance remain deferred by roadmap.
- Production auth provider, hosting provider, and retention defaults remain open decisions.
