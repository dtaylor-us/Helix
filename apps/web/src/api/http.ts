import type {
  Belief,
  BeliefDetail,
  BeliefRevision,
  CreateExperimentRequest,
  CreateBeliefRequest,
  CreateEvidenceRequest,
  CreateMemoryProposalRequest,
  CreateReflectionRequest,
  CreateReflectionResponse,
  CurrentFocusResponse,
  DataExportResponse,
  ExperimentDraft,
  CreateTransformationRequest,
  Evidence,
  Experiment,
  GraphEdge,
  GraphView,
  KnowledgeGraphRebuildResponse,
  KnowledgeGraphStatusResponse,
  KnowledgeNodeType,
  MemoryProposal,
  MemoryProposalDetail,
  MemoryProposalDraft,
  MemoryProposalRevision,
  Reflection,
  ReflectionChatFinishResponse,
  ReflectionChatMessage,
  ReflectionChatTurnResponse,
  ReviewMemoryProposalRequest,
  ReviseBeliefRequest,
  ReviseMemoryProposalRequest,
  Suggestion,
  TodayResponse,
  Transformation,
  CreateWisdomRequest,
  ReviseWisdomRequest,
  SearchResponse,
  SearchIndexRebuildResponse,
  WeeklyRetrospective,
  WeeklyRetrospectiveDraft,
  WisdomDetail,
  WisdomEntry,
  WisdomRevision,
} from '../../../../packages/contracts/src/index'

