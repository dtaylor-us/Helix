import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { MemoryPage } from './MemoryPage'

describe('MemoryPage', () => {
  it('shows loading state', () => {
    const client = new QueryClient()
    render(
      <QueryClientProvider client={client}>
        <MemoryPage />
      </QueryClientProvider>,
    )

    expect(screen.getByText(/Loading memory governance/i)).toBeInTheDocument()
  })
})