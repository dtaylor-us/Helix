# Helix Roadmap (as of 2026-08-02)

This document reconciles three prior planning threads — the original
`implementation-roadmap.md` (Increments 0–10), the
`product-experience-realignment-plan.md` (Phases 1–5), and the ad hoc QA/bugfix
and design-prompt work done directly against `main` on 2026-08-01 — against
the actual current state of `main` (confirmed via GitHub: merged PRs, open
PRs, and live branches, not assumption). It supersedes both prior docs as the
single source of truth for "what's left." Those docs are left in place for
history; this one is what to work from going forward.

## How to read this doc

Each phase below states its goal, what's already true that it builds on, the
concrete scope, and open questions that need a decision before or during
implementation — same discipline as the realignment plan's own phases.
Phases are roughly ordered by dependency and risk, not by importance; several
could be worked in parallel by different sessions since they touch different
surfaces.

## Part 1 — Current state (verified against GitHub, not memory)

### Shipped and merged to `main`

- **Original Increments 0–6**: monorepo foundation, the Today/reflection
  vertical slice, beliefs & evidence, weekly retrospective + wisdom +
  structured search, the AI provider abstraction (OpenAI/Ollama/NoOp), and
  user-governed memory lifecycle.
- **Product Experience Realignment Phases 1–5 (A–D)**, merged via PRs #1–#7:
  navigation/onboarding rework, guided transformation & experiment creation,
  progressive reflection questions, contextual wisdom capture + weekly
  teaser, AI-generated suggestions/retrospective/experiment drafts, and the
  conversational reflection chat flow (ADR-017). PR #7 in particular was
  stacked (`base: product-experience-realignment-phase-5-slice-a`) and both
  legs are now in `main`.
- **Eight QA-driven bugfixes**, PRs #16–#23 (all merged 2026-08-01, all
  authored by the Copilot coding agent from the issue list filed after the
  first end-to-end QA pass): wisdom-capture card persistence, complete
  deterministic experiment-draft fields + visible provenance, actionable
  Memory source-validation errors, a minimum semantic-relevance threshold for
  search, inline Journey title validation, removal of the generated
  Spring-Security-password startup warning, local save confirmation on
  experiment creation, and weekly-snapshot save confirmation + visible
  history.
- **The two Today regressions** found in the follow-up QA pass (suggestion
  replacement text not displaying; weekly teaser stale until reload) are
  **already fixed in `main`** — confirmed directly in `TodayPage.tsx`
  (`displayedSuggestionText` reads `replacementText`; `reflectMutation`'s
  `onSuccess` invalidates both `['today']` and `['weekly-retrospective-draft']`).
  There's a leftover branch, `codex/fix-today-replacement-weekly-teaser`, with
  no open PR — it's almost certainly redundant with what's already merged.
  **Action: verify its diff is empty/superseded and delete it**, rather than
  opening a PR against it.

### Not yet merged

- Nothing outstanding. **PR #5** ("Fix: add missing CORS configuration to
  apps/api") looked open and unmerged in GitHub's PR list, but the fix it
  describes was applied directly to `main` outside of that PR (confirmed:
  `SecurityConfig.java` on `main` already has the exact `CorsConfigurationSource`
  bean and `.cors(Customizer.withDefaults())` wiring the PR proposed) — the PR
  itself was just a stale, redundant diff against an already-fixed file. It's
  now closed as not-planned.

### Written but not implemented

- **ADR-015 ("Data export and deletion as foundational capabilities")**
  exists as an accepted decision record, but nothing under `apps/api`
  implements it — no export endpoint, no deletion flow, no schema support.
  This is a decision waiting on Phase 6 below to catch up to it.

### Explicitly known gaps (carried forward, still true)

- `AiProperties.timeoutSeconds` / `retryMaxAttempts` / `retryDelayMs` remain
  defined but unused by every AI adapter (flagged in ADR-016, never fixed,
  present since Slice A).
- `CurrentFocus` backend projection — Today still assembles its view from two
  separate calls (`/today` + `/transformations`) instead of one endpoint.
- Server-persisted onboarding state — the welcome screen is still purely
  `transformations.length === 0`, not a real per-user flag.
- Memory has no contextual trigger anywhere in the app — it's a fully-built
  governance workspace (propose/accept/reject/revise, source validation) that
  nothing else in Helix ever calls into. Confirmed by code search: the only
  caller of `MemoryProposalService.create` is `MemoryProposalController`, and
  its only caller is `MemoryPage.tsx`'s own form.
