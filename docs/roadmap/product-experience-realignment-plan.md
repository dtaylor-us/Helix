# Product Experience Realignment Plan

Status: Phase 1 in progress
Owner: Agent-assisted delivery session, 2026-07-27
Source: External architecture/UX review of the `main` branch (2026-07-27), reconciled against the actual repository state in this document.

## Why this plan exists

An external review of the repository found that the technical foundation (modular-monolith Spring Boot backend, React/TanStack PWA, PostgreSQL, optional AI adapters, ADR-driven governance) is sound, but the product currently exposes its domain model and implementation roadmap directly to the user instead of guiding them through a coherent growth practice. Concretely, on `main` at the time of this review:

- `AppLayout` presents six equal-weight nav destinations (Today, Transformations, Wisdom, Search, Knowledge, Settings) with no explanation of what each means or where to start.
- `TodayPage` has no first-use state: when there is no active experiment it shows a single sentence with no action, and it ships a card literally titled **"Placeholders"** with the text *"Recent Insight: coming in a later increment."* / *"Continue Conversation: coming in a later increment."* — implementation-roadmap language leaking into production UI.
- `TransformationsPage` asks for a bare Title + Purpose with no connection to vision, identity, or obstacles.
- `PlaceholderPage` (used for Settings, Settings/Privacy, Settings/AI, Settings/Export) tells the user *"This area is intentionally scaffolded and will be expanded in future increments."*
- The reflection draft is stored under a single global `localStorage` key (`helix:reflection-draft`) rather than namespaced per experiment, so a draft can bleed across experiments.
- Today's action-suggestion card is ordered after the reflection card, even though the report and the product's own loop (`Vision → Transformation → Belief → Experiment → Reflection → Evidence → Insight → Wisdom`) put the small next action before the reflection on it.

This plan captures the corrected, verified version of the reviewer's recommendations and sequences them into phases that can each ship as an independently reviewable, backend-safe increment, consistent with the existing `implementation-roadmap.md` / `development-log.md` delivery discipline already used in this repository (see `.github/agents/helix-next-phase-implementer.agent.md` and `AGENTS.md`).

## Guiding constraint

> Helix should never make the user decide which module to use. It should understand where they are in the growth loop and gently guide them to the next useful step.

Every phase below is evaluated against this constraint, and against the repository's own product principles (`docs/product/product-principles.md`): user-owned data, calm/nonjudgmental interaction, AI as optional and assistive, reflection/evidence over gamification, revisable and traceable insight.

## Phase plan

### Phase 1 — Fix product orientation (frontend-only, no backend/API changes)
Scope: navigation, first-use state, empty states, terminology help, Today reprioritization, removal of roadmap language from the UI, and one data-integrity fix (per-experiment draft namespacing).

Deliverables:
1. Primary navigation reduced to **Today / Journey / Library**; Search, Knowledge, Memory, and Settings moved to a secondary "More" menu. (`AppLayout.tsx`)
2. Active-route styling, a skip-to-content link, and visible focus states added to the shell.
3. A client-side onboarding/first-use state (localStorage-backed, consistent with the existing offline-draft pattern under ADR-012) that shows a one-screen journey explainer and a single "Begin my first transformation" call to action when the user has no transformations yet.
4. `TodayPage` empty states rewritten with direct calls to action instead of inert sentences; the "Placeholders" card removed entirely.
5. `TodayPage` card order changed so the suggested small action appears before the reflection prompt, with a one-line "why this" derived from the active experiment (no fabricated content).
6. Reflection draft key namespaced per experiment (`helix:reflection-draft:<experimentId>`), fixing the cross-experiment draft bleed.
7. Lightweight, glossary-grounded contextual help ("What's a Transformation?" / "What's an Experiment?" / "What's Wisdom?") added at first mention on Journey/Today, sourced verbatim from `docs/product/glossary.md` — no invented definitions.
8. `TransformationsPage` reframed as "Journey" with one line of orientation copy; `PlaceholderPage` copy rewritten to be honest and non-technical (no "increments" language) for the remaining Settings stubs.
9. A `LibraryPage` wrapper introduced around the existing Wisdom workspace, with a short explainer and links out to Knowledge, Search, and Memory as secondary utilities — content itself is unchanged (Phase 4 owns the deeper wisdom-workflow rework).
10. Suggestion accept/dismiss/replace buttons disabled while their mutation is pending; reflection save status exposed via an `aria-live` region.

Explicitly deferred out of Phase 1 (see below for where they land): a dedicated "Reflect" primary destination, the guided transformation/experiment wizards, progressive reflection questions, contextual "keep this lesson" wisdom capture, the `Current Focus` backend projection, and any visual redesign beyond what's needed to support the above (no new color system, no layout overhaul).

