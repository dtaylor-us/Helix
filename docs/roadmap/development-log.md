# Development Log

This log is updated at the end of significant delivery sessions.

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
