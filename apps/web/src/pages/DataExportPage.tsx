import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { api } from '../api/http'

const DELETE_CONFIRMATION_PHRASE = 'DELETE'

export function DataExportPage() {
  const queryClient = useQueryClient()
  const [exportStatusText, setExportStatusText] = useState<string | null>(null)
  const [exportErrorText, setExportErrorText] = useState<string | null>(null)
  const [confirmationInput, setConfirmationInput] = useState('')
  const [deleteStatusText, setDeleteStatusText] = useState<string | null>(null)
  const [deleteErrorText, setDeleteErrorText] = useState<string | null>(null)

  const exportMutation = useMutation({
    mutationFn: api.exportData,
    onSuccess: (data) => {
      // Round-tripped through JSON.stringify rather than the raw response body so the
      // downloaded file is always pretty-printed, regardless of how the server formats it.
      const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' })
      const url = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = `helix-export-${new Date().toISOString().slice(0, 10)}.json`
      document.body.appendChild(link)
      link.click()
      link.remove()
      URL.revokeObjectURL(url)
      setExportStatusText('Your data was downloaded.')
      setExportErrorText(null)
    },
    onError: () => {
      setExportStatusText(null)
      setExportErrorText('Could not export your data. Please try again.')
    },
  })

  const deleteMutation = useMutation({
    mutationFn: api.deleteAllData,
    onSuccess: () => {
      setDeleteStatusText('Everything has been permanently deleted.')
      setDeleteErrorText(null)
      setConfirmationInput('')
      // Every cached response is now stale in the strongest possible sense -- the underlying
      // records are gone, not just changed -- so a full cache clear is correct here rather than
      // invalidating individual query keys one at a time.
      queryClient.clear()
    },
    onError: () => {
      setDeleteStatusText(null)
      setDeleteErrorText('Could not delete your data. Please try again.')
    },
  })

  const canDelete = confirmationInput === DELETE_CONFIRMATION_PHRASE

  return (
    <div className="stack">
      <section className="card">
        <h2>Export your data</h2>
        <p className="muted">
          Download a complete, human-readable copy of everything you&rsquo;ve recorded in Helix
          &mdash; transformations, experiments, reflections, beliefs, evidence, wisdom,
          retrospectives, and memory.
        </p>
        <button onClick={() => exportMutation.mutate()} disabled={exportMutation.isPending}>
          {exportMutation.isPending ? 'Preparing your export...' : 'Download my data'}
        </button>
        {exportStatusText && (
          <p role="status" aria-live="polite" className="muted">
            {exportStatusText}
          </p>
        )}
        {exportErrorText && <p role="alert">{exportErrorText}</p>}
      </section>

      <section className="card">
        <h2>Delete everything</h2>
        <p className="muted">
          This permanently deletes every transformation, experiment, reflection, belief, piece of
          evidence, wisdom entry, retrospective, and memory in Helix. There is no undo. Download
          your data above first if you want to keep a copy.
        </p>
        <label htmlFor="delete-confirmation-input">Type DELETE to confirm</label>
        <input
          id="delete-confirmation-input"
          value={confirmationInput}
          onChange={(e) => {
            setConfirmationInput(e.target.value)
            setDeleteStatusText(null)
            setDeleteErrorText(null)
          }}
        />
        <div className="row">
          <button onClick={() => deleteMutation.mutate()} disabled={!canDelete || deleteMutation.isPending}>
            {deleteMutation.isPending ? 'Deleting...' : 'Delete everything'}
          </button>
        </div>
        {deleteStatusText && (
          <p role="status" aria-live="polite" className="muted">
            {deleteStatusText}
          </p>
        )}
        {deleteErrorText && <p role="alert">{deleteErrorText}</p>}
      </section>
    </div>
  )
}
