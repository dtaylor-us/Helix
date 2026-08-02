# ADR-020 Implement the Helix knowledge graph as a relational, user-governed projection over authoritative domain records

- Status: Accepted (2026-08-02) — sign-off given via the explicit instruction to
  implement all knowledge graph phases, which supersedes the standalone
  product-owner-approval gate originally recorded here (see
  `docs/product/knowledge-graph-scoping.md`)
- Date: 2026-08-02

## Context
Phase 11 (the knowledge graph) is scoped as an advanced exploratory capability that helps a user see
relationships across transformations, beliefs, experiments, evidence, reflections, wisdom, and
memory that are otherwise only visible one record at a time. A detailed product brief (supplied by
the user) recommends a specific architecture and explicitly forbids beginning implementation before
that architecture is decided in an ADR and the product/domain scoping is complete. The full scoping
work — node catalog, edge catalog mapped against Helix's actual schema, provenance model, governance
model, data-volume estimate, and answers to 20 required scoping questions — lives in
`docs/product/knowledge-graph-scoping.md`. This ADR records only the architecture decision itself.

## Decision
1. **Relational graph projection in PostgreSQL.** New tables (`knowledge_node`, `knowledge_edge`,
   `knowledge_projection_checkpoint`, `knowledge_edge_source`) modeled after the brief's suggested
   schema, added via the existing Flyway migration sequence — no new datastore technology.
2. **No dedicated graph database for the initial implementation** (no Neo4j, Neptune, Cosmos DB
   Gremlin, etc.). Single-user scale, bounded-depth traversal (one to two hops per the scoping doc),
   and a data-volume estimate that stays in the low thousands of rows per table even at a 10-year
   horizon (scoping doc Section 11) make PostgreSQL clearly sufficient.
3. **Domain modules remain authoritative.** The Knowledge Graph module never owns transformations,
   beliefs, experiments, reflections, evidence, wisdom, or memory — it consumes their existing
   repositories/services and builds a projection. No graph edge ever writes back to a domain record.
4. **A new `com.helix.api.knowledge` (name TBD at 11B start) module owns projection and graph
   queries only.** Follows this codebase's established hexagonal layout
   (`domain`/`adapter.out.persistence`/`application`/`adapter.in.http`), same as every other module
   added in this project (memory, onboarding, data).
5. **Explicit and derived relationships are distinguished from proposed ones**, via the `origin` and
   `status` fields on every edge (scoping doc Section 5). The first release ships **zero AI-proposed
   edges** — every edge is either `EXPLICIT_DOMAIN_RELATIONSHIP` (a direct foreign key) or
   `DETERMINISTIC_DERIVATION` (a traceable join across explicit relationships), both auto-confirmed.
   `AI_PROPOSED` and its confirm/reject/hide governance UI are deferred to Phase 11E, since building
   governance controls with nothing to govern would be untestable, unused UI.
6. **Graph views are bounded and contextual, never a full-graph load.** Every query is scoped to a
   focus node with a node-count budget (25 by default, scoping doc Section 11) and one-to-two-hop
   depth (Section 12, question 10).
7. **An accessible, non-visual structured-list view ships alongside the graph from the start**, not
   as a later addition — required by the brief and made easier in the first release specifically
   because every edge is pre-confirmed (no proposed/rejected states to represent yet).
8. **Projection is rebuildable from authoritative data.** A manual rebuild endpoint (mirroring the
   existing `POST /api/v1/search/index/rebuild` pattern) ships first; scheduled reconciliation is a
   later recovery mechanism, not a launch requirement.
9. **Temporal metadata (`effectiveFrom`/`effectiveTo`/`supersededByEdgeId`) is retained in the schema
   from day one**, even though the first release doesn't populate or expose most of it — cheaper to
   include the columns now than to migrate them in later, and the brief explicitly warns against
   discarding this data just because the first visualization is static.

## Alternatives
- A dedicated graph database (Neo4j, etc.). Rejected for the initial implementation: no measured
  need exists yet (single user, bounded traversal depth, low row counts — scoping doc Section 11),
  and it would add a second datastore technology this project has no other reason to operate.
  Explicitly left open as a future reconsideration if traversal needs or scale materially change
  (see Reconsideration Triggers).
