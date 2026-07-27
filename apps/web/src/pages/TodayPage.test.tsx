import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { TodayPage } from './TodayPage'

describe('TodayPage', () => {
  it('shows loading state', () => {
    const client = new QueryClient()
    render(
      <QueryClientProvider client={client}>
        <TodayPage />
      </QueryClientProvider>,
    )

    expect(screen.getByText(/Loading your active context/i)).toBeInTheDocument()
  })
})
