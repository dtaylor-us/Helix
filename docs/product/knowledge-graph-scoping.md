# Phase 11A — Knowledge Graph Product and Domain Scoping

- Status: Scoping complete. No graph visualization or persistence changes have been made — per
  the phase's own exit criterion, this document and ADR-020 must be reviewed/approved before any
  Phase 11B implementation work begins.
- Date: 2026-08-02
- Input: a detailed product brief supplied by the user, adapted here against Helix's actual current
  domain model (verified directly against the schema and entity code, not assumed).

This document resolves the 20 required scoping questions from the brief, defines the initial node
and edge catalog *as it actually maps to Helix's existing tables*, and sets the governance and
provenance model. ADR-020 records the architecture decision this document leads to.

## 1. Purpose statement

The knowledge graph helps a Helix user see how their growth-journey records connect to each other —
which experiments tested a belief, what evidence challenged it, what wisdom emerged, which
transformation it all serves — when those records would otherwise only be visible one at a time on
separate pages. It is a governed, read-oriented **projection** over the existing domain records.
Domain modules (transformation, beliefs, experiments, reflection, evidence, wisdom, memory) remain
the sole authoritative source of truth; the graph never becomes an independent data store that can
drift from them.

## 2. User questions the first release must answer

Narrowed from the brief's full list to what's answerable from data that exists today (see the node
and edge catalog below for exactly which questions map to explicit vs. derived vs. not-yet-possible
relationships):

1. What experiences (experiments, reflections) shaped this belief?
2. What evidence supports or challenges this belief?
3. Which experiments and evidence contributed to this transformation?
4. What wisdom has emerged from work on this transformation?
5. Why did Helix propose this relationship? (provenance/explanation on every edge)

Explicitly **not** answerable in the first release (see catalog notes for why): "which beliefs
influence multiple transformations," "what practices support peace/confidence/courage" (no Value or
Growth Dimension concept exists in Helix today — see Section 3), and "why did Helix propose this
memory" beyond what Memory's existing provenance already shows (that's Memory's own detail view, not
a graph concern).

## 3. Node catalog (mapped to actual Helix entities)

The brief proposes 8 node types. Two of them — **Value** and **Growth Dimension** — do not exist
anywhere in Helix's domain model or product docs today (verified: no `values` or `growth_dimensions`
table, no mention in `docs/product/*.md`). The brief itself says growth dimensions "should be
included only if their use and meaning have been sufficiently defined elsewhere in Helix" — they
aren't, so per that same conditional and this project's standing principle against introducing
undefined domain concepts, **both are deferred out of the initial catalog**. Introducing them here
would mean inventing a taxonomy (what counts as a "value"? who defines the list?) that's a real
product decision belonging to a future phase, not something this scoping pass should smuggle in.

| Node type | Backed by | Notes |
|---|---|---|
| Transformation | `transformations` | 1:1 with existing entity. |
| Belief | `beliefs` | 1:1. Revision history (`belief_revisions`) is *intra-node* history, not a separate node or inter-node edge (see Section 4). |
| Experiment | `experiments` | 1:1. |
| Evidence | `evidence` | 1:1. |
| Reflection | `reflections` | 1:1. Per the brief, hidden by default (shown only in expanded/detail views) — Helix already has significantly more reflections than any other record type, and showing them by default would blow past the node-count budget in Section 8. |
| Wisdom | `wisdom_entries` | 1:1. Revision history (`wisdom_revisions`) — same intra-node-history treatment as Belief. |
| Memory | `memory_proposals` | Only `CONFIRMED` status by default, per the brief and consistent with ADR-008's propose-review-accept governance — a `PROPOSED` memory hasn't been vouched for by the user yet and shouldn't read as established knowledge in an exploratory graph. |
| ~~Value~~ | *(deferred)* | No backing entity. Out of scope for Phase 11A-D. |
| ~~Growth Dimension~~ | *(deferred)* | No backing entity. Out of scope for Phase 11A-D. |

## 4. Edge catalog (mapped to actual Helix relationships)

This is the most consequential part of this scoping pass: the brief proposes ~20 edge types as if
they were all equally available. They are not. Cross-referencing every proposed edge type against
Helix's actual foreign keys and fields shows three real categories, which map directly onto the
brief's own `origin` taxonomy:

**A. Explicit — a direct foreign key or field already encodes this relationship.** These require no
new domain behavior; the graph just projects an existing column.

