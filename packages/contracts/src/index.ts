export type UUID = string;

// ADR-021: the authenticated session's identity, as returned by GET /api/v1/auth/me.
export interface CurrentUser {
  id: UUID;
  email: string;
  displayName?: string;
}

export interface Transformation {
  id: UUID;
  title: string;
  purpose?: string;
  desiredIdentity?: string;
  obstacle?: string;
  createdAt: string;
}

export interface Experiment {
  id: UUID;
  transformationId: UUID;
  title: string;
  hypothesis?: string;
  nextAction?: string;
  cadence?: string;
  evidenceOfSuccess?: string;
  reviewAt?: string;
  status: "ACTIVE" | "COMPLETED";
  createdAt: string;
}

export interface ExperimentDraft {
  title: string;
  hypothesis: string;
  nextAction: string;
  cadence?: string;
  evidenceOfSuccess?: string;
  source: "AI" | "DETERMINISTIC";
  aiProvider?: string;
  aiModel?: string;
}

export interface Reflection {
  id: UUID;
  experimentId: UUID;
  content: string;
  attempted?: boolean | null;
  noticed?: string | null;
  evidenceNoted?: string | null;
  surprise?: string | null;
  createdAt: string;
}

export interface Suggestion {
  id: UUID;
  experimentId: UUID;
  reflectionId?: UUID;
  text: string;
  status: "PROPOSED" | "ACCEPTED" | "DISMISSED" | "REPLACED";
  replacementText?: string;
  createdAt: string;
  respondedAt?: string;
  /** Whether this suggestion's text came from a live AI call or a deterministic/fallback response (ADR-016). */
  source: "AI" | "DETERMINISTIC";
  aiProvider?: string;
  aiModel?: string;
}

export interface Provenance {
  sourceKind: "MANUAL_ENTRY" | "REFLECTION" | "AI_DERIVED";
  recordType: "MANUAL_ENTRY" | "REFLECTION" | "EXPERIMENT";
  recordId?: UUID;
  excerpt?: string;
}

export interface Evidence {
  id: UUID;
  beliefId: UUID;
  experimentId?: UUID;
  reflectionId?: UUID;
  summary: string;
  interpretation?: string;
  direction: "SUPPORTS" | "CHALLENGES";
  provenance: Provenance;
  createdAt: string;
}

export interface Belief {
  id: UUID;
  transformationId: UUID;
  statement: string;
  type: "LIMITING" | "EMPOWERING";
  createdAt: string;
  revisedAt: string;
}

export interface BeliefRevision {
  id: UUID;
  beliefId: UUID;
  previousStatement: string;
  newStatement: string;
  previousType: "LIMITING" | "EMPOWERING";
  newType: "LIMITING" | "EMPOWERING";
  reason: string;
  sourceEvidenceId?: UUID;
  createdAt: string;
}

export interface BeliefDetail {
  belief: Belief;
  revisions: BeliefRevision[];
  evidenceTimeline: Evidence[];
  narrative: string;
}

export interface ReflectionSummary {
  reflectionId: UUID;
  createdAt: string;
  summary: string;
}

export interface WeeklyRetrospectiveDraft {
  periodStart: string;
  periodEnd: string;
  reflectionSummaries: ReflectionSummary[];
  summary: string;
  assistance: string;
  /** Whether summary/assistance came from a live AI call or a deterministic/fallback response (ADR-016). */
  source: "AI" | "DETERMINISTIC";
  aiProvider?: string;
  aiModel?: string;
}

export interface WeeklyRetrospective {
  id: UUID;
  periodStart: string;
  periodEnd: string;
  summary: string;
  assistance: string;
  createdAt: string;
  source: "AI" | "DETERMINISTIC";
  aiProvider?: string;
  aiModel?: string;
}

export interface WisdomEntry {
  id: UUID;
  statement: string;
  status: "ACCEPTED" | "SUPERSEDED";
  retrospectiveId?: UUID;
  createdAt: string;
  revisedAt: string;
}

export interface WisdomRevision {
  id: UUID;
  wisdomId: UUID;
  previousStatement: string;
  newStatement: string;
  reason: string;
  createdAt: string;
}

export interface WisdomSourceLink {
  id: UUID;
  wisdomId: UUID;
  sourceType: "REFLECTION" | "EVIDENCE" | "RETROSPECTIVE";
  sourceRecordId: UUID;
  note?: string;
  createdAt: string;
}

export interface WisdomDetail {
  wisdom: WisdomEntry;
  revisions: WisdomRevision[];
  sources: WisdomSourceLink[];
}

export interface SearchRecord {
  recordType: "REFLECTION" | "BELIEF" | "EVIDENCE" | "WISDOM" | "RETROSPECTIVE";
  recordId: UUID;
  snippet: string;
  createdAt: string;
  matchType: "KEYWORD" | "SEMANTIC" | "HYBRID";
  score: number;
}

