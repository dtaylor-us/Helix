import { describe, expect, it, vi, afterEach } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { createMemoryHistory, createRootRoute, createRoute, createRouter, RouterProvider } from '@tanstack/react-router'
import { TransformationDetailPage } from './TransformationDetailPage'
import { api } from '../api/http'

vi.mock('../api/http', () => ({
  api: {
    getTransformation: vi.fn(),
    createExperiment: vi.fn(),
    proposeExperimentDraft: vi.fn(),
  },
}))

function renderDetailPage() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  const rootRoute = createRootRoute({})
  const detailRoute = createRoute({
    getParentRoute: () => rootRoute,
    path: '/transformations/$id',
    component: TransformationDetailPage,
  })
  const router = createRouter({
    routeTree: rootRoute.addChildren([detailRoute]),
    history: createMemoryHistory({ initialEntries: ['/transformations/t-1'] }),
  })

  return render(
    <QueryClientProvider client={client}>
      <RouterProvider router={router} />
    </QueryClientProvider>,
  )
}

describe('TransformationDetailPage', () => {
  afterEach(() => {
    vi.mocked(api.getTransformation).mockReset()
    vi.mocked(api.createExperiment).mockReset()
    vi.mocked(api.proposeExperimentDraft).mockReset()
  })

  it('offers to draft an experiment with AI and prefills the form for review before saving', async () => {
    vi.mocked(api.getTransformation).mockResolvedValue({
      id: 't-1',
      title: 'Become more peaceful',
      purpose: 'Practice steadiness',
      createdAt: '2026-01-01T00:00:00Z',
    })
    vi.mocked(api.proposeExperimentDraft).mockResolvedValue({
      title: 'Pause before responding',
      hypothesis: 'If I pause, I respond more calmly',
      nextAction: 'Take one breath before replying',
      cadence: 'Whenever I feel criticized',
      evidenceOfSuccess: 'Fewer moments of regret',
      source: 'AI',
      aiProvider: 'openai',
      aiModel: 'gpt-4o-mini',
    })

    renderDetailPage()

    fireEvent.click(await screen.findByRole('button', { name: 'Draft this for me' }))

    await waitFor(() => expect(screen.getByLabelText('Experiment')).toHaveValue('Pause before responding'))
    expect(screen.getByLabelText('What do you want to learn?')).toHaveValue('If I pause, I respond more calmly')
    expect(screen.getByLabelText('Smallest next action')).toHaveValue('Take one breath before replying')
    expect(await screen.findByText(/Drafted by AI \(openai\)/i)).toBeInTheDocument()

    // Nothing is created until the user explicitly saves the (editable) draft.
    expect(api.createExperiment).not.toHaveBeenCalled()
  })

  it('lets the user save the experiment normally without drafting with AI', async () => {
    vi.mocked(api.getTransformation).mockResolvedValue({
      id: 't-1',
      title: 'Become more peaceful',
      purpose: 'Practice steadiness',
      createdAt: '2026-01-01T00:00:00Z',
    })
    vi.mocked(api.createExperiment).mockResolvedValue({
      id: 'e-1',
      transformationId: 't-1',
      title: 'Pause before responding',
      hypothesis: 'Pausing will help me stay grounded',
      nextAction: 'Take one breath',
      status: 'ACTIVE',
      createdAt: '2026-01-01T00:00:00Z',
    })

    renderDetailPage()

    fireEvent.change(await screen.findByLabelText('Experiment'), { target: { value: 'Pause before responding' } })
    fireEvent.change(screen.getByLabelText('What do you want to learn?'), {
      target: { value: 'Pausing will help me stay grounded' },
    })
    fireEvent.change(screen.getByLabelText('Smallest next action'), { target: { value: 'Take one breath' } })
    fireEvent.click(screen.getByRole('button', { name: 'Save experiment' }))

    await waitFor(() =>
      expect(api.createExperiment).toHaveBeenCalledWith('t-1', {
        title: 'Pause before responding',
        hypothesis: 'Pausing will help me stay grounded',
        nextAction: 'Take one breath',
        cadence: '',
        evidenceOfSuccess: '',
        reviewAt: undefined,
      }),
    )
    expect(api.proposeExperimentDraft).not.toHaveBeenCalled()
    expect(await screen.findByText(/Saved "Pause before responding" as your current active experiment./i)).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Current active experiment' })).toBeInTheDocument()
    expect(screen.getByText('Pausing will help me stay grounded')).toBeInTheDocument()
    expect(screen.getByText('Next action: Take one breath')).toBeInTheDocument()
    expect(screen.getByLabelText('Experiment')).toHaveValue('')
    expect(screen.getByLabelText('What do you want to learn?')).toHaveValue('')
    expect(screen.getByLabelText('Smallest next action')).toHaveValue('')
  })
})
