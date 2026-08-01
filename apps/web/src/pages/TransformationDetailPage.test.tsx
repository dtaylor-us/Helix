import { afterEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { RouterProvider, createRootRoute, createRoute, createRouter } from '@tanstack/react-router'
import { TransformationDetailPage } from './TransformationDetailPage'
import { api } from '../api/http'

vi.mock('../api/http', () => ({
  api: {
    getTransformation: vi.fn(),
    createExperiment: vi.fn(),
  },
}))

function renderTransformationDetailPage() {
  window.history.pushState({}, '', '/transformations/t-1')

  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  const rootRoute = createRootRoute({})
  const detailRoute = createRoute({
    getParentRoute: () => rootRoute,
    path: '/transformations/$id',
    component: TransformationDetailPage,
  })
  const router = createRouter({
    routeTree: rootRoute.addChildren([detailRoute]),
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
  })

  it('confirms the saved experiment locally and shows the current active experiment summary', async () => {
    vi.mocked(api.getTransformation).mockResolvedValue({
      id: 't-1',
      title: 'Become more peaceful',
      purpose: 'Respond with more calm',
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

    renderTransformationDetailPage()

    await screen.findByText('Become more peaceful')

    fireEvent.change(screen.getByLabelText('Experiment'), { target: { value: 'Pause before responding' } })
    fireEvent.change(screen.getByLabelText('What do you want to learn?'), {
      target: { value: 'Pausing will help me stay grounded' },
    })
    fireEvent.change(screen.getByLabelText('Smallest next action'), {
      target: { value: 'Take one breath' },
    })
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

    expect(await screen.findByText(/Saved "Pause before responding" as your current active experiment./i)).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Current active experiment' })).toBeInTheDocument()
    expect(screen.getByText('Pause before responding')).toBeInTheDocument()
    expect(screen.getByText('Pausing will help me stay grounded')).toBeInTheDocument()
    expect(screen.getByText('Next action: Take one breath')).toBeInTheDocument()
    expect(screen.getByLabelText('Experiment')).toHaveValue('')
    expect(screen.getByLabelText('What do you want to learn?')).toHaveValue('')
    expect(screen.getByLabelText('Smallest next action')).toHaveValue('')
  })
})