| Edge type | Source of truth |
|---|---|
| `TRANSFORMATION_CONTAINS_BELIEF` | `beliefs.transformation_id` |
| `TRANSFORMATION_CONTAINS_EXPERIMENT` | `experiments.transformation_id` |
| `BELIEF_SUPPORTED_BY_EVIDENCE` / `BELIEF_CHALLENGED_BY_EVIDENCE` | `evidence.belief_id` + `evidence.direction` (SUPPORTS/CHALLENGES) |
| `EXPERIMENT_PRODUCED_EVIDENCE` | `evidence.experiment_id` (nullable FK) |
| `REFLECTION_PRODUCED_EVIDENCE` | `evidence.reflection_id` (nullable FK) |
| `REFLECTION_REFERENCES_EXPERIMENT` | `reflections.experiment_id` (required FK) |
| `WISDOM_SUPPORTED_BY_EVIDENCE` / `WISDOM_EMERGED_FROM_REFLECTION` | `wisdom_source_links` where `source_type` is EVIDENCE / REFLECTION |
| `MEMORY_DERIVED_FROM` | `memory_proposals.source_record_type` + `source_record_id` |

**B. Deterministic derivation — computable by joining existing explicit relationships, but not a
single column.** These are still zero-AI and 100% traceable, just require a join/traversal instead
of a lookup.

| Edge type | Derivation | Caveat |
|---|---|---|
| `BELIEF_EXPLORED_BY_EXPERIMENT` (and its inverse `EXPERIMENT_TESTS_BELIEF`) | `evidence` rows where both `belief_id` and `experiment_id` are set | Only exists when a piece of evidence happens to link both — there's no direct belief↔experiment table today. |
| `TRANSFORMATION_PRODUCED_WISDOM` / `WISDOM_APPLIES_TO_TRANSFORMATION` | `wisdom_source_links` → reflection/evidence → experiment → transformation | **Only for wisdom sourced from REFLECTION or EVIDENCE.** Wisdom sourced from a `RETROSPECTIVE` cannot be attributed to one transformation — a weekly retrospective spans all active transformations that week — so retrospective-sourced wisdom simply won't get this edge in v1. That's an honest gap, not a bug to route around. |
| `EXPERIMENT_INFORMED_WISDOM` | Same chain as above, terminating at experiment instead of transformation | Same retrospective caveat. |
| `REFLECTION_REFERENCES_TRANSFORMATION` | `reflections.experiment_id` → `experiments.transformation_id` | Straightforward two-hop join. |

**C. Not available from current data — deferred to Phase 11E (AI-assisted) or out of scope.**

| Proposed edge type | Why it's deferred |
|---|---|
| `EXPERIMENT_FOLLOWED_BY_EXPERIMENT` | Only signal available is `created_at` ordering within a transformation. Labeling that "followed by" implies a causal/planned sequence Helix doesn't actually track — risks the "false relationship" risk the brief itself calls out. If implemented later, must be worded as "created after," not "followed by." |
| `BELIEF_RELATED_TO_BELIEF` | No explicit or derivable signal exists between two distinct beliefs. AI-proposed only (Phase 11E), never explicit/deterministic. |
| `WISDOM_RELATED_TO_VALUE`, `TRANSFORMATION_DEVELOPS_VALUE`, `TRANSFORMATION_SUPPORTS_GROWTH_DIMENSION` | Depend on the deferred Value/Growth Dimension node types (Section 3). |
| `MEMORY_RELATES_TO`, `MEMORY_INFORMS_SUGGESTION`, `MEMORY_SUPPORTS_INSIGHT` | No backing relationship exists. Helix has no "Insight" entity at all — inventing one here would be exactly the kind of undocumented domain concept this scoping pass exists to avoid. `MEMORY_INFORMS_SUGGESTION` is plausible future work once/if `SuggestionService` is changed to actually read from Memory when generating a suggestion (it currently does not). |
| `BELIEF_REVISED_FROM`, `WISDOM_REVISED_FROM` | Not modeled as inter-node edges. Helix's revision model *mutates* the same belief/wisdom row's statement over time (`belief_revisions`/`wisdom_revisions` are an append-only changelog of one node, not links between two different node records). Representing this as a graph edge would be misleading — there's only ever one Belief node, not two. **Decision: revision history is shown in the node's own detail panel (the brief already calls for a detail panel per node), not as a graph edge.** This may be revisited in Phase 11F (temporal exploration). |
| `BELIEF_INFLUENCES_TRANSFORMATION`, `EXPERIMENT_SUPPORTS_TRANSFORMATION` | Redundant with `TRANSFORMATION_CONTAINS_BELIEF` / `TRANSFORMATION_CONTAINS_EXPERIMENT` read in the reverse direction. Not modeled as separate edge types — the graph query layer can traverse either direction from one stored edge. |

