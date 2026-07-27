import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { WisdomPage } from './WisdomPage'

describe('WisdomPage', () => {
  it('shows loading state', () => {
    const client = new QueryClient()
    render(
      <QueryClientProvider client={client}>
        <WisdomPage />
      </QueryClientProvider>,
    )

    expect(screen.getByText(/Loading wisdom workspace/i)).toBeInTheDocument()
  })
})