- Making the Knowledge Graph module itself authoritative for relationships (i.e., letting confirmed
  graph edges become the source of truth). Rejected: this is precisely the "duplicate authority" risk
  the brief calls out, and conflicts with this project's standing pattern (established across every
  prior phase) of AI/derived surfaces being proposal-only until routed through an existing,
  unmodified domain workflow (ADR-008).
- Shipping AI-proposed relationships (`BELIEF_RELATED_TO_BELIEF`, cross-reflection pattern detection,
  etc.) in the first release alongside explicit/deterministic ones. Rejected: the brief is explicit
  that "the initial graph should work using only explicit domain relationships and deterministic
  projections" and that AI relationship discovery is a later subphase (11E) — doing otherwise would
  also mean building confirm/reject governance UI in the same pass as the visualization itself,
  increasing scope and risk together rather than sequencing them.
- Including `Value` and `Growth Dimension` node types as specified in the brief. Rejected for now:
  neither concept exists anywhere in Helix's domain model or product docs today (verified directly
  against the schema and `docs/product/*.md`). The brief's own conditional — "growth dimensions
  should be included only if their use and meaning have been sufficiently defined elsewhere in
  Helix" — isn't met. Introducing them here would mean inventing a taxonomy as a side effect of
  graph scoping, which is a distinct product decision this ADR shouldn't make by default.

## Consequences
- Phase 11B (projection foundation) can begin once this ADR and the scoping doc are approved, with a
  concrete, schema-verified edge catalog instead of the brief's more abstract proposal — reduces the
  risk of building projection logic for relationships Helix's data can't actually support.
- The first release genuinely needs no AI dependency, consistent with HELIX-BR-001 ("AI suggestions
  must not be mandatory for core workflow") and this project's broader pattern of AI-optional
  surfaces.
- Two node types and roughly a third of the brief's proposed edge types are explicitly deferred
  (scoping doc Sections 3-4) rather than attempted with weak or invented signals — narrows the first
  release's scope but keeps every shipped relationship honest about what it actually represents.
- Belief and wisdom revision history is represented as node-detail-panel content, not as graph edges
  between distinct nodes — a deliberate modeling choice that may need revisiting in Phase 11F
  (temporal exploration) once the product has a clearer need for revision-as-edge.

## Risks
- Deferring governance UI to land alongside Phase 11E (rather than as its own standalone 11D effort,
  per the brief's original subphase ordering) means Phase 11D and 11E are now more tightly coupled
  than the brief specified. If AI-assisted discovery (11E) is deprioritized or delayed indefinitely,
  the governance UI never ships either — acceptable, since there's genuinely nothing to govern until
  11E exists, but worth surfacing explicitly rather than silently reordering the brief's phases.
- The graph library selection (scoping doc, question 15) and mobile experience decision (question
  14) are both left open, to be resolved at the start of Phase 11C rather than now. This ADR does not
  block on them, but Phase 11C shouldn't start coding against an unevaluated library choice.

## Reconsideration Triggers
- Measured traversal needs exceed what bounded-depth PostgreSQL queries can serve performantly (the
  brief's own trigger, retained here).
- Graph volume or query depth materially exceeds the Section 11 estimates (e.g., multi-user/social
  graph use cases, which are explicitly out of scope per the brief's Section 16).
- Temporal graph analysis (Phase 11F) becomes a core, frequently-used capability rather than a nice-
  to-have — may justify revisiting the "no graph database" decision if temporal traversal patterns
  turn out to be poorly suited to relational modeling in practice.
- A real, observed product need emerges for `USER_CREATED` edges or for `Value`/`Growth Dimension`
  node types — either would need its own scoping pass, not a retrofit into this ADR.

## Related Requirements
None yet formally cataloged. `docs/product/knowledge-graph-scoping.md` Section 2 lists the user
questions this capability answers; a HELIX-FR-XXX entry should be added to
`docs/requirements/requirements-catalog.md` when Phase 11B ships an actual endpoint, not before —
per this project's standing rule against adding traceability rows for capabilities that don't exist
yet.
