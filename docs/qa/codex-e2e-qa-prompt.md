# QA Prompt: End-to-End Verification of the Knowledge Graph Feature (Phase 11, subphases 11A–11F)

Copy everything below this line into Codex as its task prompt.

---

## Your role

You are a QA engineer doing an independent, adversarial end-to-end verification pass on a feature that was just implemented but **never compiled or run** by the engineer who built it (their sandbox couldn't run a JDK 21/Gradle build). Your job is to actually run this application — backend, frontend, and database — exercise it for real through a browser, hit the API directly, and produce a QA report that tells the team whether this is safe to merge. Assume nothing works until you've seen it work. When something breaks, don't just note the symptom — open the relevant logs and trace it to a root cause before writing it up.

## What changed (context, don't just trust this — verify it)

This is "Helix," a personal-growth app: React 19/TypeScript/Vite/TanStack Router+Query frontend (`apps/web`), Java 21/Spring Boot 4 backend (`apps/api`), PostgreSQL + Flyway. The session under test added a full "Personal Knowledge Graph" feature (Phase 11, subphases 11A–11F) on top of an already-working app with these existing modules: transformations, experiments, reflections, beliefs, evidence, wisdom, memory proposals, data export.

The knowledge graph is a **read-only relational projection** over those existing modules — it derives nodes/edges from existing data, never writes back to it. Specifics:

- **New backend module** (`apps/api/src/main/java/com/helix/api/knowledge/`): a new migration (`V11__knowledge_graph.sql`) adding `knowledge_node`, `knowledge_edge`, `knowledge_edge_source`, `knowledge_projection_checkpoint` tables; a projection service that rebuilds the graph from scratch on demand; a bounded query service (max 25 nodes, 2-hop depth from a focus node); a governance service for confirm/reject/hide actions on proposed edges; an AI-assisted relationship-discovery service (compares belief pairs, proposes connections an AI judges related); and a REST controller.
- **New REST endpoints**, all under `/api/v1/knowledge-graph`:
  - `POST /rebuild` — rebuild the entire graph projection from current domain data
  - `GET /status` — projection freshness/checkpoint info per source module
  - `GET /transformation/{transformationId}` — bounded graph view centered on a transformation
  - `GET /belief/{beliefId}` — bounded graph view centered on a belief
  - `GET /focus/{nodeType}/{sourceRecordId}` — generic bounded view (`nodeType` ∈ `TRANSFORMATION, EXPERIMENT, REFLECTION, BELIEF, EVIDENCE, WISDOM, MEMORY`)
  - `POST /discover-relationships` — triggers AI-assisted belief-relationship discovery (capped at 25 pairs/run)
  - `POST /edges/{edgeId}/confirm`, `POST /edges/{edgeId}/reject`, `POST /edges/{edgeId}/hide` — governance actions on a proposed edge
- **New frontend route**: `/knowledge-graph/$nodeType/$sourceRecordId` → `KnowledgeGraphPage` (`apps/web/src/pages/KnowledgeGraphPage.tsx`). Renders a custom SVG radial diagram of the bounded graph view, a toggle to an accessible structured-list view, a node-type filter, a node detail panel, loading/error/empty states, a "Refresh connections" (rebuild) button, and a "Check for new connections" (AI discovery) button. Governance actions (Confirm/Reject/Hide) appear in the list view only for edges with `status: PROPOSED`.
- **Two new entry points** into that page:
  1. On a transformation's detail page (`apps/web/src/pages/TransformationDetailPage.tsx`), a "Explore connections" button in the transformation summary card.
  2. On a belief's detail panel inside the Knowledge page (`apps/web/src/pages/KnowledgePage.tsx`), a "Explore connections" button.
- **Also touched**: `packages/contracts/src/index.ts` (new shared TypeScript types), `apps/web/src/api/http.ts` (new API client methods), `apps/web/src/app/router.tsx` (new route registration), and — critically — the `AiAssistantPort` interface plus all three of its implementations (`OpenAiAssistantAdapter`, `OllamaAssistantAdapter`, `NoAiAssistantAdapter`) gained a new method (`proposeBeliefRelationship`).

**Known risk areas the engineer flagged themselves** (verify these specifically, don't take their word for it):
1. The backend Java code has **never been compiled** — there could be compile errors, wrong method signatures, wrong JPA mappings, or migration SQL that doesn't actually apply cleanly.
2. The two live AI adapter methods (`OpenAiAssistantAdapter.proposeBeliefRelationship`, `OllamaAssistantAdapter.proposeBeliefRelationship`) have no dedicated unit test — only structural review.
3. Frontend was typechecked/linted/unit-tested (all passing per the engineer), but **never manually driven in a real browser**.
4. A production `vite build` was never successfully completed (blocked by an unrelated sandbox file-permission issue) — confirm a clean build actually works in your environment.

## Your environment setup

Follow `docs/running-app.md` at the repo root for the authoritative guide. Summary:

```bash
cd <repo-root>
./scripts/bootstrap                                        # npm install for apps/web
cp .env.example .env                                        # review/edit as needed
docker compose -f infra/local/docker-compose.yml up -d      # Postgres on localhost:5432 (db/user/pass: helix/helix/helix)
./scripts/dev-api    # terminal 1 — Spring Boot on :8080, runs Flyway migrations on startup, logs to this terminal's stdout
./scripts/dev-web    # terminal 2 — Vite dev server on :5173
```

Open the app at `http://localhost:5173/today`.

**Before touching the feature**, confirm the baseline is healthy:
- `./scripts/dev-api` starts without exceptions and without Flyway migration failures (watch specifically for `V11__knowledge_graph.sql` applying cleanly — if it fails, the whole app may fail to start).
- `./scripts/dev-web` starts without errors.
- The existing app (Today, Transformations, Knowledge, Wisdom, Library, Memory, Data export pages) loads and works — you need a clean baseline before you can trust any diagnosis of a knowledge-graph-specific bug.

You will also want to seed some real data to test against (create at least: 2 transformations, 2+ beliefs per transformation with evidence and revisions, 1+ experiment with a reflection, at least one weekly retrospective/wisdom entry so a `WISDOM` node exists, and a confirmed memory proposal) — the knowledge graph has nothing interesting to show against an empty database. Use the running app's UI to create this data rather than inserting it directly, so you're also incidentally regression-testing the existing flows.

## Test methodology

Use **three tools together** for every scenario, not just one:

1. **A real browser.** Drive the actual UI — click buttons, fill forms, navigate via the router, resize the viewport to check responsiveness, use keyboard navigation to spot-check accessibility. Keep the browser DevTools open (Console + Network tabs) for the entire session. If your environment doesn't give you a way to reliably drive a real browser (Playwright, Claude in Chrome, or equivalent), stop and say so explicitly in your report rather than silently falling back to curl-only testing — that would be a materially weaker verification pass and the report must not imply full E2E coverage if it wasn't done.
2. **Direct API calls** (`curl` or similar) against `http://localhost:8080/api/v1/knowledge-graph/...` — to test the backend in isolation from the frontend, check exact response shapes, and probe edge cases the UI doesn't have controls for (e.g. calling `/focus/{nodeType}/{sourceRecordId}` with a `nodeType` the UI never sends, hitting `/rebuild` concurrently, calling governance actions on a non-existent edge ID).
3. **Log inspection on every failure.** This is required, not optional:
   - **Backend errors**: when an API call returns a 4xx/5xx you didn't expect, or the browser shows a failed request, go to the terminal running `./scripts/dev-api` and capture the full stack trace at the time of the request (match by timestamp). Report the exception type, message, and the top few frames of the stack trace — don't just say "got a 500."
   - **Frontend errors**: check the browser console for uncaught exceptions, React errors, or failed network requests (status code, response body) in the Network tab. Check for hydration/render warnings too, not just hard errors.
   - **Database state**: if something looks wrong in the graph (missing nodes, wrong edges, stale data), connect to Postgres directly (`docker exec -it helix-postgres psql -U helix -d helix`) and inspect `knowledge_node`, `knowledge_edge`, `knowledge_edge_source`, `knowledge_projection_checkpoint` to see whether the projection actually reflects the domain tables correctly. This is how you distinguish a backend logic bug from a frontend rendering bug — check both ends before concluding which layer is at fault.
   - When you find a bug, always answer: *what request/action triggered it, what did the log actually say, what layer is responsible (migration/entity/repository/service/controller/frontend), and what's the smallest repro.*

## Test plan

Work through these in order. For each, record pass/fail and evidence (screenshot description, response body, log excerpt).

### 1. Startup and migration integrity
- [ ] Fresh `docker compose up -d` + `./scripts/dev-api` from a clean/reset database (`./scripts/reset-local-data` if that resets the DB, or drop and recreate the `helix` database) applies all migrations including `V11__knowledge_graph.sql` with no errors.
- [ ] Confirm via psql that all four new tables exist with the expected columns and indexes.
- [ ] Confirm the app doesn't crash if `/rebuild` is called with all four knowledge tables already empty (first-run case).

### 2. Backend API — happy path
- [ ] `POST /rebuild` against a database with seeded data (see setup above) returns `{nodeCount, edgeCount, rebuiltAt}` with non-zero counts. Sanity check the counts roughly match what you'd expect from the seed data (e.g. if you created 2 transformations, 4 beliefs, expect at least 6 nodes just from those).
- [ ] `GET /status` returns checkpoints for all 7 source modules (transformations, experiments, reflections, evidence, beliefs, wisdom, memory) with recent `lastProjectedAt` timestamps.
- [ ] `GET /transformation/{id}` for a real transformation ID returns a `GraphView` with the transformation as `focusNodeId`, plausible connected nodes/edges, correct `displayLabel` per edge type (spot check a few against the mapping table in `KnowledgeGraphController.DISPLAY_LABELS`), and every node has a `sourceRoute` that's a real, working frontend path.
- [ ] `GET /belief/{id}` similarly for a real belief ID.
- [ ] `GET /focus/{nodeType}/{sourceRecordId}` for each of the 7 node types with a real ID.
- [ ] Confirm `truncated: true` appears when a focus node genuinely has more than 25 connected nodes within 2 hops (you may need to seed extra data to trigger this, or verify the cap logic by code inspection plus a targeted smaller-scale test — note which approach you used).

### 3. Backend API — error handling and edge cases
- [ ] `GET /focus/BELIEF/{a-random-uuid-that-does-not-exist}` — expect a 404 with an actionable error message (per the code, it should mention rebuilding the projection). Confirm the actual response matches.
- [ ] `GET /focus/{invalid-node-type}/{some-uuid}` (e.g. `GET /focus/NOT_A_TYPE/...`) — confirm this fails gracefully (400, not a 500 with a stack trace leaking to the client). Check backend logs for what actually happened server-side.
- [ ] `POST /edges/{a-random-uuid-that-does-not-exist}/confirm` (and `/reject`, `/hide`) — expect 404 with actionable message.
- [ ] Call `POST /rebuild` twice in a row — confirm the second call doesn't error and node/edge ids change (rebuild wipes and regenerates — ids are NOT stable across rebuilds, only `(nodeType, sourceRecordId)` is) while `(nodeType, sourceRecordId)` pairs remain stable in content.
- [ ] Delete a transformation (if the app supports deletion) or otherwise make a domain record referenced by the graph disappear, then call `/rebuild` — confirm no crash, and the orphaned node/edges are cleanly removed from the projection.

### 4. AI-assisted relationship discovery (11E) — this is the highest-risk area, spend real time here
- [ ] Check what AI provider is configured in your `.env` (`HELIX_AI_PROVIDER` or similar — check `docs/running-app.md` / `application.properties` for the exact variable name). Test with whatever is realistically configured in your environment; if no AI provider is configured, you'll exercise the `NoAiAssistantAdapter` path — that's still worth testing (confirm it returns `related: false` gracefully and doesn't error) but note in your report that the OpenAI/Ollama code paths went untested if you couldn't reach a real provider.
- [ ] If you have API access to a real provider: seed at least 3-4 beliefs across different transformations with plausible thematic overlap between some pairs and none between others. `POST /discover-relationships`, confirm the response's `pairsEvaluated`/`proposalsCreated` counts are sane, then `GET /focus/BELIEF/{id}` for one of the beliefs involved and confirm a `BELIEF_RELATED_TO_BELIEF` edge appears with `status: PROPOSED`, `origin: AI_PROPOSED`, `confidence: MODERATE`, and a non-empty `explanation`.
- [ ] Call `/discover-relationships` a second time immediately after — confirm it does NOT re-propose the same pair (the service should skip pairs with any existing edge, confirmed or rejected).
- [ ] `POST /edges/{proposedEdgeId}/reject`, then `/discover-relationships` again — confirm the rejected pair is still not re-proposed (this is a specific claim in the code/docs — verify it, don't assume).
- [ ] `POST /edges/{proposedEdgeId}/confirm` on a different proposed edge — confirm its `status` becomes `CONFIRMED` and it now appears in the bounded graph view the same as any other confirmed edge (the query service should only include `CONFIRMED` edges — verify a `PROPOSED` edge does NOT appear in `/focus/...` results before it's confirmed).
- [ ] If you can seed 8+ beliefs with no existing relationships, confirm the 25-pair cap actually stops the run where expected rather than evaluating all pairs (8 beliefs = 28 possible pairs).

### 5. Frontend — browser walkthrough
- [ ] From `/today`, navigate to a transformation's detail page and click "Explore connections." Confirm it lands on `/knowledge-graph/TRANSFORMATION/{id}` and the page loads without console errors.
- [ ] Confirm the diagram view renders: focus node visually distinct (larger/different color) from neighbors, lines connecting related nodes, labels legible, no nodes overlapping unreadably for a small graph.
- [ ] Toggle to List view — confirm every edge shows source/relationship/target in plain language, an explanation where present, and a history line (created/confirmed/rejected dates).
- [ ] Use the type filter checkboxes — confirm unchecking a type actually removes those nodes AND any edges touching them from both diagram and list views, and re-checking restores them.
- [ ] Click a node in the diagram — confirm the node detail panel appears below with the right label/summary/status, and its "View full record" link actually navigates to the correct existing page for that record (transformation/experiment/reflection page, or Knowledge/Wisdom/Memory page for the others) and that page shows the expected record.
- [ ] Click "Refresh connections" — confirm a loading state shows, then the view updates (check Network tab that it actually called `POST /rebuild` then re-fetched the view).
- [ ] Click "Check for new connections" — confirm a status message appears reporting pairs checked / connections found, matching what you saw hitting the API directly in step 4.
- [ ] If any edge is `PROPOSED` (from step 4), confirm Confirm/Reject/Hide buttons appear in the list view for that edge, and clicking each does the right thing (calls the right endpoint, updates the view, edge disappears from view after Hide/Reject, edge treated as a normal confirmed connection after Confirm).
- [ ] Repeat the "Explore connections" walkthrough starting from a belief in the Knowledge page.
- [ ] Test the empty/error states deliberately: navigate directly to `/knowledge-graph/BELIEF/00000000-0000-0000-0000-000000000000` (a UUID that doesn't exist) — confirm a sensible error state with a "Build connections" recovery action, not a blank page or unhandled crash.
- [ ] Test a focus node with genuinely zero connections (a brand-new belief with no evidence/experiments linked) — confirm the "No connections yet" empty state shows correctly rather than an empty diagram or a crash.
- [ ] Resize the browser to a mobile-width viewport (e.g. 375px) and repeat the diagram/list toggle — confirm the list view is usable at that width (this was explicitly the "mobile experience" answer chosen during scoping — verify it holds up).
- [ ] Basic keyboard-only pass: tab through the type filter checkboxes, the view-mode toggle, and the action buttons — confirm everything is reachable and operable without a mouse, and confirm the diagram SVG's `aria-label` is read sensibly by inspecting the accessibility tree (or a screen reader if available).

### 6. Regression check on existing features
Spend at least a light pass confirming nothing in the pre-existing app broke: creating a transformation, adding an experiment, submitting a reflection, adding/revising a belief, adding evidence, generating a weekly retrospective, creating/reviewing a memory proposal, and the data export page. You're specifically watching for anything that touches modules the knowledge graph reads from (transformation, experiment, reflection, belief, evidence, wisdom, memory) — a subtle regression there is exactly the kind of thing a read-only-projection feature could accidentally introduce if a shared type or shared code path got touched.

### 7. Production build sanity check
- [ ] Run `cd apps/web && npm run build` in a clean environment and confirm it completes successfully end-to-end (the engineer's own sandbox hit an unrelated file-permission error clearing `dist/` — confirm that doesn't reproduce for you, and that if it does, it's genuinely environmental and not a real build error by inspecting what `tsc -b` and the `vite build` step each individually report).
- [ ] Run `cd apps/api && ./gradlew build` (or `./gradlew test` at minimum) — this is the check that was **never possible** in the original sandbox. This is likely the single most valuable thing you can do: report every compile error, every failing test, verbatim, with file:line references.

## Report format

Produce a markdown QA report with these sections:

1. **Summary** — one paragraph: overall verdict (ship / ship with caveats / do not ship), and the single most important finding.
2. **Environment** — what you actually ran (versions, AI provider configured or not, any deviations from the setup instructions above and why).
3. **Results by test section** (mirroring sections 1–7 above) — for each checklist item: Pass / Fail / Blocked (couldn't test) / Not Applicable, with evidence. Failures must include: the exact request/action that triggered it, the exact error (status code + response body, or exception + stack trace top frames from the backend log, or browser console error text), and your root-cause assessment (which file/layer is responsible, if you can determine it).
4. **Bugs found** — a flat, prioritized list (Critical / High / Medium / Low) independent of the section they came from, each with a one-line repro and a suggested fix location (file path) if you were able to identify one.
5. **Things you could not verify** — be explicit about any test you skipped or couldn't complete (e.g. no AI provider configured, no way to seed 8+ beliefs, no screen reader available) — don't let gaps in coverage get silently absorbed into a passing grade.
6. **Recommendation** — concrete next steps before this merges.

Be skeptical, be specific, and never report "works" without having actually seen it work in this session.
