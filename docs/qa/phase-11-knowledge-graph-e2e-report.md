# Phase 11 Knowledge Graph End-to-End QA Report

Date: 2026-08-01 (America/Chicago)

## 1. Summary

**Verdict: do not ship until relationship direction and record-link correctness are fixed.** The application compiles, all backend tests pass, the frontend production build succeeds, Flyway V11 applies cleanly to a fresh PostgreSQL database, and the main graph/API workflows run. However, several projected edges have source/target ordering that contradicts their relationship type, producing materially false sentences in the accessible list view. In addition, graph links for belief/evidence nodes open `/knowledge` without selecting the referenced record and can display a different record.

## 2. Environment

- macOS host, Java Temurin 21.0.10, Gradle 9.5.1 wrapper.
- PostgreSQL 16.14 in Docker Engine 29.4.3; volume deleted and recreated before startup.
- Spring Boot 4.0.0 API on `localhost:8080`.
- Frontend built and served using the Codex-bundled Node runtime because `node`, `npm`, and `npx` were not on the shell PATH. Vite 8.1.5 served on `localhost:5173`.
- Real-browser walkthrough used the Codex in-app browser controller backed by Playwright APIs. Console warnings/errors were monitored throughout. Direct `curl` and `psql` checks were also used.
- `HELIX_AI_PROVIDER=none` was explicitly set. OpenAI and Ollama live-provider paths were not exercised.
- Seed data was created through the UI: 2 transformations, 4 beliefs, 1 experiment, 1 reflection, 1 evidence item, 1 belief revision, 1 wisdom entry, and 1 proposed-then-confirmed memory.

## 3. Results by test section

### 1. Startup and migration integrity

- **Pass — fresh migration.** Removed `local_helix-postgres-data`, recreated the container, and started the API. Flyway logged `Successfully validated 11 migrations`, `Migrating ... version "11 - knowledge graph"`, and `Successfully applied 11 migrations ... now at version v11`. API reached `Started HelixApiApplication` with no startup exception.
- **Pass — schema.** `knowledge_node`, `knowledge_edge`, `knowledge_edge_source`, and `knowledge_projection_checkpoint` exist. `information_schema` showed 30 expected columns. `pg_indexes` showed PKs plus node type, unique `(node_type, source_record_id)`, edge endpoint/status, edge-source, and unique checkpoint-module indexes (11 indexes total).
- **Pass — empty first rebuild.** With a fresh empty domain database, `POST /rebuild` returned HTTP 200: `{"nodeCount":0,"edgeCount":0,...}`.

### 2. Backend API — happy path

- **Pass — seeded rebuild.** HTTP 200 with `nodeCount: 10`, `edgeCount: 14`; after confirming a memory, counts became 11 and 15. Database counts matched all seven node types: 2 transformations, 4 beliefs, and one each of experiment, reflection, evidence, wisdom, and memory.
- **Pass — status.** Returned exactly seven current checkpoints: transformations, experiments, reflections, evidence, beliefs, wisdom, memory.
- **Pass — transformation/belief/focus views.** Real focus requests for every seeded node type returned HTTP 200, correct focus IDs, bounded connected nodes, history, explanations, references, and `truncated:false`. Dedicated transformation and belief routes were covered through the UI/API behavior.
- **Pass with defect — display labels.** Labels match `KnowledgeGraphController.DISPLAY_LABELS`, but several edges use reversed source/target endpoints; see bugs KG-1 and KG-2.
- **Pass with defect — source routes.** All nodes provide an existing route, but `/knowledge` does not identify the source record; see KG-3.
- **Blocked — 25-node truncation.** The UI-created graph had only 11 nodes. The cap has unit coverage, but a >25-node live projection was not seeded in this pass.

### 3. Backend API — error handling and edge cases

