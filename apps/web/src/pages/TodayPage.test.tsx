import { describe, expect, it, vi, afterEach } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { RouterProvider, createRootRoute, createRoute, createRouter } from '@tanstack/react-router'
import { TodayPage } from './TodayPage'
import { api } from '../api/http'
import type { Experiment, Reflection, Suggestion } from '../../../../packages/contracts/src'

vi.mock('../api/http', () => ({
  api: {
    getCurrentFocus: vi.fn(),
    createReflection: vi.fn(),
    continueReflectionChat: vi.fn(),
    finishReflectionChat: vi.fn(),
    acceptSuggestion: vi.fn(),
    dismissSuggestion: vi.fn(),
    replaceSuggestion: vi.fn(),
    getWeeklyRetrospectiveDraft: vi.fn(),
    createWisdom: vi.fn(),
    proposeMemoryDraft: vi.fn(),
    createMemoryProposal: vi.fn(),
  },
}))

const EMPTY_RETROSPECTIVE_DRAFT = {
  periodStart: '2026-01-01',
  periodEnd: '2026-01-07',
  reflectionSummaries: [],
  summary: '',
  assistance: '',
  source: 'DETERMINISTIC' as const,
}

const BASE_TRANSFORMATIONS = [{ id: 't-1', title: 'Become more peaceful', createdAt: '2026-01-01T00:00:00Z' }]

const BASE_ACTIVE_EXPERIMENT = {
  id: 'e-1',
  transformationId: 't-1',
  title: 'Pause before responding',
  hypothesis: 'Pausing helps me respond calmly',
  status: 'ACTIVE' as const,
  createdAt: '2026-01-01T00:00:00Z',
}

/** A Today/current-focus response for a user who has already completed onboarding (Phase 7) with an active experiment. */
function activeExperimentFocus(overrides: Partial<{
  activeExperiment: Experiment
  reflectionHistory: Reflection[]
  suggestionHistory: Suggestion[]
  transformations: typeof BASE_TRANSFORMATIONS
}> = {}) {
  return {
    onboardingStatus: 'COMPLETE' as const,
    transformations: BASE_TRANSFORMATIONS,
    hasActiveExperiment: true,
    activeExperiment: BASE_ACTIVE_EXPERIMENT,
    reflectionHistory: [],
    suggestionHistory: [],
    ...overrides,
  }
}

function renderTodayPage() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  const rootRoute = createRootRoute({})
  const indexRoute = createRoute({ getParentRoute: () => rootRoute, path: '/', component: TodayPage })
  const transformationsRoute = createRoute({
    getParentRoute: () => rootRoute,
    path: '/transformations',
    component: () => null,
  })
  const transformationDetailRoute = createRoute({
    getParentRoute: () => rootRoute,
    path: '/transformations/$id',
    component: () => null,
  })
  const knowledgeRoute = createRoute({
    getParentRoute: () => rootRoute,
    path: '/knowledge',
    component: () => null,
  })
  const experimentRoute = createRoute({
    getParentRoute: () => rootRoute,
    path: '/experiments/$id',
    component: () => null,
  })
  const libraryRoute = createRoute({
    getParentRoute: () => rootRoute,
    path: '/library',
    component: () => null,
  })
  const router = createRouter({
    routeTree: rootRoute.addChildren([
      indexRoute,
      transformationsRoute,
      transformationDetailRoute,
      knowledgeRoute,
      libraryRoute,
      experimentRoute,
    ]),
  })

  return render(
    <QueryClientProvider client={client}>
      <RouterProvider router={router} />
    </QueryClientProvider>,
  )
}

