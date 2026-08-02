import { describe, expect, it } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
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
    const moreSummary = screen.getByText('More')
    fireEvent.click(moreSummary)
    expect(screen.getByRole('link', { name: 'Settings' })).toBeInTheDocument()
  })

  it('closes the More menu when focus moves outside it', async () => {
    renderShell()

    const moreSummary = await screen.findByText('More')
    const menu = moreSummary.closest('details')
    fireEvent.click(moreSummary)
    expect(menu).toHaveAttribute('open')

    fireEvent.focusIn(screen.getByRole('link', { name: 'Library' }))
    await waitFor(() => expect(menu).not.toHaveAttribute('open'))
  })

  it('closes the More menu with Escape', async () => {
    renderShell()

    const moreSummary = await screen.findByText('More')
    const menu = moreSummary.closest('details')
    fireEvent.click(moreSummary)
    fireEvent.keyDown(document, { key: 'Escape' })

    await waitFor(() => expect(menu).not.toHaveAttribute('open'))
    expect(moreSummary).toHaveFocus()
  })
})