export interface SearchResponse {
  query?: string;
  results: SearchRecord[];
  note: string;
}

export interface SearchIndexRebuildResponse {
  indexedCount: number;
  embeddingModel: string;
  indexedAt: string;
}

export interface MemoryProposal {
  id: UUID;
  statement: string;
  status: "TEMPORARY" | "PROPOSED" | "CONFIRMED" | "REJECTED" | "SUPERSEDED";
  sourceKind: "MANUAL_ENTRY" | "REFLECTION" | "AI_DERIVED";
  sourceRecordType: "MANUAL_ENTRY" | "REFLECTION" | "EXPERIMENT" | "BELIEF" | "EVIDENCE" | "WISDOM" | "RETROSPECTIVE";
  sourceRecordId: UUID;
  sourceExcerpt?: string;
  createdAt: string;
  revisedAt: string;
}

export interface MemoryProposalRevision {
  id: UUID;
  memoryProposalId: UUID;
  previousStatement: string;
  newStatement: string;
  previousStatus: MemoryProposal["status"];
  newStatus: MemoryProposal["status"];
  reason: string;
  createdAt: string;
}

export interface MemoryProposalDetail {
  proposal: MemoryProposal;
  revisions: MemoryProposalRevision[];
}

export interface TodayResponse {
  hasActiveExperiment: boolean;
  activeExperiment: Experiment | null;
  reflectionHistory: Reflection[];
  suggestionHistory: Suggestion[];
}

/**
 * Server-persisted onboarding progress (Phase 7), replacing the purely client-derived
 * `transformations.length === 0` welcome-state check. NOT_STARTED -> FIRST_TRANSFORMATION_CREATED
 * happens when the first transformation is created; -> COMPLETE happens when the first experiment
 * is created. Monotonic — never moves backward.
 */
export type OnboardingStatus = "NOT_STARTED" | "FIRST_TRANSFORMATION_CREATED" | "COMPLETE";

/**
 * Phase 7 projection: everything Today's UI needs in one response, replacing two separate calls
 * to GET /api/v1/today and GET /api/v1/transformations.
 */
export interface CurrentFocusResponse {
  onboardingStatus: OnboardingStatus;
  transformations: Transformation[];
  hasActiveExperiment: boolean;
  activeExperiment: Experiment | null;
  reflectionHistory: Reflection[];
  suggestionHistory: Suggestion[];
}

export interface CreateTransformationRequest {
  title: string;
  purpose?: string;
  desiredIdentity?: string;
  obstacle?: string;
}

export interface CreateExperimentRequest {
  title: string;
  hypothesis?: string;
  nextAction?: string;
  cadence?: string;
  evidenceOfSuccess?: string;
  reviewAt?: string;
}

export interface CreateReflectionRequest {
  content: string;
  attempted?: boolean;
  noticed?: string;
  evidenceNoted?: string;
  surprise?: string;
}

export interface ReflectionChatMessage {
  role: "user" | "assistant";
  text: string;
}

export interface ReflectionChatTurnResponse {
  text: string;
  source: "AI" | "DETERMINISTIC";
  aiProvider?: string;
  aiModel?: string;
}

export interface ReflectionChatFinishResponse {
  content?: string;
  attempted?: boolean;
  noticed?: string;
  evidenceNoted?: string;
  surprise?: string;
  source: "AI" | "DETERMINISTIC";
  aiProvider?: string;
  aiModel?: string;
}

export interface CreateReflectionResponse {
  reflection: Reflection;
  suggestion: Suggestion;
}

export interface CreateBeliefRequest {
  transformationId: UUID;
  statement: string;
  type: "LIMITING" | "EMPOWERING";
}

export interface ReviseBeliefRequest {
  statement: string;
  type: "LIMITING" | "EMPOWERING";
  reason: string;
  sourceEvidenceId?: UUID;
}

export interface CreateEvidenceRequest {
  summary: string;
  interpretation?: string;
  direction: "SUPPORTS" | "CHALLENGES";
  experimentId?: UUID;
  reflectionId?: UUID;
  provenance: Provenance;
}

export interface WisdomSourceLinkRequest {
  sourceType: "REFLECTION" | "EVIDENCE" | "RETROSPECTIVE";
  sourceRecordId: UUID;
  note?: string;
}

export interface CreateWisdomRequest {
  statement: string;
  retrospectiveId?: UUID;
  sources: WisdomSourceLinkRequest[];
}

export interface ReviseWisdomRequest {
  statement: string;
  reason: string;
}

export interface CreateMemoryProposalRequest {
  statement: string;
  sourceKind: MemoryProposal["sourceKind"];
  sourceRecordType: MemoryProposal["sourceRecordType"];
  sourceRecordId: UUID;
  sourceExcerpt?: string;
}

