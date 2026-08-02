import { describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { createRootRoute, createRoute, createRouter, RouterProvider } from '@tanstack/react-router'
import { AuthGate } from './AuthGate'
import { api } from '../api/http'

vi.mock('../api/http', () => ({
  api: {
    getCurrentUser: vi.fn(),
    logout: vi.fn(),
  },
  googleLoginUrl: 'http://localhost:8080/oauth2/authorization/google',
}))

function renderGate() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  const rootRoute = createRootRoute({ component: AuthGate })
  const indexRoute = createRoute({ getParentRoute: () => rootRoute, path: '/', component: () => <p>content</p> })
  const router = createRouter({ routeTree: rootRoute.addChildren([indexRoute]) })
  return render(
    <QueryClientProvider client={queryClient}>
      <RouterProvider router={router} />
    </QueryClientProvider>,
  )
}

describe('AuthGate', () => {
  it('shows a Google sign-in link when there is no session', async () => {
    vi.mocked(api.getCurrentUser).mockRejectedValue(new Error('Not signed in'))

    renderGate()

    const link = await screen.findByRole('link', { name: 'Sign in with Google' })
    expect(link).toHaveAttribute('href', 'http://localhost:8080/oauth2/authorization/google')
  })

  it('renders the app once a session exists', async () => {
    vi.mocked(api.getCurrentUser).mockResolvedValue({
      id: 'u-1',
      email: 'derektaylor.us@gmail.com',
      displayName: 'Derek Taylor',
    })

    renderGate()

    expect(await screen.findByText('Derek Taylor')).toBeInTheDocument()
    expect(screen.getByText('content')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Sign out' })).toBeInTheDocument()
  })
})