### Phase 2 — Build the guided journey (frontend + backend)
- Transformation creation wizard (what would you love / why it matters / desired identity / current obstacle) — requires new/extended request fields on `CreateTransformationRequest` and `TransformationEntity`, so this is a coordinated frontend+backend change, not a frontend-only one.
- Guided experiment design (belief/question, cadence, evidence definition, difficulty, review date) — extends `CreateExperimentRequest` and `ExperimentEntity`.
- A `GET /api/v1/today` — or a new `CurrentFocus` — backend projection so the frontend stops assembling context from multiple calls.
- Server-persisted onboarding state (replacing the Phase 1 localStorage flag) so onboarding progress survives devices.
- A dedicated "Reflect" primary nav destination becomes justified once experiment review/reflection has real distinct content beyond what Today already shows.

### Phase 3 — Improve reflection quality
- Morning-intention vs. evening-review framing.
- Progressive reflection questions (attempted? what happened? noticed? evidence? surprise?) instead of one textarea, backed by a real question model rather than hard-coded strings.
- Evidence extraction from reflections tied into the existing `Evidence`/`Belief` domain.

### Phase 4 — Integrate wisdom naturally
- "This reflection may contain a lesson worth keeping" contextual prompt with an editable proposed statement, replacing manual statement entry as the primary path (manual entry remains available as an advanced action).
- Weekly narrative retrospective surfaced contextually rather than only inside the Wisdom workspace.

### Phase 5 — Surface advanced intelligence progressively
- Search/Knowledge graph/Memory review promoted back into primary navigation (or a prominent secondary spot) once there is enough user data for them to be useful, with real empty-state guidance until then.
- AI companion moments embedded contextually rather than as a separate destination.

## Requirement and ADR traceability (Phase 1)

Phase 1 changes are presentation-layer only and do not add, remove, or reinterpret functional requirements. They support existing requirements more faithfully:
- HELIX-UX-001 (calm, nonjudgmental language), HELIX-UX-002 (keyboard-navigable core forms/actions) — directly strengthened (focus states, skip link, disabled-while-pending, aria-live).
- HELIX-FR-006/007 (Today history/summary) — unchanged in data shape; presentation reordered.
- HELIX-BR-004 (no single-score reduction of progress) — unaffected; no new scoring introduced.
- ADR-012 (offline-capable reflection capture) — the per-experiment draft key fix is a direct correctness fix under this ADR's intent.
- No ADR is superseded. Phase 2's `CreateTransformationRequest`/`CreateExperimentRequest` extensions and any new `CurrentFocus` projection will need their own ADR note or amendment when implemented; that is explicitly out of scope for this Phase 1 change.

## Explicit assumptions and open decisions

- **Decision:** the Phase 1 "first-use" welcome state is derived from real data (`transformations.length === 0`), not a dismissible flag. This is deliberately simpler than the localStorage onboarding flag originally sketched for this plan: it needs no persisted client state, it cannot go stale, and it behaves correctly across devices (a fresh browser with zero transformations sees the welcome state; any account with at least one transformation does not). It self-clears the moment the user creates their first transformation. The tradeoff is that a user who wants to skip past the welcome screen without creating anything yet cannot permanently dismiss it — for Phase 1 this is treated as acceptable, since the welcome state itself now doubles as a real, low-cost empty state rather than nagging chrome. Richer onboarding-state tracking (e.g., distinguishing "skipped welcome" vs. "completed guided setup", AI configured, first reflection done) remains Phase 2 work, and would live server-side per the "Add onboarding-state persistence" recommendation.
- **Open decision, deferred rather than guessed:** the reviewer's recommended primary nav is Today / Journey / Reflect / Library. This plan ships Today / Journey / Library in Phase 1 and defers "Reflect" as a distinct primary destination because, today, reflection has no content that is meaningfully different from what already lives on Today — shipping a nav item that duplicates Today would itself be the kind of hollow, implementation-driven navigation this plan is trying to fix. "Reflect" becomes a real destination once Phase 3 gives it distinct, real content (progressive questions, per-experiment reflection history/review).
- **Assumption:** "Library" in Phase 1 is a thin wrapper around the existing Wisdom workspace plus links out to Knowledge/Search/Memory, not a new merged data view. A true merged Library view (wisdom + evidence + retrospectives + memories in one place) is Phase 4/5 scope once those modules have stable, complementary read models.
- Terminology help text is drawn verbatim from `docs/product/glossary.md` so as not to invent product language ahead of a real content-design pass (recommended in the source review as its own backlog item).

## Verification plan for Phase 1

- `npm run typecheck` (apps/web)
- `npm run lint` (apps/web)
- `npm run test` (apps/web) — existing Today/Knowledge/Search/Memory/Wisdom page tests plus new tests for the onboarding empty state and navigation.
- `./scripts/check-docs`
- `./scripts/test-backend` and `./scripts/verify-architecture` are **not run in this session** because the execution sandbox has Java 11 installed and the backend requires Java 21; Phase 1 makes no backend changes, so risk is low, but this is a known gap and the user/CI should run these before merging.
