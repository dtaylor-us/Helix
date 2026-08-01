# Product Experience Realignment Plan

Status: Phase 1 shipped; Phase 2 slice A (guided transformation + experiment creation) shipped; Phase 3 slice A (progressive reflection questions + morning/evening framing) shipped; Phase 4 slice A (contextual wisdom-capture prompt + weekly retrospective teaser on Today) shipped; Phase 5 slices A-C (AI-generated suggestions, AI weekly retrospective, AI-drafted experiment proposals) shipped; Phase 5 slice D (conversational reflection flow) scoped but not yet built — largest remaining slice, needs its own design pass; CurrentFocus projection, server-persisted onboarding state, and full evidence-extraction UX still open
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

## Phase 2 progress (2026-07-27, second session)

Shipped, as "Phase 2 slice A":

- Guided transformation creation: `Transformation` gained optional `desiredIdentity` ("Who are you becoming through this?") and `obstacle` ("What currently gets in the way?") fields, surfaced on the Journey page's creation form and on the transformation detail view. Backed by `V6__guided_journey_fields.sql`.
- Guided experiment design: `Experiment` gained optional `cadence` ("How often will you try this?"), `evidenceOfSuccess` ("What would count as useful evidence?"), and `reviewAt` (a plain review date) fields, surfaced on the experiment-creation form and echoed back on Today's Current Direction card when present.
- `CreateTransformationRequest`/`CreateExperimentRequest` and `TransformationEntity`/`ExperimentEntity` extended accordingly; existing 4-arg/7-arg constructors were preserved (overloaded, not replaced) so existing tests and call sites did not need to change.
- New `TransformationServiceTest` and `ExperimentServiceTest` were added (neither existed before this session).

Deliberately trimmed from the original Phase 2 field list to avoid over-engineering: no separate "difficulty" or "smallest acceptable version" field was added — the existing `nextAction` ("smallest next action") already covers that ground, and adding a second, overlapping field would have duplicated it without adding real value.

