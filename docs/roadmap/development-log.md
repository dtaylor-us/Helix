# Development Log

This log is updated at the end of significant delivery sessions.

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
