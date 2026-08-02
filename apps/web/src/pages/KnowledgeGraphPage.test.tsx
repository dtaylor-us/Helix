import { describe, expect, it, vi, afterEach } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { createMemoryHistory, createRootRoute, createRoute, createRouter, RouterProvider } from '@tanstack/react-router'
import { KnowledgeGraphPage } from './KnowledgeGraphPage'
import { api } from '../api/http'

vi.mock('../api/http', () => ({
  api: {
    getGraphFocus: vi.fn(),
    rebuildGraph: vi.fn(),
    confirmGraphEdge: vi.fn(),
    rejectGraphEdge: vi.fn(),
    hideGraphEdge: vi.fn(),
  },
}))

function renderGraphPage() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  const rootRoute = createRootRoute({})
  const graphRoute = createRoute({
    getParentRoute: () => rootRoute,
    path: '/knowledge-graph/$nodeType/$sourceRecordId',
    component: KnowledgeGraphPage,
  })
  const router = createRouter({
    routeTree: rootRoute.addChildren([graphRoute]),
    history: createMemoryHistory({ initialEntries: ['/knowledge-graph/TRANSFORMATION/t-1'] }),
  })

  return render(
    <QueryClientProvider client={client}>
      <RouterProvider router={router} />
    </QueryClientProvider>,
  )
}

describe('KnowledgeGraphPage', () => {
  afterEach(() => {
    vi.mocked(api.getGraphFocus).mockReset()
    vi.mocked(api.rebuildGraph).mockReset()
    vi.mocked(api.confirmGraphEdge).mockReset()
    vi.mocked(api.rejectGraphEdge).mockReset()
    vi.mocked(api.hideGraphEdge).mockReset()
  })

  it('renders a bounded view with a diagram, list toggle, and node detail on selection', async () => {
    vi.mocked(api.getGraphFocus).mockResolvedValue({
      title: 'Connections for this transformation',
      description: 'Beliefs, experiments, evidence, and wisdom connected to this transformation.',
      focusNodeId: 'n-1',
      nodes: [
        {
          id: 'n-1',
          type: 'TRANSFORMATION',
          label: 'Become steadier under pressure',
          summary: 'Stay calm in conflict',
          sourceRecordId: 't-1',
          sourceRoute: '/transformations/t-1',
          status: 'ACTIVE',
          visualCategory: 'transformation',
        },
        {
          id: 'n-2',
          type: 'BELIEF',
          label: 'I fall apart under pressure',
          summary: undefined,
          sourceRecordId: 'b-1',
          sourceRoute: '/knowledge',
          status: undefined,
          visualCategory: 'belief',
        },
      ],
      edges: [
        {
          id: 'e-1',
          sourceNodeId: 'n-1',
          targetNodeId: 'n-2',
          relationshipType: 'TRANSFORMATION_CONTAINS_BELIEF',
          displayLabel: 'Includes belief',
          origin: 'EXPLICIT_DOMAIN_RELATIONSHIP',
          status: 'CONFIRMED',
          confidence: 'EXPLICIT',
          explanation: 'This belief belongs to this transformation.',
          sourceReferences: [],
        },
      ],
      truncated: false,
    })

    renderGraphPage()

    expect(await screen.findByText('Connections for this transformation')).toBeInTheDocument()
    expect(screen.getByRole('img', { name: /Diagram of connections/i })).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: 'List' }))
    expect(await screen.findByText(/Includes belief/i)).toBeInTheDocument()

    fireEvent.click(screen.getByRole('checkbox', { name: 'Belief' }))
    await waitFor(() => expect(screen.queryByText(/Includes belief/i)).not.toBeInTheDocument())
  })

  it('shows a build-connections action when the projection has not been built yet', async () => {
    vi.mocked(api.getGraphFocus).mockRejectedValue(new Error('No knowledge graph node found -- rebuild the projection.'))
    vi.mocked(api.rebuildGraph).mockResolvedValue({ nodeCount: 0, edgeCount: 0, rebuiltAt: '2026-08-01T00:00:00Z' })

    renderGraphPage()

    expect(await screen.findByRole('button', { name: 'Build connections' })).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Build connections' }))
    await waitFor(() => expect(api.rebuildGraph).toHaveBeenCalled())
  })

  it('shows governance actions only for proposed edges and calls the right mutation', async () => {
    vi.mocked(api.getGraphFocus).mockResolvedValue({
      title: 'Connections',
      focusNodeId: 'n-1',
      nodes: [
        { id: 'n-1', type: 'TRANSFORMATION', label: 'Focus', sourceRecordId: 't-1', visualCategory: 'transformation' },
        { id: 'n-2', type: 'WISDOM', label: 'A proposed lesson', sourceRecordId: 'w-1', visualCategory: 'wisdom' },
      ],
      edges: [
        {
          id: 'e-2',
          sourceNodeId: 'n-1',
          targetNodeId: 'n-2',
          relationshipType: 'TRANSFORMATION_PRODUCED_WISDOM',
          displayLabel: 'Produced lesson',
          origin: 'AI_PROPOSED',
          status: 'PROPOSED',
          confidence: 'MODERATE',
          explanation: 'AI suggested this connection.',
          sourceReferences: [],
        },
      ],
      truncated: false,
    })
    vi.mocked(api.confirmGraphEdge).mockResolvedValue({
      id: 'e-2',
      sourceNodeId: 'n-1',
      targetNodeId: 'n-2',
      relationshipType: 'TRANSFORMATION_PRODUCED_WISDOM',
      displayLabel: 'Produced lesson',
      origin: 'AI_PROPOSED',
      status: 'CONFIRMED',
      confidence: 'MODERATE',
      explanation: 'AI suggested this connection.',
      sourceReferences: [],
    })

    renderGraphPage()

    fireEvent.click(await screen.findByRole('button', { name: 'List' }))
    fireEvent.click(await screen.findByRole('button', { name: 'Confirm' }))

    await waitFor(() => expect(api.confirmGraphEdge).toHaveBeenCalledWith('e-2'))
  })
})