describe('TodayPage', () => {
  afterEach(() => {
    vi.mocked(api.getCurrentFocus).mockReset()
    vi.mocked(api.createReflection).mockReset()
    vi.mocked(api.continueReflectionChat).mockReset()
    vi.mocked(api.finishReflectionChat).mockReset()
    vi.mocked(api.acceptSuggestion).mockReset()
    vi.mocked(api.dismissSuggestion).mockReset()
    vi.mocked(api.replaceSuggestion).mockReset()
    vi.mocked(api.getWeeklyRetrospectiveDraft).mockReset()
    vi.mocked(api.createWisdom).mockReset()
    vi.mocked(api.proposeMemoryDraft).mockReset()
    vi.mocked(api.createMemoryProposal).mockReset()
  })

  it('shows loading state', async () => {
    vi.mocked(api.getCurrentFocus).mockReturnValue(new Promise(() => {}))
    renderTodayPage()

    expect(await screen.findByText(/Loading your active context/i)).toBeInTheDocument()
  })

  it('shows a welcome/first-use state when onboarding has not started', async () => {
    vi.mocked(api.getCurrentFocus).mockResolvedValue({
      onboardingStatus: 'NOT_STARTED',
      transformations: [],
      hasActiveExperiment: false,
      activeExperiment: null,
      reflectionHistory: [],
      suggestionHistory: [],
    })

    renderTodayPage()

    expect(await screen.findByText(/Welcome to Helix/i)).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /Begin my first transformation/i })).toBeInTheDocument()
  })

  it('shows a direct call to action when a transformation exists but no experiment is active', async () => {
    vi.mocked(api.getCurrentFocus).mockResolvedValue({
      onboardingStatus: 'FIRST_TRANSFORMATION_CREATED',
      transformations: BASE_TRANSFORMATIONS,
      hasActiveExperiment: false,
      activeExperiment: null,
      reflectionHistory: [],
      suggestionHistory: [],
    })
    vi.mocked(api.getWeeklyRetrospectiveDraft).mockResolvedValue(EMPTY_RETROSPECTIVE_DRAFT)

    renderTodayPage()

    expect(await screen.findByRole('link', { name: /Add an experiment to/i })).toBeInTheDocument()
  })

  it('orders the suggested action above the reflection prompt and does not leak roadmap language', async () => {
    vi.mocked(api.getCurrentFocus).mockResolvedValue(activeExperimentFocus({
      suggestionHistory: [
        {
          id: 's-1',
          experimentId: 'e-1',
          text: 'Take one breath before replying',
          status: 'PROPOSED',
          createdAt: '2026-01-01T00:00:00Z',
          source: 'AI',
          aiProvider: 'openai',
          aiModel: 'gpt-4o-mini',
        },
      ],
    }))
    vi.mocked(api.getWeeklyRetrospectiveDraft).mockResolvedValue(EMPTY_RETROSPECTIVE_DRAFT)

    renderTodayPage()

    await waitFor(() => expect(screen.getByText(/Take one breath before replying/i)).toBeInTheDocument())

    const headings = screen.getAllByRole('heading', { level: 2 }).map((el) => el.textContent)
    const suggestionIndex = headings.findIndex((h) => h?.includes('Suggested Small Action'))
    const reflectIndex = headings.findIndex((h) => h === 'Morning check-in' || h === 'Evening review')
    expect(suggestionIndex).toBeGreaterThanOrEqual(0)
    expect(reflectIndex).toBeGreaterThan(suggestionIndex)

    expect(screen.getByText(/AI suggested — openai/i)).toBeInTheDocument()

    expect(screen.queryByText(/coming in a later increment/i)).not.toBeInTheDocument()
    expect(screen.queryByText(/^Placeholders$/i)).not.toBeInTheDocument()
  })

  it('shows the replacement text after replacing a suggested action', async () => {
    const originalSuggestion = {
      id: 's-1',
      experimentId: 'e-1',
      text: 'Reply immediately with the first thought',
      status: 'PROPOSED' as const,
      createdAt: '2026-01-01T00:00:00Z',
      source: 'DETERMINISTIC' as const,
    }
    const replacedSuggestion = {
      ...originalSuggestion,
      status: 'REPLACED' as const,
      replacementText: 'Take one breath before replying',
      respondedAt: '2026-01-01T00:05:00Z',
    }
    const focusResponse = activeExperimentFocus({ suggestionHistory: [originalSuggestion] })
    vi.mocked(api.getCurrentFocus)
      .mockResolvedValueOnce(focusResponse)
      .mockResolvedValue({ ...focusResponse, suggestionHistory: [replacedSuggestion] })
    vi.mocked(api.getWeeklyRetrospectiveDraft).mockResolvedValue(EMPTY_RETROSPECTIVE_DRAFT)
    vi.mocked(api.replaceSuggestion).mockResolvedValue(replacedSuggestion)

    renderTodayPage()

    await screen.findByText(originalSuggestion.text)
    fireEvent.change(screen.getByLabelText('Replacement suggestion'), {
      target: { value: replacedSuggestion.replacementText },
    })
    fireEvent.click(screen.getByRole('button', { name: 'Use this instead' }))

    await waitFor(() =>
      expect(api.replaceSuggestion).toHaveBeenCalledWith('s-1', replacedSuggestion.replacementText),
    )
    expect(await screen.findByText(replacedSuggestion.replacementText)).toBeInTheDocument()
    expect(screen.queryByText(originalSuggestion.text)).not.toBeInTheDocument()
    expect(screen.getByText(/Status: REPLACED/i)).toBeInTheDocument()
  })

  it.each([
    ['I’ll try this', 'acceptSuggestion', 'ACCEPTED', /Smallest next action on your active experiment/i],
    ['Not this one', 'dismissSuggestion', 'DISMISSED', /Passed on this action/i],
  ] as const)('immediately reflects the %s suggestion choice', async (buttonName, apiMethod, status, confirmation) => {
    const suggestion = {
      id: 's-1',
      experimentId: 'e-1',
      text: 'Take one breath before replying',
      status: 'PROPOSED' as const,
      createdAt: '2026-01-01T00:00:00Z',
      source: 'DETERMINISTIC' as const,
    }
    const respondedSuggestion = {
      ...suggestion,
      status,
      respondedAt: '2026-01-01T00:05:00Z',
    }
    const initialFocus = activeExperimentFocus({ suggestionHistory: [suggestion] })
    vi.mocked(api.getCurrentFocus)
      .mockResolvedValueOnce(initialFocus)
      .mockResolvedValue({ ...initialFocus, suggestionHistory: [respondedSuggestion] })
    vi.mocked(api.getWeeklyRetrospectiveDraft).mockResolvedValue(EMPTY_RETROSPECTIVE_DRAFT)
    vi.mocked(api[apiMethod]).mockResolvedValue(respondedSuggestion)

    renderTodayPage()
    fireEvent.click(await screen.findByRole('button', { name: buttonName }))

    expect(await screen.findByText(confirmation)).toBeInTheDocument()
    expect(screen.getByText(new RegExp(`Status: ${status}`, 'i'))).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: buttonName })).not.toBeInTheDocument()
  })

  it('shows the accepted small action as the experiment\'s next action, both on Today and via the Journey link', async () => {
    const suggestion = {
      id: 's-1',
      experimentId: 'e-1',
      text: 'Take one breath before replying',
      status: 'PROPOSED' as const,
      createdAt: '2026-01-01T00:00:00Z',
      source: 'DETERMINISTIC' as const,
    }
    const initialFocus = activeExperimentFocus({ suggestionHistory: [suggestion] })
    // After accepting, the backend now also revises the experiment's own nextAction (see
    // SuggestionService.accept) -- the refetched current-focus response reflects that.
    const refetchedFocus = activeExperimentFocus({
      activeExperiment: { ...BASE_ACTIVE_EXPERIMENT, nextAction: 'Take one breath before replying' },
      suggestionHistory: [{ ...suggestion, status: 'ACCEPTED' as const, respondedAt: '2026-01-01T00:05:00Z' }],
    })
    vi.mocked(api.getCurrentFocus).mockResolvedValueOnce(initialFocus).mockResolvedValue(refetchedFocus)
    vi.mocked(api.getWeeklyRetrospectiveDraft).mockResolvedValue(EMPTY_RETROSPECTIVE_DRAFT)
    vi.mocked(api.acceptSuggestion).mockResolvedValue({
      ...suggestion, status: 'ACCEPTED', respondedAt: '2026-01-01T00:05:00Z',
    })

    renderTodayPage()

    expect(await screen.findByText(/Not set yet — accept a suggested action below/i)).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'See this experiment in your Journey' })).toHaveAttribute(
      'href', '/experiments/e-1',
    )

    fireEvent.click(await screen.findByRole('button', { name: 'I’ll try this' }))

    expect(await screen.findByText(/Smallest next action on your active experiment/i)).toBeInTheDocument()
    await waitFor(() => {
      const nextAction = screen.getByText('Smallest next action').closest('.direction-next-action')
      expect(nextAction).toHaveTextContent('Take one breath before replying')
    })
  })

  it('captures reflection via chat, allows review edits, and saves the edited structured payload', async () => {
    vi.mocked(api.getCurrentFocus).mockResolvedValue(activeExperimentFocus())
    vi.mocked(api.getWeeklyRetrospectiveDraft).mockResolvedValue(EMPTY_RETROSPECTIVE_DRAFT)
    vi.mocked(api.continueReflectionChat).mockResolvedValue({
      text: 'What did you notice internally afterward?',
      source: 'AI',
      aiProvider: 'openai',
      aiModel: 'gpt-4o-mini',
    })
    vi.mocked(api.finishReflectionChat).mockResolvedValue({
      content: 'I paused twice today before replying.',
      attempted: true,
      noticed: 'My shoulders were tense at first.',
      evidenceNoted: 'The conversation stayed calmer.',
      surprise: 'It felt easier by the second attempt.',
      source: 'AI',
      aiProvider: 'openai',
      aiModel: 'gpt-4o-mini',
    })
    vi.mocked(api.createReflection).mockResolvedValue({
      reflection: {
        id: 'r-1',
        experimentId: 'e-1',
        content: 'I paused twice today.',
        createdAt: '2026-01-01T00:00:00Z',
      },
      suggestion: {
        id: 's-1',
        experimentId: 'e-1',
        text: 'Optional next step: keep going',
        status: 'PROPOSED',
        createdAt: '2026-01-01T00:00:00Z',
        source: 'AI',
        aiProvider: 'openai',
        aiModel: 'gpt-4o-mini',
      },
    })
    vi.mocked(api.proposeMemoryDraft).mockResolvedValue({ source: 'DETERMINISTIC' })

    renderTodayPage()

    await screen.findByLabelText(/Your message/i)
    fireEvent.change(screen.getByLabelText(/Your message/i), { target: { value: 'I paused twice today.' } })
    fireEvent.click(screen.getByRole('button', { name: 'Send' }))

    await waitFor(() =>
      expect(api.continueReflectionChat).toHaveBeenCalledWith('e-1', [{ role: 'user', text: 'I paused twice today.' }]),
    )
    expect(await screen.findByText(/Helix:/i)).toBeInTheDocument()
    expect(screen.getByText(/What did you notice internally afterward\?/i)).toBeInTheDocument()

    fireEvent.change(screen.getByLabelText(/Your message/i), { target: { value: 'My shoulders relaxed after.' } })
    fireEvent.click(screen.getByRole('button', { name: 'Send' }))

    await waitFor(() =>
      expect(api.continueReflectionChat).toHaveBeenLastCalledWith('e-1', [
        { role: 'user', text: 'I paused twice today.' },
        { role: 'assistant', text: 'What did you notice internally afterward?' },
        { role: 'user', text: 'My shoulders relaxed after.' },
      ]),
    )

    fireEvent.click(screen.getByRole('button', { name: /I'm done — review my reflection/i }))

    await waitFor(() =>
      expect(api.finishReflectionChat).toHaveBeenCalledWith('e-1', [
        { role: 'user', text: 'I paused twice today.' },
        { role: 'assistant', text: 'What did you notice internally afterward?' },
        { role: 'user', text: 'My shoulders relaxed after.' },
        { role: 'assistant', text: 'What did you notice internally afterward?' },
      ]),
    )

    expect(await screen.findByRole('heading', { name: /Review before saving/i })).toBeInTheDocument()
    fireEvent.change(screen.getByLabelText('What evidence did this give you?'), {
      target: { value: 'The conversation stayed calmer and shorter.' },
    })
    fireEvent.change(screen.getByLabelText('What surprised you?'), {
      target: { value: 'I felt less defensive than usual.' },
    })

    fireEvent.click(screen.getByRole('button', { name: 'Save reflection' }))

    await waitFor(() =>
      expect(api.createReflection).toHaveBeenCalledWith('e-1', {
        content: 'I paused twice today before replying.',
        attempted: true,
        noticed: 'My shoulders were tense at first.',
        evidenceNoted: 'The conversation stayed calmer and shorter.',
        surprise: 'I felt less defensive than usual.',
      }),
    )

    expect(await screen.findByText(/This might be useful evidence/i)).toBeInTheDocument()
  })

  it('offers a contextual wisdom prompt prefilled from the most specific answer, and saves it', async () => {
    vi.mocked(api.getCurrentFocus).mockResolvedValue(activeExperimentFocus())
    vi.mocked(api.getWeeklyRetrospectiveDraft).mockResolvedValue(EMPTY_RETROSPECTIVE_DRAFT)
    vi.mocked(api.continueReflectionChat).mockResolvedValue({
      text: 'What evidence did that give you?',
      source: 'AI',
      aiProvider: 'openai',
      aiModel: 'gpt-4o-mini',
    })
    vi.mocked(api.finishReflectionChat).mockResolvedValue({
      content: 'I paused twice today.',
      attempted: true,
      noticed: 'My shoulders were tense.',
      evidenceNoted: 'The conversation stayed calmer.',
      surprise: '',
      source: 'AI',
      aiProvider: 'openai',
      aiModel: 'gpt-4o-mini',
    })
    vi.mocked(api.createReflection).mockResolvedValue({
      reflection: {
        id: 'r-1',
        experimentId: 'e-1',
        content: 'I paused twice today.',
        createdAt: '2026-01-01T00:00:00Z',
      },
      suggestion: {
        id: 's-1',
        experimentId: 'e-1',
        text: 'Optional next step: keep going',
        status: 'PROPOSED',
        createdAt: '2026-01-01T00:00:00Z',
        source: 'AI',
        aiProvider: 'openai',
        aiModel: 'gpt-4o-mini',
      },
    })
    vi.mocked(api.createWisdom).mockResolvedValue({
      id: 'w-1',
      statement: 'The conversation stayed calmer.',
      status: 'ACCEPTED',
      createdAt: '2026-01-01T00:00:00Z',
      revisedAt: '2026-01-01T00:00:00Z',
    })
    vi.mocked(api.proposeMemoryDraft).mockResolvedValue({ source: 'DETERMINISTIC' })

    renderTodayPage()

    await screen.findByLabelText(/Your message/i)
    expect(screen.queryByText(/This reflection may contain a lesson worth keeping/i)).not.toBeInTheDocument()

    fireEvent.change(screen.getByLabelText(/Your message/i), { target: { value: 'I paused twice today.' } })
    fireEvent.click(screen.getByRole('button', { name: 'Send' }))
    await waitFor(() => expect(api.continueReflectionChat).toHaveBeenCalled())
    fireEvent.click(screen.getByRole('button', { name: /I'm done — review my reflection/i }))
    await waitFor(() => expect(api.finishReflectionChat).toHaveBeenCalled())
    fireEvent.click(screen.getByRole('button', { name: 'Save reflection' }))

    await screen.findByText(/This reflection may contain a lesson worth keeping/i)
    // Prefilled from evidenceNoted, the most specific answer given.
    expect(screen.getByLabelText('Proposed wisdom statement')).toHaveValue('The conversation stayed calmer.')

    fireEvent.click(screen.getByRole('button', { name: 'Save as wisdom' }))

    await waitFor(() =>
      expect(api.createWisdom).toHaveBeenCalledWith({
        statement: 'The conversation stayed calmer.',
        sources: [{ sourceType: 'REFLECTION', sourceRecordId: 'r-1' }],
      }),
    )
    expect(await screen.findByText(/Wisdom saved to your Library/i)).toBeInTheDocument()
    expect(screen.queryByText(/This reflection may contain a lesson worth keeping/i)).not.toBeInTheDocument()
  })

  it('offers an AI-derived memory proposal after saving a reflection, distinct from the wisdom card, and saves it', async () => {
    vi.mocked(api.getCurrentFocus).mockResolvedValue(activeExperimentFocus())
    vi.mocked(api.getWeeklyRetrospectiveDraft).mockResolvedValue(EMPTY_RETROSPECTIVE_DRAFT)
    vi.mocked(api.continueReflectionChat).mockResolvedValue({
      text: 'What did you notice internally afterward?',
      source: 'AI',
      aiProvider: 'openai',
      aiModel: 'gpt-4o-mini',
    })
    vi.mocked(api.finishReflectionChat).mockResolvedValue({
      content: 'I paused twice today before replying.',
      attempted: true,
      noticed: 'My shoulders were tense at first.',
      source: 'AI',
      aiProvider: 'openai',
      aiModel: 'gpt-4o-mini',
    })
    vi.mocked(api.createReflection).mockResolvedValue({
      reflection: {
        id: 'r-1',
        experimentId: 'e-1',
        content: 'I paused twice today before replying.',
        createdAt: '2026-01-01T00:00:00Z',
      },
      suggestion: {
        id: 's-1',
        experimentId: 'e-1',
        text: 'Optional next step: keep going',
        status: 'PROPOSED',
        createdAt: '2026-01-01T00:00:00Z',
        source: 'AI',
        aiProvider: 'openai',
        aiModel: 'gpt-4o-mini',
      },
    })
    vi.mocked(api.proposeMemoryDraft).mockResolvedValue({
      statement: 'I tend to feel steadier when I pause before reacting.',
      source: 'AI',
      aiProvider: 'openai',
      aiModel: 'gpt-4o-mini',
    })
    vi.mocked(api.createMemoryProposal).mockResolvedValue({
      id: 'm-1',
      statement: 'I tend to feel steadier when I pause before reacting.',
      status: 'PROPOSED',
      sourceKind: 'AI_DERIVED',
      sourceRecordType: 'REFLECTION',
      sourceRecordId: 'r-1',
      createdAt: '2026-01-01T00:00:00Z',
      revisedAt: '2026-01-01T00:00:00Z',
    })

    renderTodayPage()

    await screen.findByLabelText(/Your message/i)
    expect(screen.queryByText(/Something worth remembering about you/i)).not.toBeInTheDocument()

    fireEvent.change(screen.getByLabelText(/Your message/i), { target: { value: 'I paused twice today.' } })
    fireEvent.click(screen.getByRole('button', { name: 'Send' }))
    await waitFor(() => expect(api.continueReflectionChat).toHaveBeenCalled())
    fireEvent.click(screen.getByRole('button', { name: /I'm done — review my reflection/i }))
    await waitFor(() => expect(api.finishReflectionChat).toHaveBeenCalled())
    fireEvent.click(screen.getByRole('button', { name: 'Save reflection' }))

    await waitFor(() => expect(api.proposeMemoryDraft).toHaveBeenCalledWith('r-1'))
    await screen.findByText(/Something worth remembering about you/i)
    expect(screen.getByLabelText('Proposed memory statement')).toHaveValue(
      'I tend to feel steadier when I pause before reacting.',
    )
    expect(screen.getByText(/AI suggested — openai/i)).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: 'Remember this' }))

    await waitFor(() =>
      expect(api.createMemoryProposal).toHaveBeenCalledWith({
        statement: 'I tend to feel steadier when I pause before reacting.',
        sourceKind: 'AI_DERIVED',
        sourceRecordType: 'REFLECTION',
        sourceRecordId: 'r-1',
      }),
    )
    expect(await screen.findByText(/Saved to Memory for review/i)).toBeInTheDocument()
    expect(screen.queryByText(/Something worth remembering about you/i)).not.toBeInTheDocument()
  })

  it('does not show a memory-proposal card when the AI has nothing durable to propose', async () => {
    vi.mocked(api.getCurrentFocus).mockResolvedValue(activeExperimentFocus())
    vi.mocked(api.getWeeklyRetrospectiveDraft).mockResolvedValue(EMPTY_RETROSPECTIVE_DRAFT)
    vi.mocked(api.continueReflectionChat).mockResolvedValue({
      text: 'What did you notice internally afterward?',
      source: 'DETERMINISTIC',
    })
    vi.mocked(api.finishReflectionChat).mockResolvedValue({
      content: 'Nothing notable happened today.',
      source: 'DETERMINISTIC',
    })
    vi.mocked(api.createReflection).mockResolvedValue({
      reflection: {
        id: 'r-1',
        experimentId: 'e-1',
        content: 'Nothing notable happened today.',
        createdAt: '2026-01-01T00:00:00Z',
      },
      suggestion: {
        id: 's-1',
        experimentId: 'e-1',
        text: 'Optional next step: keep going',
        status: 'PROPOSED',
        createdAt: '2026-01-01T00:00:00Z',
        source: 'DETERMINISTIC',
      },
    })
    vi.mocked(api.proposeMemoryDraft).mockResolvedValue({ source: 'AI', aiProvider: 'openai', aiModel: 'gpt-4o-mini' })

    renderTodayPage()

    await screen.findByLabelText(/Your message/i)
    fireEvent.change(screen.getByLabelText(/Your message/i), { target: { value: 'Nothing much happened.' } })
    fireEvent.click(screen.getByRole('button', { name: 'Send' }))
    await waitFor(() => expect(api.continueReflectionChat).toHaveBeenCalled())
    fireEvent.click(screen.getByRole('button', { name: /I'm done — review my reflection/i }))
    await waitFor(() => expect(api.finishReflectionChat).toHaveBeenCalled())
    fireEvent.click(screen.getByRole('button', { name: 'Save reflection' }))

    await waitFor(() => expect(api.proposeMemoryDraft).toHaveBeenCalledWith('r-1'))
    expect(screen.queryByText(/Something worth remembering about you/i)).not.toBeInTheDocument()
  })

  it('shows a clear connection-required error when chat turn request fails', async () => {
    vi.mocked(api.getCurrentFocus).mockResolvedValue(activeExperimentFocus())
    vi.mocked(api.getWeeklyRetrospectiveDraft).mockResolvedValue(EMPTY_RETROSPECTIVE_DRAFT)
    vi.mocked(api.continueReflectionChat).mockRejectedValue(new Error('network down'))

    renderTodayPage()

    await screen.findByLabelText(/Your message/i)
    fireEvent.change(screen.getByLabelText(/Your message/i), { target: { value: 'I paused once.' } })
    fireEvent.click(screen.getByRole('button', { name: 'Send' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('You need a connection to continue this reflection.')
  })

  it('surfaces a weekly retrospective teaser on Today when there is one to show', async () => {
    vi.mocked(api.getCurrentFocus).mockResolvedValue(activeExperimentFocus())
    vi.mocked(api.getWeeklyRetrospectiveDraft).mockResolvedValue({
      periodStart: '2026-01-01',
      periodEnd: '2026-01-07',
      reflectionSummaries: [{ reflectionId: 'r-1', createdAt: '2026-01-01T00:00:00Z', summary: 'Paused before responding.' }],
      summary: 'You practiced pausing three times this week.',
      assistance: 'Notice how often the pause changed the outcome.',
      source: 'AI',
      aiProvider: 'openai',
      aiModel: 'gpt-4o-mini',
    })

    renderTodayPage()

    expect(await screen.findByText(/You practiced pausing three times this week\./i)).toBeInTheDocument()
    expect(screen.getByText(/AI suggested — openai/i)).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /See full weekly retrospective/i })).toHaveAttribute('href', '/library')
  })

  it('refetches and shows the weekly retrospective teaser after saving the first reflection', async () => {
    vi.mocked(api.getCurrentFocus).mockResolvedValue(activeExperimentFocus())
    vi.mocked(api.getWeeklyRetrospectiveDraft)
      .mockResolvedValueOnce(EMPTY_RETROSPECTIVE_DRAFT)
      .mockResolvedValue({
        ...EMPTY_RETROSPECTIVE_DRAFT,
        reflectionSummaries: [
          { reflectionId: 'r-1', createdAt: '2026-01-01T00:00:00Z', summary: 'Paused before responding.' },
        ],
        summary: 'Your first reflection is now part of this week.',
        assistance: 'Keep noticing what changes after the pause.',
      })
    vi.mocked(api.continueReflectionChat).mockResolvedValue({
      text: 'What did you notice afterward?',
      source: 'DETERMINISTIC',
    })
    vi.mocked(api.finishReflectionChat).mockResolvedValue({
      content: 'I paused before replying.',
      attempted: true,
      noticed: 'The conversation stayed calm.',
      source: 'DETERMINISTIC',
    })
    vi.mocked(api.createReflection).mockResolvedValue({
      reflection: {
        id: 'r-1',
        experimentId: 'e-1',
        content: 'I paused before replying.',
        createdAt: '2026-01-01T00:00:00Z',
      },
      suggestion: {
        id: 's-1',
        experimentId: 'e-1',
        text: 'Try the pause again tomorrow.',
        status: 'PROPOSED',
        createdAt: '2026-01-01T00:00:00Z',
        source: 'DETERMINISTIC',
      },
    })
    vi.mocked(api.proposeMemoryDraft).mockResolvedValue({ source: 'DETERMINISTIC' })

    renderTodayPage()

    await waitFor(() => expect(api.getWeeklyRetrospectiveDraft).toHaveBeenCalledTimes(1))
    expect(screen.queryByRole('heading', { name: 'This week' })).not.toBeInTheDocument()

    fireEvent.change(screen.getByLabelText(/Your message/i), { target: { value: 'I paused before replying.' } })
    fireEvent.click(screen.getByRole('button', { name: 'Send' }))
    await waitFor(() => expect(api.continueReflectionChat).toHaveBeenCalled())
    fireEvent.click(screen.getByRole('button', { name: /I'm done — review my reflection/i }))
    await screen.findByRole('heading', { name: /Review before saving/i })
    fireEvent.click(screen.getByRole('button', { name: 'Save reflection' }))

    expect(await screen.findByText('Your first reflection is now part of this week.')).toBeInTheDocument()
    expect(api.getWeeklyRetrospectiveDraft).toHaveBeenCalledTimes(2)
  })
})