**Initial release edge set (Phase 11B/C/D): all of category A, plus category B excluding the
retrospective-sourced-wisdom gap.** Every edge in the first release has `origin` of either
`EXPLICIT_DOMAIN_RELATIONSHIP` or `DETERMINISTIC_DERIVATION` — **zero AI-proposed edges in the
first release**, matching the brief's own instruction that "the initial graph should work using only
explicit domain relationships and deterministic projections."

## 5. Provenance model

Adopting the brief's `KnowledgeEdge` shape as-is — it's well-designed and every field maps cleanly
onto decisions already made above:

```
KnowledgeEdge
- id, sourceNodeId, targetNodeId, relationshipType
- origin: EXPLICIT_DOMAIN_RELATIONSHIP | USER_CREATED | DETERMINISTIC_DERIVATION | AI_PROPOSED
- status: CONFIRMED | PROPOSED | REJECTED | SUPERSEDED | HIDDEN
- confidence: EXPLICIT | HIGH | MODERATE | LOW | NOT_APPLICABLE
- explanation, createdAt, effectiveFrom, effectiveTo
- sourceRecordIds, aiInvocationId, confirmedAt, rejectedAt, supersededByEdgeId
```

For the first release: every edge is `origin: EXPLICIT_DOMAIN_RELATIONSHIP` or
`DETERMINISTIC_DERIVATION`, both **auto-confirmed** (`status: CONFIRMED`, `confidence: EXPLICIT`)
the moment they're projected — per the brief, "explicit domain relationships may be considered
confirmed automatically when they directly reflect authoritative domain data," and the same
reasoning extends to deterministic derivations since they're just multi-hop views of explicit data,
not inference. `USER_CREATED` and `AI_PROPOSED` origins are defined in the model now but have no
producer until Phase 11E (`AI_PROPOSED`) or a future decision (`USER_CREATED` — see Section 6).

## 6. Governance model

- **Read-only in the first release, with no exceptions.** Since every initial edge is auto-confirmed
  and explicit/deterministic, there is nothing to confirm or reject yet — the confirm/reject/hide
  governance UI described in the brief has no work to do until Phase 11E introduces `AI_PROPOSED`
  edges. Building confirm/reject controls against an all-confirmed edge set in Phase 11C/D would be
  UI with no real function; **defer the governance UI itself to land alongside Phase 11E**, not
  before.
- **`USER_CREATED` edges (manually adding a relationship on the canvas): not in scope for any
  subphase through 11E.** The brief itself says "add a user-defined relationship only if the product
  use case is clearly established" — it isn't yet. Revisit only if real usage shows the deterministic
  set is missing connections users clearly want to draw themselves.
- **No graph edge ever writes back to a domain record.** Corrections happen through each module's
  existing workflow (e.g., re-linking evidence to a different belief happens by editing the evidence
  record, not by dragging an edge on the graph). This directly answers scoping question 8 below.

## 7. Initial user journey (transformation-centered)

Matches the brief's recommended first view and scenario exactly — no changes needed, it already
fits Helix's actual data shape (Transformation → beliefs, experiments, evidence, wisdom is precisely
what's derivable per Section 4):

```
Transformation (focus node)
├── Beliefs (via TRANSFORMATION_CONTAINS_BELIEF)
├── Experiments (via TRANSFORMATION_CONTAINS_EXPERIMENT)
├── Evidence (via EXPERIMENT_PRODUCED_EVIDENCE, one hop from the experiments above)
└── Wisdom (via TRANSFORMATION_PRODUCED_WISDOM, where derivable — see Section 4 caveat)
```

Entry point: an "Explore connections" action on `TransformationDetailPage`, per the brief's
contextual-entry-point guidance (not a standalone-only "Knowledge" tab — though Helix already has a
`/knowledge` route today; seev Section 11 for how this interacts with it). Second view (belief-
centered) follows the same pattern one hop from a Belief node instead of a Transformation node.

