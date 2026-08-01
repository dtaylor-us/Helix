import type {
  Belief,
  BeliefDetail,
  BeliefRevision,
  CreateExperimentRequest,
  ExperimentDraft,
  CreateBeliefRequest,
  CreateEvidenceRequest,
  CreateMemoryProposalRequest,
  CreateReflectionRequest,
  CreateReflectionResponse,
  CreateTransformationRequest,
  Evidence,
  Experiment,
  MemoryProposal,
  MemoryProposalDetail,
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
    const message = await response.text()
    throw new Error(message || `Request failed: ${response.status}`)
  }

  const body = await response.text()
  if (!body) {
    return undefined as T
  }

  return JSON.parse(body) as T
}

export const api = {
  getToday: () => request<TodayResponse>('/api/v1/today'),
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
}