- Slice D's frontend was **never actually verified** — the session that
  implemented it had no Node/npm toolchain available, so `typecheck`, `lint`,
  `vitest`, and `build` were never run against the conversational reflection
  chat UI. Backend tests and architecture checks did pass. This is a real,
  outstanding verification gap on code currently live in `main`.
- Increments 7–10 from the original roadmap (knowledge graph, offline
  outbox/conflict handling, export/deletion, production hardening) have not
  been started.
- The visual redesign (calm editorial minimalism, Headway-inspired, CSS
  design-token approach, whole-app scope) shipped and is merged to `main`
  (commits `912a409`, `f32b4ec`, 2026-08-01) — see Phase 8 below. This
  predates this document and was only reconciled into it after the fact.

## Part 2 — Roadmap

### Phase 0 — Housekeeping (do this before anything else)

Goal: get `main` into a state where "current state" claims in this doc and
future work are actually trustworthy.

- Confirm and delete the stale `codex/fix-today-replacement-weekly-teaser`
  branch (diff it against `main` first to be certain nothing unique is on
  it).
- Actually run `npm run typecheck`, `npm run lint`, `npx vitest run`, and
  `npm run build` against `main`'s current `apps/web` (with a real Node
  toolchain this time) to close the Slice D verification gap. Fix anything
  that surfaces — don't assume the hand-reviewed backend work and the
  frontend chat UI are both clean just because they merged.
- Run `./scripts/test-backend` and `./scripts/verify-architecture` against
  current `main` as a final sanity check across everything merged since the
  last time those were confirmed to pass.

This phase has no product scope of its own — it's the "make sure the ground
is solid" pass before building more on top of it.

### Phase 6 — Contextual memory proposals — SHIPPED (2026-08-02, local, unmerged)

Goal: close the gap identified in this session's Memory review — a
fully-governed memory lifecycle with no contextual entry point.

Resolution: implemented directly on the local working copy per explicit
instruction (no branch/PR for this phase). AI-derived source, triggered as a
second, distinct card right after a reflection saves — alongside, not merged
with, the existing wisdom-capture card. `AiAssistantPort.proposeMemory` added
across all three adapters (ADR-018); `MemoryProposalService`/
`MemoryProposalController` extended with a non-persisting
`POST /api/v1/memory/proposals/draft` endpoint; the existing create/review
lifecycle is otherwise unchanged. Frontend, tests, and docs (traceability
matrix, dev log, this roadmap) are all updated — see the 2026-08-02 dev log
entry for full detail, including a latent test-isolation bug (localStorage
not cleared between Vitest tests) fixed along the way.

Not yet done: this work has not been committed, pushed, or verified against
the backend test suite (`./scripts/test-backend`/`./scripts/verify-architecture`)
— this sandbox only had JDK 11 available. Frontend typecheck/lint/vitest/build
all passed (build verified via a scratch `--outDir` due to an unrelated sandbox
file-permission issue on `apps/web/dist/`). Before this phase is truly closed:
run the backend verification scripts on a machine with JDK 21, then commit and
open a PR following this repo's normal workflow.

Original scope (for reference):
- Add a contextual trigger that proposes a candidate memory statement at a
  natural moment — the two strongest candidates are (a) right after a
  reflection saves (parallel to the existing wisdom-capture card, but for
  memory), and (b) right after a weekly retrospective is generated, since
  that's already a synthesized narrative.
- Decide whether this trigger is AI-derived (using `AiAssistantPort`, source
  kind `AI_DERIVED`) or a deterministic prefill like wisdom capture — this
  needs the same kind of explicit user decision Phase 5 slice D got via
  `AskUserQuestion` before implementation, not an assumption.
- Whichever surface it lands on, it must still route through the existing,
  unmodified `MemoryProposalService`/`MemoryProposalController` — nothing
  about the propose → review → accept lifecycle (ADR-008) changes; only a new
  caller populating the "propose" step contextually.
- Update `docs/requirements/traceability-matrix.md` (HELIX-FR-017) and the
  ADR-008 reference to reflect the new caller.

Open questions to resolve before implementation:
- Is this AI-assisted or deterministic? (Affects whether it needs its own
  ADR amendment like ADR-016/017 did.)
- Does it compete with or complement the existing wisdom-capture card on
  Today — should a single reflection ever propose both a wisdom entry and a
  memory, or are these meant to stay distinct concepts the user chooses
  between?

### Phase 7 — `CurrentFocus` projection + server-persisted onboarding — SHIPPED (2026-08-02, local, unmerged)

Goal: close two long-carried-forward gaps from the original Phase 2 plan that
have been deferred through every subsequent phase.