## 8. Interaction "wireframes" (textual)

No design tool was used for this scoping pass; the structural shape is fully specified by the
brief's `GraphView`/`GraphNode`/`GraphEdge` response shapes (Section 9 of the brief, adopted
as-is) plus the node/edge detail panel content lists (also adopted as-is from the brief's Sections
10.4/10.5). No changes needed there — they're already generic enough to fit Helix's actual data.

## 9. Accessibility approach

Adopt the brief's Section 12 requirement directly: a structured, navigable list view is a
first-class alternative, not an afterthought bolted onto the canvas. Given the initial edge set is
entirely explicit/deterministic (Section 4), the list view is actually *simpler* to build correctly
in the first release than it would be with proposed/rejected states mixed in — another reason
Section 6's decision to defer governance UI to 11E is low-risk.

## 10. Read-only vs. governed editing decision

**Read-only for Phase 11C. Governed editing (confirm/reject/hide) added in Phase 11D, but only
becomes meaningful once Phase 11E's `AI_PROPOSED` edges exist to act on** (see Section 6). This
slightly reorders the brief's own subphase sequence (11D before 11E) — recommend building 11D's
confirm/reject/hide *mechanism* in the same effort as 11E rather than as a standalone phase against
an all-confirmed graph, since building governance UI with nothing to govern yet doesn't serve users
and can't be meaningfully tested.

## 11. Initial data-volume estimate

Single user, personal-growth journaling cadence (this is the actual usage pattern every prior phase
in this project has assumed — daily-to-weekly reflection, not high-frequency logging):

| Horizon | Transformations | Experiments | Reflections | Evidence | Beliefs | Wisdom |
|---|---|---|---|---|---|---|
| 1 year | 3-8 | 15-40 | 150-400 | 50-150 | 10-25 | 10-30 |
| 3 years | 8-15 | 50-120 | 500-1,200 | 150-400 | 25-60 | 30-80 |
| 10 years | 15-30 | 150-350 | 1,500-4,000 | 400-1,200 | 60-150 | 80-250 |

Even at the 10-year high end, this is a few thousand rows per table — trivially within PostgreSQL's
comfort zone for indexed, bounded-depth queries (confirming ADR-020's "no dedicated graph database"
decision doesn't need revisiting on volume grounds; see ADR-020 Section on reconsideration
triggers). The node-count budget for any single graph *view* stays at the brief's recommended
10-25 visible nodes regardless of total corpus size, since views are always focus-node-scoped, not
whole-graph.

## 12. Answers to the 20 required scoping questions

1. **Which graph-centered user question will the first release answer?** "What beliefs, experiments,
   evidence, and wisdom are connected to this transformation?" (Section 7).
2. **Which node types are necessary?** Transformation, Belief, Experiment, Evidence, Wisdom for the
   default view; Reflection and Memory available in expanded/detail views only (Section 3).
3. **Which relationships are explicit in current domain data?** Section 4, category A.
4. **Which relationships require new domain behavior?** None for the first release — deliberately
   scoped to avoid this. (Category C items would require new domain behavior or new node types and
   are all deferred.)
5. **Which relationships are deterministic projections?** Section 4, category B.
6. **Which relationships may only be proposed by AI?** None ship in the first release; category C's
   `BELIEF_RELATED_TO_BELIEF` is the clearest future AI-only candidate (Phase 11E).
7. **What does user confirmation change?** In the first release, nothing — all edges are
   pre-confirmed. Once 11E ships, confirming an `AI_PROPOSED` edge moves its `status` from
   `PROPOSED` to `CONFIRMED`; it never writes to a domain table (question 8).
8. **Can a confirmed graph relationship update an authoritative domain record?** No. It remains
   graph metadata only, permanently. Corrections go through each domain module's existing workflow
   (Section 6).
9. **How are deleted, archived, rejected, and superseded records represented?** Deleted domain
   records: the projection is rebuildable from source (per ADR-020), so a deleted record's node/edges
   are removed on next rebuild/incremental update, never left dangling. Rejected edges: `status:
   REJECTED`, excluded from default views, visible only in a "rejected" filter if ever built.
   Superseded: `status: SUPERSEDED` + `supersededByEdgeId` pointing at the replacement (not used in
   the first release since nothing supersedes anything yet — reserved for Phase 11F).
