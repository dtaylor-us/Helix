import { describe, expect, it, vi, afterEach } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { RouterProvider, createRootRoute, createRoute, createRouter } from '@tanstack/react-router'
import { TodayPage } from './TodayPage'
import { api } from '../api/http'

vi.mock('../api/http', () => ({
  api: {
    getToday: vi.fn(),
    listTransformations: vi.fn(),
    createReflection: vi.fn(),
    acceptSuggestion: vi.fn(),
    dismissSuggestion: vi.fn(),
    replaceSuggestion: vi.fn(),
    getWeeklyRetrospectiveDraft: vi.fn(),
    createWisdom: vi.fn(),
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
    vi.mocked(api.getToday).mockReset()
    vi.mocked(api.listTransformations).mockReset()
    vi.mocked(api.createReflection).mockReset()
    vi.mocked(api.acceptSuggestion).mockReset()
    vi.mocked(api.dismissSuggestion).mockReset()
    vi.mocked(api.replaceSuggestion).mockReset()
    vi.mocked(api.getWeeklyRetrospectiveDraft).mockReset()
    vi.mocked(api.createWisdom).mockReset()
  })

  it('shows loading state', async () => {
    vi.mocked(api.getToday).mockReturnValue(new Promise(() => {}))
    vi.mocked(api.listTransformations).mockReturnValue(new Promise(() => {}))
    renderTodayPage()

    expect(await screen.findByText(/Loading your active context/i)).toBeInTheDocument()
  })

  it('shows a welcome/first-use state when there are no transformations yet', async () => {
    vi.mocked(api.getToday).mockResolvedValue({
      hasActiveExperiment: false,
      activeExperiment: null,
      reflectionHistory: [],
      suggestionHistory: [],
    })
    vi.mocked(api.listTransformations).mockResolvedValue([])

    renderTodayPage()

    expect(await screen.findByText(/Welcome to Helix/i)).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /Begin my first transformation/i })).toBeInTheDocument()
  })

  it('shows a direct call to action when a transformation exists but no experiment is active', async () => {
    vi.mocked(api.getToday).mockResolvedValue({
      hasActiveExperiment: false,
      activeExperiment: null,
      reflectionHistory: [],
      suggestionHistory: [],
    })
    vi.mocked(api.listTransformations).mockResolvedValue([
      { id: 't-1', title: 'Become more peaceful', createdAt: '2026-01-01T00:00:00Z' },
    ])
    vi.mocked(api.getWeeklyRetrospectiveDraft).mockResolvedValue(EMPTY_RETROSPECTIVE_DRAFT)

    renderTodayPage()

    expect(await screen.findByRole('link', { name: /Add an experiment to/i })).toBeInTheDocument()
  })

  it('orders the suggested action above the reflection prompt and does not leak roadmap language', async () => {
    vi.mocked(api.getToday).mockResolvedValue({
      hasActiveExperiment: true,
      activeExperiment: {
        id: 'e-1',
        transformationId: 't-1',
        title: 'Pause before responding',
        hypothesis: 'Pausing helps me respond calmly',
        status: 'ACTIVE',
        createdAt: '2026-01-01T00:00:00Z',
      },
      reflectionHistory: [],
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
    })
    vi.mocked(api.listTransformations).mockResolvedValue([
      { id: 't-1', title: 'Become more peaceful', createdAt: '2026-01-01T00:00:00Z' },
    ])
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

  it('reveals progressive follow-up questions one at a time once the main answer has content, and submits them all', async () => {
    vi.mocked(api.getToday).mockResolvedValue({
      hasActiveExperiment: true,
      activeExperiment: {
        id: 'e-1',
        transformationId: 't-1',
        title: 'Pause before responding',
        hypothesis: 'Pausing helps me respond calmly',
        status: 'ACTIVE',
        createdAt: '2026-01-01T00:00:00Z',
      },
      reflectionHistory: [],
      suggestionHistory: [],
    })
    vi.mocked(api.listTransformations).mockResolvedValue([
      { id: 't-1', title: 'Become more peaceful', createdAt: '2026-01-01T00:00:00Z' },
    ])
    vi.mocked(api.getWeeklyRetrospectiveDraft).mockResolvedValue(EMPTY_RETROSPECTIVE_DRAFT)
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

    renderTodayPage()

    // No follow-up prompts before there is any main answer.
    await screen.findByLabelText(/What happened/i)
    expect(screen.queryByText(/What did you notice internally\?/i)).not.toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: 'Yes' }))
    fireEvent.change(screen.getByLabelText(/What happened/i), { target: { value: 'I paused twice today.' } })

    // Follow-ups reveal one at a time.
    fireEvent.click(screen.getByRole('button', { name: /\+ What did you notice internally\?/i }))
    fireEvent.change(screen.getByLabelText('What did you notice internally?'), {
      target: { value: 'My shoulders were tense.' },
    })
    expect(screen.queryByLabelText('What evidence did this give you?')).not.toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: /\+ What evidence did this give you\?/i }))
    fireEvent.change(screen.getByLabelText('What evidence did this give you?'), {
      target: { value: 'The conversation stayed calmer.' },
    })

    fireEvent.click(screen.getByRole('button', { name: 'Save reflection' }))

    await waitFor(() =>
      expect(api.createReflection).toHaveBeenCalledWith('e-1', {
        content: 'I paused twice today.',
        attempted: true,
        noticed: 'My shoulders were tense.',
        evidenceNoted: 'The conversation stayed calmer.',
        surprise: undefined,
      }),
    )

    expect(await screen.findByText(/This might be useful evidence/i)).toBeInTheDocument()
  })

  it('offers a contextual wisdom prompt prefilled from the most specific answer, and saves it', async () => {
    vi.mocked(api.getToday).mockResolvedValue({
      hasActiveExperiment: true,
      activeExperiment: {
        id: 'e-1',
        transformationId: 't-1',
        title: 'Pause before responding',
        hypothesis: 'Pausing helps me respond calmly',
        status: 'ACTIVE',
        createdAt: '2026-01-01T00:00:00Z',
      },
      reflectionHistory: [],
      suggestionHistory: [],
    })
    vi.mocked(api.listTransformations).mockResolvedValue([
      { id: 't-1', title: 'Become more peaceful', createdAt: '2026-01-01T00:00:00Z' },
    ])
    vi.mocked(api.getWeeklyRetrospectiveDraft).mockResolvedValue(EMPTY_RETROSPECTIVE_DRAFT)
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

    renderTodayPage()

    await screen.findByLabelText(/What happened/i)
    expect(screen.queryByText(/This reflection may contain a lesson worth keeping/i)).not.toBeInTheDocument()

    fireEvent.change(screen.getByLabelText(/What happened/i), { target: { value: 'I paused twice today.' } })
    fireEvent.click(screen.getByRole('button', { name: /\+ What did you notice internally\?/i }))
    fireEvent.change(screen.getByLabelText('What did you notice internally?'), {
      target: { value: 'My shoulders were tense.' },
    })
    fireEvent.click(screen.getByRole('button', { name: /\+ What evidence did this give you\?/i }))
    fireEvent.change(screen.getByLabelText('What evidence did this give you?'), {
      target: { value: 'The conversation stayed calmer.' },
    })
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

  it('surfaces a weekly retrospective teaser on Today when there is one to show', async () => {
    vi.mocked(api.getToday).mockResolvedValue({
      hasActiveExperiment: true,
      activeExperiment: {
        id: 'e-1',
        transformationId: 't-1',
        title: 'Pause before responding',
        hypothesis: 'Pausing helps me respond calmly',
        status: 'ACTIVE',
        createdAt: '2026-01-01T00:00:00Z',
      },
      reflectionHistory: [],
      suggestionHistory: [],
    })
    vi.mocked(api.listTransformations).mockResolvedValue([
      { id: 't-1', title: 'Become more peaceful', createdAt: '2026-01-01T00:00:00Z' },
    ])
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
})