- **Pass — missing focus.** HTTP 404 Problem Details: `No knowledge graph node found for BELIEF ... -- the projection may need to be rebuilt (POST /api/v1/knowledge-graph/rebuild).`
- **Pass — invalid type.** HTTP 400 Problem Details, no server trace leaked: `No enum constant ... KnowledgeNodeType.NOT_A_TYPE`. Backend log recorded a resolved `MethodArgumentTypeMismatchException` warning.
- **Pass — missing governance edge.** Confirm/reject/hide each returned HTTP 404 with `Knowledge graph edge not found`. Logs recorded resolved `NoSuchElementException` warnings only.
- **Pass — repeat rebuild.** Consecutive rebuilds returned HTTP 200 and stable `(nodeType, sourceRecordId)` pairs/counts while every projection node UUID changed.
- **Pass — simultaneous rebuild probe.** Two concurrent calls both returned HTTP 200 and the same coherent `10/14` summary.
- **Blocked — orphan cleanup after deletion.** No domain record was deleted because the browser flow did not expose a scoped non-destructive delete for the seeded transformation; the only obvious data deletion control deletes everything.

### 4. AI-assisted relationship discovery

- **Pass — no-AI adapter.** With `HELIX_AI_PROVIDER=none`, 4 beliefs produced 6 evaluated pairs and 0 proposals, HTTP 200. Repeating through the UI displayed `Checked 6 pairs of beliefs — nothing new to review.` No exception or console error occurred.
- **Blocked — live OpenAI/Ollama proposals.** No usable provider credential/local Ollama endpoint was available, so AI proposal shape, pair de-duplication after reject, and confirm/reject/hide behavior on real proposed edges were not exercised end to end.
- **Blocked — 25-pair cap.** Only 4 beliefs (6 pairs) were seeded, not 8 beliefs (28 pairs). Unit tests passed but this was not live-verified.

### 5. Frontend browser walkthrough

- **Pass — transformation entry point.** `Explore connections` navigated from the transformation detail page to `/knowledge-graph/TRANSFORMATION/{id}`.
- **Pass — diagram rendering.** Seven-node graph rendered in a 480x480 radial layout; focus radius was 20 with accent fill versus neighbor radius 14 with soft fill. The SVG had the expected accessible label and no console errors.
- **Pass with defect — list view.** Explanations and history render, but several human-readable edge sentences are directionally false (KG-1/KG-2).
- **Pass — filters.** Unchecking Evidence changed the list from 12 to 9 edges, removed the evidence node text, and rechecking restored 12.
- **Pass with defect — node detail.** Clicking an SVG node opened the correct detail panel, but `View full record` for a belief opened a different selected belief (KG-3).
- **Pass — refresh/discovery.** Refresh rebuilt and refetched the view. Discovery reported 6 pairs and 0 findings, matching direct API evidence.
- **Blocked — governance controls.** No `PROPOSED` edge existed under the no-AI adapter.
- **Pass — belief entry point.** The Knowledge page exposes `Explore connections` for the selected belief with the correct graph URL.
- **Pass — missing-record recovery.** After TanStack Query retries (about 10 seconds), the page displayed a sensible error and `Build connections` action. No console exception occurred.
- **Pass — zero-connection behavior.** A belief with no direct evidence still displayed its transformation/sibling-belief neighborhood; no truly isolated projected node was available. The conditional empty-state implementation was therefore not exercised with an isolated live node.
- **Pass — mobile.** At 375x812, list width was 292px, document scroll width 360px (no horizontal overflow), and action buttons stacked to full usable width.
- **Pass with caveat — keyboard/accessibility.** Native toggle buttons and filter checkboxes are keyboard controls, and the structured list is accessible. SVG node groups themselves have no role or `tabindex`; the SVG is intentionally exposed as an image and directs users to List view. No screen reader was available.
- **Pass — browser console.** No React exception, hydration warning, or failed-request console error appeared during successful scenarios.

### 6. Regression checks

