import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { MemoryPage } from './MemoryPage'
import { api } from '../api/http'

vi.mock('../api/http', () => ({
  api: {
    listMemoryProposals: vi.fn(),
    getMemoryProposal: vi.fn(),
    createMemoryProposal: vi.fn(),
    reviseMemoryProposal: vi.fn(),
    acceptMemoryProposal: vi.fn(),
    rejectMemoryProposal: vi.fn(),
    deleteMemoryProposal: vi.fn(),
  },
}))

function renderMemoryPage() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={client}>
      <MemoryPage />
    </QueryClientProvider>,
  )
}

describe('MemoryPage', () => {
  afterEach(() => {
    vi.mocked(api.listMemoryProposals).mockReset()
    vi.mocked(api.getMemoryProposal).mockReset()
    vi.mocked(api.createMemoryProposal).mockReset()
    vi.mocked(api.reviseMemoryProposal).mockReset()
    vi.mocked(api.acceptMemoryProposal).mockReset()
    vi.mocked(api.rejectMemoryProposal).mockReset()
    vi.mocked(api.deleteMemoryProposal).mockReset()
  })

  it('shows loading state', () => {
    vi.mocked(api.listMemoryProposals).mockReturnValue(new Promise(() => {}))
    renderMemoryPage()

    expect(screen.getByText(/Loading memory governance/i)).toBeInTheDocument()
  })

  it('shows a source validation error when proposal creation fails', async () => {
    vi.mocked(api.listMemoryProposals).mockResolvedValue([])
    vi.mocked(api.createMemoryProposal).mockRejectedValue(
      new Error("That source record couldn't be found — check the ID/type and try again."),
    )

    renderMemoryPage()

    await screen.findByText(/No memory proposals yet/i)

    fireEvent.change(screen.getByLabelText(/Memory statement/i), {
      target: { value: 'Small actions protect consistency.' },
    })
    fireEvent.change(screen.getByLabelText(/Source record id/i), {
      target: { value: '11111111-1111-1111-1111-111111111111' },
    })
    fireEvent.click(screen.getByRole('button', { name: /Save proposed memory/i }))

    await waitFor(() => {
      expect(screen.getByRole('alert')).toHaveTextContent("That source record couldn't be found — check the ID/type and try again.")
    })
  })
})