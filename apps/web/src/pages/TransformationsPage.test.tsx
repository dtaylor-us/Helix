import { describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { createRootRoute, createRoute, createRouter, RouterProvider } from '@tanstack/react-router'
import { TransformationsPage } from './TransformationsPage'
import { api } from '../api/http'

vi.mock('../api/http', () => ({
  api: {
    listTransformations: vi.fn(),
    createTransformation: vi.fn(),
  },
}))

function renderTransformationsPage() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  const rootRoute = createRootRoute({})
  const indexRoute = createRoute({
    getParentRoute: () => rootRoute,
    path: '/',
    component: TransformationsPage,
  })
  const detailRoute = createRoute({
    getParentRoute: () => rootRoute,
    path: '/transformations/$id',
    component: () => null,
  })
  const router = createRouter({ routeTree: rootRoute.addChildren([indexRoute, detailRoute]) })

  return render(
    <QueryClientProvider client={client}>
      <RouterProvider router={router} />
    </QueryClientProvider>,
  )
}

describe('TransformationsPage', () => {
  it('offers guided prompts for desired identity and obstacle alongside title and purpose', async () => {
    vi.mocked(api.listTransformations).mockResolvedValue([])

    renderTransformationsPage()

    expect(await screen.findByLabelText(/What would you love to become or experience\?/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/Why does this matter to you right now\?/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/Who are you becoming through this\?/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/What currently gets in the way\?/i)).toBeInTheDocument()
  })
})
