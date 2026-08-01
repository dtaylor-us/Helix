import { afterEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { SearchPage } from './SearchPage'
import { api } from '../api/http'

vi.mock('../api/http', () => ({
  api: {
    search: vi.fn(),
  },
}))

function renderSearchPage() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={client}>
      <SearchPage />
    </QueryClientProvider>,
  )
}

describe('SearchPage', () => {
  afterEach(() => {
    vi.mocked(api.search).mockReset()
  })

  it('shows initial guidance', () => {
    renderSearchPage()

    expect(screen.getByText(/Enter at least two characters to search/i)).toBeInTheDocument()
  })

  it('shows an empty state when the API returns no matches', async () => {
    vi.mocked(api.search).mockResolvedValue({
      note: 'Keyword and semantic search are available.',
      results: [],
    })

    renderSearchPage()
    fireEvent.change(screen.getByLabelText(/Keyword/i), { target: { value: 'zzzz-no-such-helix-record' } })

    expect(await screen.findByText(/No matches found\./i)).toBeInTheDocument()
  })
})
