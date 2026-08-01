export type UUID = string;

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
}

export interface WeeklyRetrospective {
  id: UUID;
  periodStart: string;
  periodEnd: string;
  summary: string;
  assistance: string;
  createdAt: string;
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
