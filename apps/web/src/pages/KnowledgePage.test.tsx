import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { KnowledgePage } from './KnowledgePage'

describe('KnowledgePage', () => {
  it('shows loading state', () => {
    const client = new QueryClient()
    render(
      <QueryClientProvider client={client}>
        <KnowledgePage />
      </QueryClientProvider>,
    )

    expect(screen.getByText(/Loading knowledge foundations/i)).toBeInTheDocument()
  })
})