- **Pass — live create flows.** Created transformations, experiment, reflection, belief, evidence, revision, wisdom, and governed memory through the UI.
- **Pass — baseline pages.** Today, Journey, Knowledge, Library, Wisdom, Memory, and Data Export loaded with expected data and no console warning/error.
- **Partial — export.** Export page rendered and the existing test suite passed; the download button was not activated because browser download artifact verification was outside the useful graph-focused pass.
- **Partial — weekly retrospective.** Live retrospective content derived from the reflection rendered, but `Save weekly snapshot` was not submitted.

### 7. Production build sanity

- **Pass — frontend.** TypeScript project build followed by Vite production build completed: 162 modules transformed; PWA assets and service worker generated.
- **Pass — backend.** `./gradlew build` completed successfully in 40 seconds. Java main and test sources compiled, all tests passed, and Gradle reported `BUILD SUCCESSFUL` (7 actionable tasks).

## 4. Bugs found

1. **High — KG-1: evidence/belief and experiment/belief edge direction contradicts relationship type.** Repro: rebuild seeded data, open List view. Actual: `A deliberate pause... — challenged by — I can respond...` and `Pause before responding... — tested through — I can respond...`. The explanations state the reverse, correct semantics. Root cause: `KnowledgeGraphProjectionService` creates `BELIEF_*_BY_EVIDENCE` with EVIDENCE as source and BELIEF as target, and `BELIEF_EXPLORED_BY_EXPERIMENT` with EXPERIMENT as source and BELIEF as target. Fix `apps/api/src/main/java/com/helix/api/knowledge/application/KnowledgeGraphProjectionService.java` around the evidence projection block and add endpoint-order assertions.
2. **High — KG-2: wisdom/memory relationship direction is likewise reversed.** Repro: focus confirmed memory. Actual edge is REFLECTION -> MEMORY with type `MEMORY_DERIVED_FROM`, causing list semantics equivalent to “reflection derived from memory.” `WISDOM_EMERGED_FROM_REFLECTION` has the same mismatch. Fix endpoint ordering in the wisdom/memory blocks of `KnowledgeGraphProjectionService.java`, or rename types/labels consistently if the intended canonical direction is producer -> product.
3. **Medium — KG-3: graph `View full record` does not deep-link belief/evidence records.** Repro: click the graph node `I can respond thoughtfully...`, then `View full record`. Browser opens `/knowledge`, which selected `Small consistent sessions create meaningful work.` instead. Root cause: controller emits `/knowledge` for every BELIEF/EVIDENCE node and `KnowledgePage` has no record identifier in route/search state. Fix source-route generation plus Knowledge-page routing/selection, e.g. `/knowledge?beliefId={id}` and evidence-aware selection.
4. **Low — KG-4: missing-focus error takes about 10 seconds to appear.** Repro: navigate to an all-zero belief UUID. The page shows loading while TanStack Query retries a deterministic 404, then shows the recovery action. Configure graph queries not to retry 4xx/not-found responses.

## 5. Things not verified

- Live OpenAI and Ollama adapters, real AI proposals, proposal governance, rejected-pair suppression, and proposal de-duplication.
- Live 25-pair discovery cap and live >25-node truncation.
- Projection cleanup after deleting an individual domain record.
- A genuinely isolated projected focus node.
- Full screen-reader output; accessibility-tree/DOM semantics were inspected instead.
- Data-export download contents and saved weekly retrospective mutation.

## 6. Recommendation

Do not merge yet. Correct the edge endpoint semantics and add projection/controller tests that assert `(source type, relationship type, target type)` for every edge type, not only counts/existence. Add record-addressable Knowledge routes so graph detail links cannot land on an unrelated belief/evidence record. After those fixes, rerun the list-view sentences and source links in a real browser. Before final release, run one provider-backed discovery session (or a deterministic fake-provider integration profile) to exercise proposed-edge governance and the 25-pair cap.