10. **How deep should the default graph query traverse?** One hop from the focus node, per the
    brief's recommendation and Section 7's journey (transformation → its direct beliefs/experiments,
    experiments → their direct evidence). Two-hop traversal (e.g., transformation → experiment →
    evidence) is allowed since Section 7 already requires it for the recommended first view, but no
    query goes to three hops in the first release.
11. **What is the maximum initial node count?** 25 visible nodes by default (top of the brief's
    10-25 range, chosen because Section 7's four-category view can plausibly need more nodes than a
    single-category view), with expand-on-demand beyond that.
12. **Expected record volume after 1/3/10 years?** Section 11.
13. **What accessible alternative will accompany the visual graph?** Section 9 — a structured list
    view, first-class from the start.
14. **What is the mobile experience?** Not decided in this scoping pass — genuinely open, and the
    brief doesn't prescribe an answer either. **Flag for Phase 11C implementation to resolve**: most
    likely the list-based accessible view (Section 9) *is* the mobile experience rather than a
    separate canvas-on-small-screens attempt, given the brief's own warning against "a visually
    impressive but confusing hairball" — a force-directed canvas is a poor fit for small screens
    regardless of accessibility. Recommend confirming this with the user before 11C starts.
15. **Which graph library best meets accessibility, maintenance, performance, and licensing needs?**
    Not decided — genuinely requires the evaluation the brief calls for (Section 11: React
    compatibility, accessibility, bundle size, licensing, etc.) against real candidates. **Deferred
    to the start of Phase 11C** as its own short spike, not decided speculatively here.
16. **How will projection rebuilds be triggered and monitored?** Manual `POST` rebuild endpoint
    (mirroring the existing `POST /api/v1/search/index/rebuild` pattern already in this codebase) for
    the first release; scheduled reconciliation is explicitly named in the brief as a *recovery*
    mechanism, not a requirement for launch. A rebuild is cheap given Section 11's volume estimates.
17. **How will stale graph data be detected?** `knowledge_projection_checkpoint` (from the brief's
    suggested schema) records the last successfully-projected point per source module; a simple
    "projection freshness" check compares each module's latest `updated_at`/`revised_at` against the
    checkpoint. Full design belongs in Phase 11B, not this document.
18. **How will relationship provenance be shown in plain language?** Every edge's `explanation` field
    is generated deterministically from a template per edge type (e.g., `BELIEF_SUPPORTED_BY_EVIDENCE`
    → "This evidence supports the belief because it was recorded with direction: supports"), not
    freeform text — keeps the first release AI-free per Section 4's "zero AI-proposed edges" decision.
19. **Which relationships are meaningful enough to expose to the user?** The category A + B set in
    Section 4 — deliberately excludes anything that would require inventing a causal or evaluative
    claim Helix's data doesn't actually support (see the `EXPERIMENT_FOLLOWED_BY_EXPERIMENT` and
    `BELIEF_RELATED_TO_BELIEF` deferrals).
20. **What user action should become easier after exploring the graph?** Recognizing that a belief
    already has real evidence against it (or for it) and real experiments testing it, without having
    to separately open Knowledge, Library, and Journey pages and mentally cross-reference them —
    directly serves product principle #5 ("every insight must be revisable and traceable to
    sources").

## 13. Relationship to the existing `/knowledge` route

Helix already has a `KnowledgePage`/`/knowledge` route (beliefs + evidence, per HELIX-FR-008/009).
The new graph is **not** a replacement for it — Knowledge stays the place to *create and manage*
beliefs/evidence; the graph is a *cross-module exploration* layer on top of Knowledge plus every
other module. Phase 11C's contextual entry points (Section 7) should link *into* existing pages
(Knowledge, Library, Today) for editing, never fork a parallel editing surface.

## Exit criteria check (per the brief's Phase 11A exit criteria)

- Node and edge scope resolved (with two node types and several edge types explicitly deferred, not
  silently dropped) — **needs your sign-off**, this is exactly the "product owner approves" gate the
  brief requires.
- Relationship semantics are explicit — Section 4 categorizes every edge by exactly how it's derived.
- Authoritative ownership is clear — Section 6, domain modules always own the facts.
- AI proposal behavior is defined — zero AI in the first release; Phase 11E's role defined but not
  built.
- Initial graph use case selected — Section 7.

No Phase 11B implementation (Knowledge Graph module, migrations, projection code) should start
until this document and ADR-020 are approved.
