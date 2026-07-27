import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import { createRootRoute, createRoute, createRouter, RouterProvider } from '@tanstack/react-router'
import { AppLayout } from './AppLayout'

function renderShell() {
  const rootRoute = createRootRoute({ component: AppLayout })
  const indexRoute = createRoute({ getParentRoute: () => rootRoute, path: '/', component: () => <p>content</p> })
  const router = createRouter({ routeTree: rootRoute.addChildren([indexRoute]) })
  return render(<RouterProvider router={router} />)
}

describe('AppLayout', () => {
  it('shows a reduced primary navigation and a skip link', async () => {
    renderShell()

    expect(await screen.findByRole('link', { name: 'Skip to content' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Today' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Journey' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Library' })).toBeInTheDocument()
    // Secondary destinations are tucked behind "More" rather than competing for primary attention.
    expect(screen.getByText('More')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Settings' })).toBeInTheDocument()
  })
})
