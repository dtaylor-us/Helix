import { describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen } from '@testing-library/react'
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

  it('shows inline guidance when the transformation title is blank', async () => {
    vi.mocked(api.listTransformations).mockResolvedValue([])

    renderTransformationsPage()

    const titleInput = await screen.findByLabelText(/What would you love to become or experience\?/i)
    expect(titleInput).toHaveAttribute('aria-invalid', 'true')
    expect(titleInput).toHaveAttribute('aria-describedby', 'title-helper')
    expect(screen.getByText('Add a title to save your transformation.')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Save transformation' })).toBeDisabled()

    fireEvent.change(titleInput, { target: { value: 'Become steadier with feedback' } })

    expect(titleInput).toHaveAttribute('aria-invalid', 'false')
    expect(titleInput).not.toHaveAttribute('aria-describedby')
    expect(screen.queryByText('Add a title to save your transformation.')).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Save transformation' })).toBeEnabled()
  })
})