export interface ReviseMemoryProposalRequest {
  statement: string;
  reason: string;
  sourceExcerpt?: string;
}

export interface ReviewMemoryProposalRequest {
  reason: string;
}

/**
 * Phase 9 (ADR-015, ADR-019): a complete, human-readable export of every user-owned record.
 * Deliberately excludes the semantic search index — that's a derived/regenerable cache, not
 * user-authored content.
 */
export interface DataExportResponse {
  onboardingStatus: OnboardingStatus;
  transformations: Transformation[];
  experiments: Experiment[];
  reflections: Reflection[];
  suggestions: Suggestion[];
  beliefs: Belief[];
  beliefRevisions: BeliefRevision[];
  evidence: Evidence[];
  weeklyRetrospectives: WeeklyRetrospective[];
  wisdomEntries: WisdomEntry[];
  wisdomRevisions: WisdomRevision[];
  wisdomSourceLinks: WisdomSourceLink[];
  memoryProposals: MemoryProposal[];
  memoryProposalRevisions: MemoryProposalRevision[];
}

export interface ProposeMemoryDraftRequest {
  reflectionId: UUID;
}

export interface MemoryProposalDraft {
  statement?: string;
  source: "AI" | "DETERMINISTIC";
  aiProvider?: string;
  aiModel?: string;
}

/**
 * Phase 11 (ADR-020): the knowledge graph is a read-oriented relational projection over the
 * authoritative domain records above, not a separate source of truth. `sourceRecordId` is the id of
 * the underlying Transformation/Belief/Experiment/Evidence/Reflection/Wisdom/Memory row this node
 * projects; node/edge ids themselves are NOT stable across a projection rebuild.
 */
export type KnowledgeNodeType = "TRANSFORMATION" | "BELIEF" | "EXPERIMENT" | "EVIDENCE" | "REFLECTION" | "WISDOM" | "MEMORY";

export interface GraphNode {
  id: UUID;
  type: KnowledgeNodeType;
  label: string;
  summary?: string;
  sourceRecordId: UUID;
  sourceRoute?: string;
  status?: string;
  visualCategory: string;
}

export interface GraphEdgeSourceReference {
  recordType: string;
  recordId: UUID;
}

/**
 * Phase 11F: a lightweight history view over columns every edge has carried since the 11B
 * migration. `effectiveFrom`/`effectiveTo` are reserved for a future edge-validity-window feature
 * and are typically null today — nothing in this app currently revises an edge's validity window.
 */
export interface GraphEdgeHistory {
  createdAt: string;
  confirmedAt?: string;
  rejectedAt?: string;
  effectiveFrom?: string;
  effectiveTo?: string;
  supersededByEdgeId?: UUID;
}

/**
 * Every edge carries its provenance (ADR-020): `origin` explains how it was derived, `status`
 * whether it's currently shown/confirmed, `confidence` how certain the derivation is. The first
 * release ships only EXPLICIT_DOMAIN_RELATIONSHIP/DETERMINISTIC_DERIVATION origins, all CONFIRMED —
 * AI_PROPOSED/PROPOSED only start appearing once Phase 11E ships.
 */
export interface GraphEdge {
  id: UUID;
  sourceNodeId: UUID;
  targetNodeId: UUID;
  relationshipType: string;
  displayLabel: string;
  origin: "EXPLICIT_DOMAIN_RELATIONSHIP" | "USER_CREATED" | "DETERMINISTIC_DERIVATION" | "AI_PROPOSED";
  status: "CONFIRMED" | "PROPOSED" | "REJECTED" | "SUPERSEDED" | "HIDDEN";
  confidence: "EXPLICIT" | "HIGH" | "MODERATE" | "LOW" | "NOT_APPLICABLE";
  explanation?: string;
  sourceReferences: GraphEdgeSourceReference[];
  history: GraphEdgeHistory;
}

/** A bounded, focus-node-centered view — never the whole graph (default: 25 nodes, 2-hop depth). */
export interface GraphView {
  title: string;
  description?: string;
  focusNodeId: UUID;
  nodes: GraphNode[];
  edges: GraphEdge[];
  truncated: boolean;
}

export interface KnowledgeGraphRebuildResponse {
  nodeCount: number;
  edgeCount: number;
  rebuiltAt: string;
}

export interface KnowledgeGraphCheckpoint {
  sourceModule: string;
  lastProjectedAt: string;
}

export interface KnowledgeGraphStatusResponse {
  checkpoints: KnowledgeGraphCheckpoint[];
}

/**
 * Phase 11E: a manually triggered, bounded pass comparing pairs of beliefs with no existing
 * connection. Anything found lands as a PROPOSED/AI_PROPOSED edge for a human to review via the
 * Phase 11D governance actions — this response is just the run's own accounting, not the edges.
 */
export interface KnowledgeGraphDiscoveryResponse {
  pairsEvaluated: number;
  proposalsCreated: number;
}
