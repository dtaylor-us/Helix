import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { DataExportPage } from './DataExportPage'
import { api } from '../api/http'

vi.mock('../api/http', () => ({
  api: {
    exportData: vi.fn(),
    deleteAllData: vi.fn(),
  },
}))

function renderDataExportPage() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={client}>
      <DataExportPage />
    </QueryClientProvider>,
  )
}

describe('DataExportPage', () => {
  beforeEach(() => {
    // jsdom doesn't implement the Blob/object-URL download machinery the export button drives.
    URL.createObjectURL = vi.fn(() => 'blob:mock-url')
    URL.revokeObjectURL = vi.fn()
  })

  afterEach(() => {
    vi.mocked(api.exportData).mockReset()
    vi.mocked(api.deleteAllData).mockReset()
  })

  it('downloads the export bundle and shows a confirmation', async () => {
    vi.mocked(api.exportData).mockResolvedValue({
      onboardingStatus: 'COMPLETE',
      transformations: [],
      experiments: [],
      reflections: [],
      suggestions: [],
      beliefs: [],
      beliefRevisions: [],
      evidence: [],
      weeklyRetrospectives: [],
      wisdomEntries: [],
      wisdomRevisions: [],
      wisdomSourceLinks: [],
      memoryProposals: [],
      memoryProposalRevisions: [],
    })

    renderDataExportPage()
    fireEvent.click(screen.getByRole('button', { name: 'Download my data' }))

    await waitFor(() => expect(api.exportData).toHaveBeenCalled())
    expect(await screen.findByText(/Your data was downloaded/i)).toBeInTheDocument()
    expect(URL.createObjectURL).toHaveBeenCalled()
    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:mock-url')
  })

  it('shows an error if export fails', async () => {
    vi.mocked(api.exportData).mockRejectedValue(new Error('network down'))

    renderDataExportPage()
    fireEvent.click(screen.getByRole('button', { name: 'Download my data' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Could not export your data. Please try again.')
  })

  it('keeps the delete button disabled until the confirmation phrase is typed exactly', async () => {
    renderDataExportPage()

    const deleteButton = screen.getByRole('button', { name: 'Delete everything' })
    expect(deleteButton).toBeDisabled()

    fireEvent.change(screen.getByLabelText('Type DELETE to confirm'), { target: { value: 'delete' } })
    expect(deleteButton).toBeDisabled()

    fireEvent.change(screen.getByLabelText('Type DELETE to confirm'), { target: { value: 'DELETE' } })
    expect(deleteButton).not.toBeDisabled()
  })

  it('deletes everything after confirmation and clears the confirmation input', async () => {
    vi.mocked(api.deleteAllData).mockResolvedValue(undefined)

    renderDataExportPage()
    fireEvent.change(screen.getByLabelText('Type DELETE to confirm'), { target: { value: 'DELETE' } })
    fireEvent.click(screen.getByRole('button', { name: 'Delete everything' }))

    await waitFor(() => expect(api.deleteAllData).toHaveBeenCalled())
    expect(await screen.findByText(/Everything has been permanently deleted/i)).toBeInTheDocument()
    expect(screen.getByLabelText('Type DELETE to confirm')).toHaveValue('')
    expect(screen.getByRole('button', { name: 'Delete everything' })).toBeDisabled()
  })

  it('shows an error if deletion fails', async () => {
    vi.mocked(api.deleteAllData).mockRejectedValue(new Error('server down'))

    renderDataExportPage()
    fireEvent.change(screen.getByLabelText('Type DELETE to confirm'), { target: { value: 'DELETE' } })
    fireEvent.click(screen.getByRole('button', { name: 'Delete everything' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Could not delete your data. Please try again.')
  })
})
