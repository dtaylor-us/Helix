import { afterEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { WisdomPage } from './WisdomPage'
import { api } from '../api/http'

vi.mock('../api/http', () => ({
  api: {
    getWeeklyRetrospectiveDraft: vi.fn(),
    listRetrospectives: vi.fn(),
    saveWeeklyRetrospective: vi.fn(),
    listWisdom: vi.fn(),
    getWisdom: vi.fn(),
    createWisdom: vi.fn(),
    reviseWisdom: vi.fn(),
  },
}))

const DRAFT = {
  periodStart: '2026-01-01',
  periodEnd: '2026-01-07',
  reflectionSummaries: [{ reflectionId: 'r-1', createdAt: '2026-01-01T00:00:00Z', summary: 'Paused before responding.' }],
  summary: 'A calmer week overall.',
  assistance: 'Patterns point toward steadier pauses.',
}

function renderWisdomPage() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={client}>
      <WisdomPage />
    </QueryClientProvider>,
  )
}

describe('WisdomPage', () => {
  afterEach(() => {
    vi.mocked(api.getWeeklyRetrospectiveDraft).mockReset()
    vi.mocked(api.listRetrospectives).mockReset()
    vi.mocked(api.saveWeeklyRetrospective).mockReset()
    vi.mocked(api.listWisdom).mockReset()
    vi.mocked(api.getWisdom).mockReset()
    vi.mocked(api.createWisdom).mockReset()
    vi.mocked(api.reviseWisdom).mockReset()
  })

  it('shows loading state', () => {
    vi.mocked(api.getWeeklyRetrospectiveDraft).mockReturnValue(new Promise(() => {}))
    vi.mocked(api.listRetrospectives).mockReturnValue(new Promise(() => {}))
    vi.mocked(api.listWisdom).mockReturnValue(new Promise(() => {}))

    renderWisdomPage()

    expect(screen.getByText(/Loading wisdom workspace/i)).toBeInTheDocument()
  })

  it('confirms a saved weekly snapshot and shows it in snapshot history', async () => {
    const existingSnapshot = {
      id: 'retro-1',
      periodStart: '2025-12-25',
      periodEnd: '2025-12-31',
      summary: 'Closed the year with more consistency.',
      assistance: 'Steady habits stayed visible.',
      createdAt: '2026-01-01T00:00:00Z',
    }
    const savedSnapshot = {
      id: 'retro-2',
      periodStart: '2026-01-01',
      periodEnd: '2026-01-07',
      summary: 'A calmer week overall.',
      assistance: 'Patterns point toward steadier pauses.',
      createdAt: '2026-01-08T00:00:00Z',
    }

    vi.mocked(api.getWeeklyRetrospectiveDraft).mockResolvedValue(DRAFT)
    vi.mocked(api.listRetrospectives)
      .mockResolvedValueOnce([existingSnapshot])
      .mockResolvedValueOnce([savedSnapshot, existingSnapshot])
    vi.mocked(api.saveWeeklyRetrospective).mockResolvedValue(savedSnapshot)
    vi.mocked(api.listWisdom).mockResolvedValue([])

    renderWisdomPage()

    expect(await screen.findByText(/A calmer week overall\./i)).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: /Saved snapshots/i })).toBeInTheDocument()
    expect(screen.getByText(/2025-12-25 to 2025-12-31/i)).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: /Save weekly snapshot/i }))

    await waitFor(() => expect(api.saveWeeklyRetrospective).toHaveBeenCalledTimes(1))
    expect(await screen.findByText(/Weekly snapshot saved to history\./i)).toBeInTheDocument()
    expect(await screen.findByText(/2026-01-01 to 2026-01-07/i)).toBeInTheDocument()
    await waitFor(() => expect(api.listRetrospectives).toHaveBeenCalledTimes(2))
  })
})
