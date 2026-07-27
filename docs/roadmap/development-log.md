# Development Log

This log is updated at the end of significant delivery sessions.

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