Resolution: implemented directly on the local working copy per explicit
instruction (no branch/PR, same as Phase 6). New `GET /api/v1/current-focus`
endpoint (a new endpoint, not an expanded `/today` — `/today` and
`/transformations` are both left unchanged for their other existing callers);
3-state onboarding (`NOT_STARTED` -> `FIRST_TRANSFORMATION_CREATED` ->
`COMPLETE`), mapped onto the two gates Today's UI already had rather than
inventing new milestones — `COMPLETE` is reached the moment the first
experiment is ever created. Onboarding state is a singleton row (Helix is
single-user, no auth yet per ADR-013) advanced automatically from
`TransformationService`/`ExperimentService`, monotonic, no explicit "finish
onboarding" action. See the 2026-08-02 dev log entry for full detail.

Not yet done: not committed, pushed, or verified against the backend test
suite — same JDK 21 gap as Phase 6, compounded this time by `./gradlew` also
being unable to download the Gradle distribution itself without network
access to `services.gradle.org`. Frontend typecheck/lint/vitest/build all
passed. Before this phase is truly closed: get a JDK 21 environment, run
`./scripts/test-backend` and `./scripts/verify-architecture`, then commit and
open a PR (this and Phase 6 should probably be committed together, in the
order they were built, given Phase 7 has no dependency on Phase 6 but both
are sitting uncommitted in the same working copy right now).

Original scope (for reference):
- A single backend projection endpoint (e.g. `GET /api/v1/current-focus` or
  folded into an expanded `/api/v1/today`) that returns everything Today
  currently assembles from two separate calls (`/today` + `/transformations`)
  in one response — reduces round trips and removes the "has zero
  transformations" special-casing currently done client-side.
- A real, server-persisted onboarding-state field (distinguishing e.g.
  "never started," "created first transformation," "completed guided setup")
  replacing the purely-derived `transformations.length === 0` welcome-state
  check — this was explicitly named in Phase 1's own "open decision" section
  as deferred to Phase 2, and has stayed deferred through every phase since.

### Phase 8 — Visual redesign — SHIPPED (2026-08-01, merged to main)

Goal: execute the calm-editorial-minimalism redesign already scoped in
detail in this session (Headway-inspired direction, CSS custom-property
design-token system, no new framework dependency, whole-app scope, hard
constraint that no component logic/API calls/accessible names change).

Resolution: implemented and merged via commits `912a409` ("visual redesign")
and `f32b4ec` ("Visual redesign: calm editorial design system") on
2026-08-01 — both on `main`, pushed to `origin/main`, predating this
document. `apps/web/src/styles/main.css` now defines the full token system:
warm parchment canvas (`--canvas: #f7f3ec`), warm near-black ink
(`--ink: #26221d`), a single rust/terracotta accent (`--accent: #b5502f`),
a distinct provenance/AI-badge color (`--provenance: #3e5c46`), radii up to
1.25rem, soft diffuse card shadows, and a real type scale
(`--text-sm` through `--text-2xl`, weighted 550/750). Page components
(`TodayPage.tsx`, `AppLayout.tsx`, etc.) were confirmed unchanged at the
logic level — they still reference the same plain class names (`card`,
`cta-button`, `secondary-button`, `nav-link`, `ai-badge`) the tokens now
style, consistent with the hard constraint that this stay a pure styling
layer. Playwright screenshot/accessibility regression artifacts were
committed alongside as evidence of the before/after verification pass.

Not tracked here until now: this shipped before `2026-08-02-roadmap.md`
existed and was never logged in `docs/roadmap/development-log.md` — no
dev-log entry exists for the redesign session itself. Worth backfilling if
the full history matters, but not required for the roadmap to be accurate
going forward.

Original scope (for reference): token system, palette/type/spacing/radius
scale, AI-provenance badge treatment, full page coverage, before/after
verification against the existing test suite.

### Phase 9 — Data export and deletion (closes ADR-015) — SHIPPED (2026-08-02, local, unmerged)

Goal: implement what ADR-015 already decided but nothing has built yet.

Resolution: implemented directly on the local working copy (no branch/PR,
same as Phases 6 and 7). The open question was resolved via `AskUserQuestion`
before implementation, documented in new ADR-019: **hard delete** (no
tombstones — a deleted record's revision history goes with it, since that
history's only purpose was serving the same user who just asked to delete
everything) and **whole-app scope** (`DELETE /api/v1/data` deletes every
record in every module — there's no per-user boundary to delete a subset of
yet, since ADR-013 still defers auth). `GET /api/v1/data/export` produces a
complete JSON bundle of every user-owned record, explicitly excluding the
semantic search index (a derived/regenerable cache, not authored content).
The frontend's real safeguard against accidental deletion is a type-`DELETE`-
to-confirm input on the `/settings/export` page (previously an unreachable
placeholder — now linked from the nav); the backend's `confirm: true` body
requirement is a secondary guard against bare no-body calls, not real
security. See the 2026-08-02 dev log entry and ADR-019 for full reasoning,
including an explicit reconsideration trigger: this must be re-scoped the
moment auth actually ships.

