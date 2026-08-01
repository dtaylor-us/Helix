import { describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { TransformationDetailPage } from './TransformationDetailPage'
import { api } from '../api/http'

vi.mock('@tanstack/react-router', async () => {
  const actual = await vi.importActual<typeof import('@tanstack/react-router')>('@tanstack/react-router')
  return {
    ...actual,
    useParams: () => ({ id: 't-1' }),
  }
})

vi.mock('../api/http', () => ({
  api: {
    getTransformation: vi.fn(),
    proposeExperimentDraft: vi.fn(),
    createExperiment: vi.fn(),
  },
}))

function renderTransformationDetailPage() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={client}>
      <TransformationDetailPage />
    </QueryClientProvider>,
  )
}

describe('TransformationDetailPage', () => {
  it('fills the hypothesis from a deterministic draft and shows fallback provenance', async () => {
    vi.mocked(api.getTransformation).mockResolvedValue({
      id: 't-1',
      title: 'Become more peaceful',
      purpose: 'Practice steadiness',
      desiredIdentity: 'Respond calmly',
      obstacle: 'Feeling rushed',
      createdAt: '2026-01-01T00:00:00Z',
    })
    vi.mocked(api.proposeExperimentDraft).mockResolvedValue({
      title: 'First small step toward Become more peaceful',
      hypothesis: 'A smaller, repeatable action will help me learn what actually moves this transformation forward.',
      nextAction: 'Choose one action you can finish in under ten minutes and try it once today.',
      cadence: 'Once today',
      evidenceOfSuccess: 'You notice one concrete sign that this transformation felt easier to practice.',
      source: 'DETERMINISTIC',
      aiProvider: 'none',
      aiModel: 'deterministic',
    })

    renderTransformationDetailPage()

    await screen.findByText('Become more peaceful')
    fireEvent.click(screen.getByRole('button', { name: /Draft this for me/i }))

    await waitFor(() => expect(api.proposeExperimentDraft).toHaveBeenCalledWith('t-1'))

    expect(await screen.findByLabelText(/What do you want to learn\?/i)).toHaveValue(
      'A smaller, repeatable action will help me learn what actually moves this transformation forward.',
    )
    expect(screen.getByLabelText(/Experiment/i)).toHaveValue('First small step toward Become more peaceful')
    expect(screen.getByLabelText(/Smallest next action/i)).toHaveValue(
      'Choose one action you can finish in under ten minutes and try it once today.',
    )
    expect(screen.getByRole('status')).toHaveTextContent(/Fallback draft — none/i)
  })
})