const API_BASE = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${API_BASE}${path}`, {
    headers: {
      'Content-Type': 'application/json',
      ...(init?.headers ?? {}),
    },
    ...init,
  })

  if (!response.ok) {
    const body = await response.text()
    const message = extractErrorMessage(body) || `Request failed: ${response.status}`
    throw new Error(message)
  }

  const body = await response.text()
  if (!body) {
    return undefined as T
  }

  return JSON.parse(body) as T
}

function extractErrorMessage(body: string): string | null {
  if (!body) {
    return null
  }

  try {
    const parsed = JSON.parse(body) as unknown
    if (typeof parsed === 'string') {
      return parsed
    }

    if (parsed && typeof parsed === 'object') {
      if ('detail' in parsed && typeof parsed.detail === 'string') {
        return parsed.detail
      }

      if ('message' in parsed && typeof parsed.message === 'string') {
        return parsed.message
      }

      if ('title' in parsed && typeof parsed.title === 'string') {
        return parsed.title
      }
    }
  } catch {
    return body
  }

  return body
}

export const api = {
  getToday: () => request<TodayResponse>('/api/v1/today'),
  getCurrentFocus: () => request<CurrentFocusResponse>('/api/v1/current-focus'),
  createTransformation: (payload: CreateTransformationRequest) =>
    request<Transformation>('/api/v1/transformations', {
      method: 'POST',
      body: JSON.stringify(payload),
    }),
  listTransformations: () => request<Transformation[]>('/api/v1/transformations'),
  getTransformation: (id: string) => request<Transformation>(`/api/v1/transformations/${id}`),
  createExperiment: (transformationId: string, payload: CreateExperimentRequest) =>
    request<Experiment>(`/api/v1/transformations/${transformationId}/experiments`, {
      method: 'POST',
      body: JSON.stringify(payload),
    }),
  getExperiment: (id: string) => request<Experiment>(`/api/v1/experiments/${id}`),
  proposeExperimentDraft: (transformationId: string) =>
    request<ExperimentDraft>(`/api/v1/transformations/${transformationId}/experiments/draft`, { method: 'POST' }),
  createReflection: (experimentId: string, payload: CreateReflectionRequest) =>
    request<CreateReflectionResponse>(`/api/v1/experiments/${experimentId}/reflections`, {
      method: 'POST',
      body: JSON.stringify(payload),
    }),
  continueReflectionChat: (experimentId: string, transcript: ReflectionChatMessage[]) =>
    request<ReflectionChatTurnResponse>(`/api/v1/experiments/${experimentId}/reflection-chat/turn`, {
      method: 'POST',
      body: JSON.stringify({ transcript }),
    }),
  finishReflectionChat: (experimentId: string, transcript: ReflectionChatMessage[]) =>
    request<ReflectionChatFinishResponse>(`/api/v1/experiments/${experimentId}/reflection-chat/finish`, {
      method: 'POST',
      body: JSON.stringify({ transcript }),
    }),
  listBeliefs: () => request<Belief[]>('/api/v1/beliefs'),
  createBelief: (payload: CreateBeliefRequest) =>
    request<Belief>('/api/v1/beliefs', {
      method: 'POST',
      body: JSON.stringify(payload),
    }),
  getBelief: (id: string) => request<BeliefDetail>(`/api/v1/beliefs/${id}`),
  reviseBelief: (id: string, payload: ReviseBeliefRequest) =>
    request<BeliefRevision>(`/api/v1/beliefs/${id}/revisions`, {
      method: 'POST',
      body: JSON.stringify(payload),
    }),
  createEvidence: (beliefId: string, payload: CreateEvidenceRequest) =>
    request<Evidence>(`/api/v1/beliefs/${beliefId}/evidence`, {
      method: 'POST',
      body: JSON.stringify(payload),
    }),
  getWeeklyRetrospectiveDraft: () => request<WeeklyRetrospectiveDraft>('/api/v1/wisdom/weekly-retrospective'),
  saveWeeklyRetrospective: () =>
    request<WeeklyRetrospective>('/api/v1/wisdom/weekly-retrospective', {
      method: 'POST',
    }),
  listRetrospectives: () => request<WeeklyRetrospective[]>('/api/v1/wisdom/retrospectives'),
  listWisdom: () => request<WisdomEntry[]>('/api/v1/wisdom'),
  getWisdom: (id: string) => request<WisdomDetail>(`/api/v1/wisdom/${id}`),
  createWisdom: (payload: CreateWisdomRequest) =>
    request<WisdomEntry>('/api/v1/wisdom', {
      method: 'POST',
      body: JSON.stringify(payload),
    }),
  reviseWisdom: (id: string, payload: ReviseWisdomRequest) =>
    request<WisdomRevision>(`/api/v1/wisdom/${id}/revisions`, {
      method: 'POST',
      body: JSON.stringify(payload),
    }),
  search: (query: string) => request<SearchResponse>(`/api/v1/search?q=${encodeURIComponent(query)}`),
  rebuildSearchIndex: () =>
    request<SearchIndexRebuildResponse>('/api/v1/search/index/rebuild', {
      method: 'POST',
    }),
  getReflection: (id: string) => request<Reflection>(`/api/v1/reflections/${id}`),
  listMemoryProposals: () => request<MemoryProposal[]>('/api/v1/memory/proposals'),
  getMemoryProposal: (id: string) => request<MemoryProposalDetail>(`/api/v1/memory/proposals/${id}`),
  proposeMemoryDraft: (reflectionId: string) =>
    request<MemoryProposalDraft>('/api/v1/memory/proposals/draft', {
      method: 'POST',
      body: JSON.stringify({ reflectionId }),
    }),
  createMemoryProposal: (payload: CreateMemoryProposalRequest) =>
    request<MemoryProposal>('/api/v1/memory/proposals', {
      method: 'POST',
      body: JSON.stringify(payload),
    }),
  reviseMemoryProposal: (id: string, payload: ReviseMemoryProposalRequest) =>
    request<MemoryProposalRevision>(`/api/v1/memory/proposals/${id}/revise`, {
      method: 'POST',
      body: JSON.stringify(payload),
    }),
  acceptMemoryProposal: (id: string, payload: ReviewMemoryProposalRequest) =>
    request<MemoryProposalRevision>(`/api/v1/memory/proposals/${id}/accept`, {
      method: 'POST',
      body: JSON.stringify(payload),
    }),
  rejectMemoryProposal: (id: string, payload: ReviewMemoryProposalRequest) =>
    request<MemoryProposalRevision>(`/api/v1/memory/proposals/${id}/reject`, {
      method: 'POST',
      body: JSON.stringify(payload),
    }),
  deleteMemoryProposal: (id: string) => request<void>(`/api/v1/memory/proposals/${id}`, { method: 'DELETE' }),
  acceptSuggestion: (id: string) => request<Suggestion>(`/api/v1/suggestions/${id}/accept`, { method: 'POST' }),
  dismissSuggestion: (id: string) => request<Suggestion>(`/api/v1/suggestions/${id}/dismiss`, { method: 'POST' }),
  replaceSuggestion: (id: string, replacementText: string) =>
    request<Suggestion>(`/api/v1/suggestions/${id}/replace`, {
      method: 'POST',
      body: JSON.stringify({ replacementText }),
    }),
  exportData: () => request<DataExportResponse>('/api/v1/data/export'),
  getGraphByTransformation: (transformationId: string) =>
    request<GraphView>(`/api/v1/knowledge-graph/transformation/${transformationId}`),
  getGraphByBelief: (beliefId: string) => request<GraphView>(`/api/v1/knowledge-graph/belief/${beliefId}`),
  getGraphFocus: (nodeType: KnowledgeNodeType, sourceRecordId: string) =>
    request<GraphView>(`/api/v1/knowledge-graph/focus/${nodeType}/${sourceRecordId}`),
  getGraphStatus: () => request<KnowledgeGraphStatusResponse>('/api/v1/knowledge-graph/status'),
  rebuildGraph: () => request<KnowledgeGraphRebuildResponse>('/api/v1/knowledge-graph/rebuild', { method: 'POST' }),
  confirmGraphEdge: (edgeId: string) => request<GraphEdge>(`/api/v1/knowledge-graph/edges/${edgeId}/confirm`, { method: 'POST' }),
  rejectGraphEdge: (edgeId: string) => request<GraphEdge>(`/api/v1/knowledge-graph/edges/${edgeId}/reject`, { method: 'POST' }),
  hideGraphEdge: (edgeId: string) => request<GraphEdge>(`/api/v1/knowledge-graph/edges/${edgeId}/hide`, { method: 'POST' }),
  // Backend requires an explicit confirm: true body (ADR-019) — not a security control, just
  // protection against a reflexive, no-body DELETE destroying everything by accident.
  deleteAllData: () =>
    request<void>('/api/v1/data', {
      method: 'DELETE',
      body: JSON.stringify({ confirm: true }),
    }),
}