Not yet done: not committed, pushed, or verified against the backend test
suite — same JDK 21 / `./gradlew` gap as Phases 6 and 7, now three phases
deep. Frontend typecheck/lint/vitest/build all passed.

Original scope (for reference): a data export endpoint producing a complete
user-owned dump across every module, and a deletion flow with an open
decision on hard-delete vs. soft-delete/tombstone and how that interacts with
revision history.

### Phase 10 — Offline resilience (Increment 8, narrowed by ADR-012/017)

Goal: address the original Increment 8 scope ("offline outbox and conflict
handling"), but re-scoped against where the app actually is now — most
notably, ADR-017 already narrowed offline guarantees for reflection *chat*
specifically (composing is bufferable, sending/finishing requires network).

Scope:
- Decide, explicitly, which write operations across the app are expected to
  work offline at all (today only reflection-chat-draft text and the wisdom
  draft are locally buffered) versus which are allowed to simply fail
  cleanly with a clear "you're offline" message.
- If a genuine offline write-queue ("outbox") is still wanted for some
  subset of operations (e.g. queuing a reflection save itself, not just the
  in-progress chat text), design conflict handling for it — what happens if
  the same experiment gets a competing edit from another device before the
  queued write flushes.
- This phase should start with a decision doc, not code — the scope has
  visibly narrowed since Increment 8 was first written (this app didn't have
  a conversational, network-dependent reflection flow back then), and a
  fresh scoping pass is warranted before committing to an outbox design.

### Phase 11 — Knowledge graph (Increment 7)

Goal: the original Increment 7 scope — Knowledge currently renders beliefs
and evidence as a flat list/detail view (confirmed: no graph library, no
graph data model, no visualization anywhere in `apps/web` or `apps/api`
under the knowledge/beliefs modules).

Scope: needs its own product-level scoping pass before implementation — "a
knowledge graph" is underspecified until decisions are made on: what the
nodes/edges actually represent (belief↔evidence↔transformation
relationships? something richer?), whether this is a genuine graph data
model change on the backend or a client-side visualization over existing
relational data, and what interaction model it needs (read-only exploration
vs. editing). Treat this as the lowest-priority, highest-ambiguity phase in
this roadmap — don't start implementation without a dedicated scoping
session first.

### Phase 12 — Production deployment & operations hardening (Increment 10)

Goal: the original Increment 10 scope. Still entirely local-dev-only today
(Docker Compose Postgres, `./scripts/dev-api`/`dev-web`); no deployment
target, secrets management, observability, or ops hardening exists anywhere
in the repo.

Scope: this is the one phase in this roadmap that's a business/infra
decision as much as an engineering one — needs a decision on hosting target,
auth strategy (ADR-013 explicitly defers auth "behind a port" — that port
still has nothing plugged into it), secrets management, and what
"production" even means for this app (single-user personal tool vs.
multi-tenant SaaS) before any implementation scope can be written. Treat as
blocked on a product decision, not an engineering task ready to pick up.

## Part 3 — Suggested sequencing

1. **Phase 0** (housekeeping) — always first, low effort, unblocks trust in
   everything after it.
2. **Phase 6** (memory) and **Phase 7** (`CurrentFocus`/onboarding) — can run
   in parallel, both are well-scoped, moderate-risk, backend+frontend work
   with no unresolved product-level ambiguity beyond the two open questions
   named above.
3. **Phase 8** (visual redesign) — ready to hand off as-is once Phase 0's
   verification baseline is trustworthy.
4. **Phase 9** (export/deletion) — ready once the hard-delete-vs-tombstone
   question is answered; otherwise this is the next thing worth a scoping
   session.
5. **Phase 10** (offline resilience) — needs its own scoping pass before
   implementation; do that scoping pass whenever capacity allows, don't block
   on it.
6. **Phase 11** (knowledge graph) and **Phase 12** (production hardening) —
   both need dedicated product-decision passes before they're actionable;
   lowest priority of everything listed here unless the product direction
   changes (e.g. a decision to actually ship this beyond local dev, which
   would immediately promote Phase 12).
