import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { SearchPage } from './SearchPage'

describe('SearchPage', () => {
  it('shows initial guidance', () => {
    const client = new QueryClient()
    render(
      <QueryClientProvider client={client}>
        <SearchPage />
      </QueryClientProvider>,
    )

    expect(screen.getByText(/Enter at least two characters to search/i)).toBeInTheDocument()
  })
})