Still open from Phase 2 (not attempted this session):
- The `CurrentFocus` backend projection (frontend still assembles Today from two calls: `/today` + `/transformations`).
- Server-persisted onboarding state (Phase 1's data-driven welcome state is unchanged).
- The dedicated "Reflect" primary nav destination (still correctly deferred to Phase 3 per the reasoning above).
- Any UI surfacing of `reviewAt` as a "review due" prompt — the field is captured and displayed, but nothing yet acts on it when the date passes.

Verification for this slice:
- `npm run typecheck`, `npm run lint`, `npm run test` (10 tests / 7 files, including new `TransformationsPage.test.tsx`), and `npm run build` all passed for `apps/web`.
- Backend changes (`apps/api`) were hand-reviewed carefully against existing conventions but **could not be compiled or tested**: this execution sandbox has no network path to install a JDK 21 toolchain (only JDK 11 is preinstalled, and the sandbox's outbound proxy blocks the JDK distribution host). Run `./scripts/test-backend` and `./scripts/verify-architecture` locally before relying on this backend change.
- While fixing this slice's verification, an unrelated pre-existing break was found and fixed: a GitHub Copilot Autofix commit merged into Phase 1 had added an `@testing-library/user-event` import to `AppLayout.test.tsx` without adding the package as a dependency, which broke `npm run typecheck`. Replaced with `fireEvent` from the already-installed `@testing-library/react`, which achieves the same click-to-open assertion without a new dependency.

## Phase 3 progress (2026-07-27, third session)

Shipped, as "Phase 3 slice A":

- Progressive reflection questions: `Reflection` gained optional `attempted` (boolean), `noticed`, `evidenceNoted`, and `surprise` fields. On Today, once the user has typed something into the required "What happened" answer, a single "+ next question" button appears; clicking it reveals that follow-up question's textarea and (if more remain) the next "+ ..." button for the following one — one at a time, rather than a wall of optional fields shown up front. Backed by `V7__progressive_reflection_fields.sql`.
- A "Did you try it?" Yes/Not yet control was added ahead of the main answer, populating `attempted`.
- The Reflect section heading is now time-of-day-conditional ("Morning check-in" before noon, "Evening review" after) with matching subtext, giving the section real, distinct content — this is what unblocks promoting "Reflect" to a primary nav destination in a later phase, per the reasoning recorded in the Phase 1 open-decision above (still not attempted this session; see "Still open" below).
- `ReflectionCard` on the `/today` response now includes `attempted`/`noticed`/`evidenceNoted`/`surprise` when present; Today's reflection history list surfaces `noticed`/`evidenceNoted`/`surprise` when present.
- After a successful save, a low-cost nudge — "This might be useful evidence — add it in Knowledge." — links to the already-existing evidence-from-reflection flow on `KnowledgePage` (confirmed implemented via its `EvidenceSourceMode` picker sourcing `reflectionHistory`). No new evidence-extraction UI was built, since one already exists and duplicating it would have been the over-engineering this plan is trying to avoid.
- `CreateReflectionRequest`/`ReflectionEntity`/`ReflectionService.create` extended accordingly; the existing 2-arg `create(experimentId, content)` service method and 4-arg entity constructor were preserved as overloads, so the deterministic suggestion flow (confirmed to only read `nextAction` and attempt count, never reflection content) and existing call sites were unaffected.
- New `ReflectionServiceTest` case (`createWithProgressiveAnswersPersistsThem`) added alongside the existing test, which now also asserts the new fields default to `null` when omitted.

Deliberately trimmed from Phase 3 scope:
- No new evidence-extraction UI (see above — the existing Knowledge flow already covers this; only a navigational nudge was added).
- Follow-up answers (`noticed`/`evidenceNoted`/`surprise`) are **not** persisted to `localStorage` the way the main `content` draft is. Only the required answer survives a page reload/navigation-away; the optional follow-ups do not. This is called out explicitly in the UI itself ("Follow-up answers are not [saved on this device]") rather than silently losing data. Extending per-experiment draft persistence to the follow-up fields is straightforward but was left out of this slice to keep it small; it's a natural pickup for the next reflection-focused increment.
- "Reflect" was **not** promoted to a primary nav destination this session, even though the morning/evening framing gives it more distinct content now — doing so is a nav-structure change that deserves its own deliberate pass (and ideally a check that Today doesn't become redundant with it) rather than a side effect of this slice.
- The `reviewAt` "review due" surfacing carried over from Phase 2 remains open.

Still open from Phase 3 (not attempted this session):
- The `CurrentFocus` backend projection.
- Server-persisted onboarding state.
- Promoting "Reflect" to a primary nav destination.
- Persisting follow-up answers to `localStorage`.
- Any UI surfacing of `reviewAt` as a "review due" prompt.

Verification for this slice:
- `npm run typecheck`, `npm run lint`, `npm run test` (11 tests / 7 files, including the new progressive-follow-up test in `TodayPage.test.tsx`), and `npm run build` all passed for `apps/web`, run against a scratch clone with its own `npm install` (the sandbox's Linux/arm64 environment cannot run native bindings installed against the user's Mac `node_modules`).
- `./scripts/check-docs` passed.
- Backend changes (`apps/api`) were hand-reviewed carefully against existing conventions but **could not be compiled or tested**: this execution sandbox has only JDK 11 preinstalled, has no permissions to install packages via `apt`, and its outbound proxy blocks the JDK 21 distribution host (`api.adoptium.net`, confirmed via a direct `curl` returning `403 blocked-by-allowlist`). Run `./scripts/test-backend` and `./scripts/verify-architecture` locally before relying on this backend change.

## Phase 4 progress (2026-07-27, fourth session)

Shipped, as "Phase 4 slice A":

- A contextual wisdom-capture prompt on Today: after a reflection is saved, if there's anything to build a statement from, a card titled "This reflection may contain a lesson worth keeping" appears with an editable, deterministically prefilled statement (priority order: `evidenceNoted` → `noticed` → `surprise` → the main answer, trimmed and capped at 500 characters — no AI involved, consistent with ADR-006's optional-AI stance) and two actions, "Save as wisdom" and "Not now". Saving calls the existing `POST /api/v1/wisdom` endpoint with a `REFLECTION` source pointing at the just-saved reflection.
- This becomes the primary path for turning a reflection into wisdom; manual statement entry on the Wisdom page (`WisdomPage.tsx`, reachable via Library) is unchanged and remains available as the advanced/edit path, exactly as the plan called for.
- A weekly narrative retrospective teaser: a "This week" card on Today shows the same deterministic `summary`/`assistance` text already generated by `GET /api/v1/wisdom/weekly-retrospective`, with a link to the full workspace in Library. It only renders once there is at least one reflection summary for the week, and the underlying query is only fetched once the user has at least one transformation (so it doesn't fire on the true first-use welcome screen).
- **No backend changes were required for this slice.** `POST /api/v1/wisdom` already accepted a `REFLECTION`-sourced statement and `GET /api/v1/wisdom/weekly-retrospective` already existed; Phase 4 slice A only adds new frontend call sites for endpoints Phase 3 (and earlier increments) had already shipped. This also means the JDK 21 verification gap that has applied to every prior phase does not apply here — the full verification suite ran cleanly in this session.
- Added two new `TodayPage.test.tsx` cases: one exercising the prefill-priority logic and the full save flow (asserting the exact `createWisdom` payload), and one asserting the weekly retrospective teaser renders with a working link to `/library` and stays hidden when there's nothing to show.

Deliberately trimmed from Phase 4 scope:
- No changes to `WisdomPage.tsx` itself — manual entry remains there unmodified as the advanced path, per the plan's own wording ("manual entry remains available as an advanced action").
- No new backend endpoint or "detected lesson" heuristic beyond the deterministic prefill priority described above; a genuinely smarter proposal (e.g. AI-assisted or looking across multiple recent reflections) is out of scope and would need its own ADR discussion given ADR-006/007's AI-optionality constraints.
- The proposed statement is derived from a single reflection only; nothing yet looks across a full week's reflections to propose a wisdom statement the way the retrospective teaser summarizes a week's reflections. That remains open for a later increment.

Still open from Phase 4 (not attempted this session):
- The `CurrentFocus` backend projection.
- Server-persisted onboarding state.
- Promoting "Reflect" to a primary nav destination.
- Persisting follow-up answers to `localStorage`.
- Any UI surfacing of `reviewAt` as a "review due" prompt.
- Phase 5 (surfacing Search/Knowledge graph/Memory review, contextual AI companion moments).

Verification for this slice:
- `npm run typecheck`, `npm run lint`, `npm run test` (13 tests / 7 files, including two new `TodayPage.test.tsx` cases), and `npm run build` all passed for `apps/web`, run against a scratch clone synced from the mounted repo with its own `npm install`.
- `./scripts/check-docs` passed.
- No backend changes were made in this slice, so the usual JDK 21 verification gap does not apply here.
- While verifying this slice, an unrelated pre-existing gap was found and fixed: `@testing-library/react`'s automatic per-test DOM cleanup was never actually running, because it only self-registers when Vitest's `test.globals` option is enabled (this project doesn't enable it), so `afterEach`/`cleanup` was never wired up. Every test's rendered DOM was silently piling up in the document instead of being unmounted between tests. This had gone unnoticed because no earlier test asserted the *absence* of something using `queryByText(...).not.toBeInTheDocument()` in a way that leftover DOM from a prior test could break — the two new Phase 4 tests were the first to do so, and both failed only when run as part of the full suite (not in isolation), which is what surfaced it. Fixed by adding `afterEach(cleanup)` to `src/test/setup.ts`, which now protects every test file, not just this one.

## Phase 5 — Real AI in suggestions, retrospectives, experiment drafting, and reflection (scoped 2026-08-01)

Scope change, requested directly by the user rather than derived from the external review: replace
deterministic string-templated "intelligence" with real AI reasoning in the places that were only
pretending to be smart, and move the reflection UX away from structured form-filling toward
selection/prompting. The user explicitly chose the broadest first-slice scope (AI suggestions + AI
retrospectives + AI-drafted experiments + a conversational reflection flow), explicitly chose to
**drop** the previously-mandatory deterministic no-AI fallback requirement (see ADR-016, which
narrows ADR-006 for these features only), and chose **OpenAI** as the provider.

Given the size of that combined scope, this is sequenced into four slices, each independently
shippable, mirroring the delivery discipline used in Phases 1-4:

- **Slice A — AI-generated "Suggested Small Action" (this session, shipped).** `AiAssistantPort`
  gains `suggestNextAction(String context)`, implemented in all three adapters (OpenAI, Ollama,
  NoOp) alongside the existing `suggestReflectiveQuestion`. `ReflectionService.create(...)` now
  calls the AI port (built from the experiment's title/hypothesis/nextAction plus the reflection's
  content/noticed/evidenceNoted/surprise and the prior-attempt count) instead of
  `SuggestionService.createDeterministic`. `SuggestionEntity` gained `source`
  (`AI`/`DETERMINISTIC`), `ai_provider`, and `ai_model` columns (migration `V8`) so provenance is
  recorded and visible; Today shows an "(AI suggested — openai)" badge next to the suggestion text
  when it came from a live model call. `SuggestionService.createDeterministic` and the
  `NoAiAssistantAdapter`/circuit-breaker fallback paths are unchanged and still exist — they're now
  what a user sees during a provider outage or with `HELIX_AI_PROVIDER=none`, not a maintained
  parallel experience (see ADR-016).
- **Slice B — AI-authored weekly retrospective (this session, shipped).**
  `AiAssistantPort` gains `summarizeWeek(String context)`, returning a two-part
  `AiWeeklySummary(summary, assistance, provider, model, deterministicFallback)`. Each adapter
  prompts for exactly two labeled lines (`SUMMARY:` / `NEXT:`) and parses them with a shared
  `extractLabeledLine` helper; a missing/blank required line is treated as a parse failure and
  falls back, same convention as Slice A. `WeeklyRetrospectiveService.draft()` now calls this
  instead of its old count/length-based string concatenation — **except** when there are zero
  reflections in the window, which stays a genuine deterministic empty state (there's nothing for
  AI to narrate, so it isn't invoked at all; confirmed by a `verifyNoInteractions` test).
  `WeeklyRetrospectiveEntity` gained `source`/`ai_provider`/`ai_model` columns (migration `V9`,
  same backward-compatible-legacy-constructor pattern as `SuggestionEntity`). Today's "This week"
  teaser and the Wisdom page's retrospective card both show the same "(AI suggested — openai)"
  badge convention as Slice A.
- **Slice C — AI-drafted experiment proposals (this session, shipped).** `AiAssistantPort` gains
  `proposeExperiment(String context)`, returning an `AiExperimentDraft` with
  title/hypothesis/nextAction/cadence/evidenceOfSuccess plus provenance; each adapter prompts for
  five labeled lines (`TITLE:`/`HYPOTHESIS:`/`NEXT_ACTION:`/`CADENCE:`/`EVIDENCE:`), treating a
  missing `TITLE` as a parse failure (the other four are genuinely optional, matching
  `CreateExperimentRequest`'s own optionality). New `ExperimentService.proposeDraft(transformationId)`
  builds context from the transformation's title/purpose/desiredIdentity/obstacle and calls the
  port — **nothing is persisted by this call**; it returns a plain `ExperimentDraft` record, not an
  entity, and a new `POST /api/v1/transformations/{id}/experiments/draft` endpoint exposes it. On
  `TransformationDetailPage`, a new "Draft this for me" button calls this endpoint and prefills the
  *existing* experiment-creation form fields, which the user can edit freely before pressing the
  unchanged "Save experiment" button — per ADR-008, nothing AI-drafted becomes a real experiment
  without that explicit, editable review step. Manual experiment creation is completely unchanged;
  the draft button is additive.
- **Slice D — Conversational reflection flow (scoped 2026-08-01, not yet built).** Replace the
  structured reflection form (main answer + up to three progressive follow-up questions) with a
  chat-style exchange: the user describes what happened in their own words, and the AI asks
  clarifying follow-ups conversationally instead of from a fixed question bank
  (`REFLECTION_FOLLOW_UP_QUESTIONS`). Scoped directly with the user via `AskUserQuestion` (the
  first attempt used developer-jargon phrasing the user flagged as unclear — "I don't understand
  the question" — and was re-asked in plain language before these decisions were made):
  - **Chat fully replaces the form**, rather than supplementing it. `TodayPage`'s "Morning
    check-in"/"Evening review" section becomes a chat UI; the existing progressive-follow-up form
    and `REFLECTION_FOLLOW_UP_QUESTIONS` are retired from that surface (the content module itself
    can remain, unused, rather than being deleted outright).
  - **The user decides when they're done**, via an explicit "I'm done" action — no fixed AI-turn
    cap. The AI can ask as many or as few clarifying questions as it judges useful; nothing forces
    the exchange to resolve after N turns.
  - **The AI still sorts the conversation into the same 4 boxes** the app already stores per
    reflection (the main "what happened" answer, plus the `noticed`/`evidenceNoted`/`surprise`
    follow-ups) — the chat transcript is not saved as free-form text instead of those fields. A
    second AI call, run once the user signals they're done, reads the full transcript and proposes
    values for all four fields; the user reviews and edits this structured proposal (an ADR-008
    propose-then-accept step) before the existing, unmodified `POST /api/v1/experiments/{id}/reflections`
    endpoint is called to actually save it.
  - **Reflection capture now requires network connectivity going forward.** ADR-012 currently
    guarantees reflection drafting works fully offline; a live back-and-forth with a model cannot.
    This needs a new ADR (ADR-017) that narrows ADR-012 specifically for reflection capture,
    documenting that composing/typing a message can still be locally buffered, but sending a
    message to the AI and finishing/structuring a reflection require a live connection.
  - Not yet built: `AiAssistantPort` needs two new methods (a chat-turn method, likely reusing the
    `AiSuggestion`-shaped record, and a transcript-to-structured-fields method returning a new
    record shaped like the four reflection fields plus provenance), implemented across all three
    adapters; a new stateless chat endpoint pair (turn / finish) that persists nothing itself; and
    the `TodayPage` chat UI plus an editable review step before save. This remains the largest
    single piece of Phase 5 and is sequenced after Slices A-C ship as reviewable PRs.

Deliberately **not** touched by Slices A-C:
- `AiProperties.timeoutSeconds` / `retryMaxAttempts` / `retryDelayMs` remain unused by any adapter
  (pre-existing gap, not introduced by these slices) — a slow OpenAI response will block the calling
  request rather than timing out into the circuit breaker. Flagged in ADR-016 as follow-up work,
  still not fixed.
- No changes to `suggestReflectiveQuestion` or its (currently uncalled) usage — it remains dormant.
- `AiOrchestrationService`'s health polling still bypasses the port abstraction and isn't wired into
  any live request path; left as pre-existing behavior.
- Slice C's AI-drafted proposal only covers the *first* experiment for a transformation's stated
  purpose/identity/obstacle — it doesn't look at prior experiments/reflections/evidence for that
  transformation the way a genuinely adaptive coach might. Acceptable for a first slice; flagged as
  a natural quality improvement for later.
- The `RetrospectiveSource` enum (wisdom module) and `SuggestionSource` enum (suggestions module)
  are intentionally separate, identical-shaped types rather than a shared one, to keep each feature
  module owning its own domain vocabulary (ADR-001) rather than introducing a cross-module domain
  dependency for a two-value enum.

Verification for Slices A-C:
- Backend changes could not be compiled or run in this sandbox (no JDK 21 available, per the
  constraint noted in every earlier phase) — hand-reviewed only, including a repo-wide grep for every
  call site of the constructors/methods that changed shape (`new ExperimentService(`,
  `new WeeklyRetrospectiveService(`, `new ReflectionService(`, `new SuggestionEntity(`,
  `new WeeklyRetrospectiveEntity(`) to confirm nothing else broke. `./scripts/test-backend` and
  `./scripts/verify-architecture` (ArchUnit layering test) need to be run by the user or CI before
  merging.
- Frontend (`packages/contracts`, `TodayPage.tsx`/`.test.tsx`, `WisdomPage.tsx`,
  `TransformationDetailPage.tsx` + new `.test.tsx`) verified via `npm run typecheck`, `npm run lint`,
  `npm run test` (15 tests / 8 files), and `npm run build` against a scratch clone.
- `./scripts/check-docs` run against the doc changes in these slices.
- No live call against a real OpenAI API key was possible in this sandbox (no network egress to
  `api.openai.com`); the adapters' parsing logic for the new labeled-line response formats was
  reviewed by hand but not exercised against an actual model response.
