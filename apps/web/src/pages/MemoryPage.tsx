import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { api } from '../api/http'
import { TermHint } from '../components/TermHint'

const SOURCE_KINDS = ['AI_DERIVED', 'REFLECTION', 'MANUAL_ENTRY'] as const
const SOURCE_RECORD_TYPES = ['REFLECTION', 'EXPERIMENT', 'BELIEF', 'EVIDENCE', 'WISDOM', 'RETROSPECTIVE'] as const

export function MemoryPage() {
  const queryClient = useQueryClient()
  const [statement, setStatement] = useState('')
  const [sourceKind, setSourceKind] = useState<(typeof SOURCE_KINDS)[number]>('AI_DERIVED')
  const [sourceRecordType, setSourceRecordType] = useState<(typeof SOURCE_RECORD_TYPES)[number]>('REFLECTION')
  const [sourceRecordId, setSourceRecordId] = useState('')
  const [sourceExcerpt, setSourceExcerpt] = useState('')
  const [selectedMemoryId, setSelectedMemoryId] = useState('')
  const [revisionStatement, setRevisionStatement] = useState('')
  const [revisionReason, setRevisionReason] = useState('')
  const [revisionExcerpt, setRevisionExcerpt] = useState('')
  const [reviewReason, setReviewReason] = useState('')

  const memoryQuery = useQuery({
    queryKey: ['memory-proposals'],
    queryFn: api.listMemoryProposals,
  })

  const activeMemoryId = selectedMemoryId || memoryQuery.data?.[0]?.id || ''

  const memoryDetailQuery = useQuery({
    queryKey: ['memory-proposals', activeMemoryId],
    queryFn: () => api.getMemoryProposal(activeMemoryId),
    enabled: activeMemoryId.length > 0,
  })

  const createMemoryProposal = useMutation({
    mutationFn: () =>
      api.createMemoryProposal({
        statement,
        sourceKind,
        sourceRecordType,
        sourceRecordId,
        sourceExcerpt: sourceExcerpt || undefined,
      }),
    onSuccess: (entry) => {
      setStatement('')
      setSourceRecordId('')
      setSourceExcerpt('')
      setReviewReason('')
      queryClient.invalidateQueries({ queryKey: ['memory-proposals'] })
      setSelectedMemoryId(entry.id)
    },
  })

  const reviseMemoryProposal = useMutation({
    mutationFn: () =>
      api.reviseMemoryProposal(activeMemoryId, {
        statement: revisionStatement,
        reason: revisionReason,
        sourceExcerpt: revisionExcerpt || undefined,
      }),
    onSuccess: () => {
      setRevisionReason('')
      queryClient.invalidateQueries({ queryKey: ['memory-proposals'] })
      queryClient.invalidateQueries({ queryKey: ['memory-proposals', activeMemoryId] })
    },
  })

  const reviewMemoryProposal = useMutation({
    mutationFn: (status: 'accept' | 'reject') => {
      const payload = { reason: reviewReason.trim() }
      return status === 'accept'
        ? api.acceptMemoryProposal(activeMemoryId, payload)
        : api.rejectMemoryProposal(activeMemoryId, payload)
    },
    onSuccess: () => {
      setReviewReason('')
      queryClient.invalidateQueries({ queryKey: ['memory-proposals'] })
      queryClient.invalidateQueries({ queryKey: ['memory-proposals', activeMemoryId] })
    },
  })

  const deleteMemoryProposal = useMutation({
    mutationFn: () => api.deleteMemoryProposal(activeMemoryId),
    onSuccess: () => {
      setSelectedMemoryId('')
      queryClient.invalidateQueries({ queryKey: ['memory-proposals'] })
    },
  })

  if (memoryQuery.isLoading) {
    return <p>Loading memory governance...</p>
  }

  if (!memoryQuery.data) {
    return <p>Memory governance is unavailable right now.</p>
  }

  const canCreate = Boolean(statement.trim() && sourceRecordId.trim())
  const selectedDetail = memoryDetailQuery.data
  const createMemoryError = createMemoryProposal.error instanceof Error ? createMemoryProposal.error.message : null

  return (
    <div className="stack">
      <section className="card stack">
        <h2>Memory governance</h2>
        <p>AI-derived memory stays proposed until you review it. Every memory proposal keeps source provenance attached.</p>
        <p className="muted">Accepted memories remain editable, but edits return the item to proposed status for re-review.</p>
        <TermHint term="Memory" />
      </section>

      <section className="card stack">
        <h2>Propose memory</h2>
        <p className="muted">Most memories begin as suggestions during reflection. Use this form when you intentionally want to add one yourself.</p>
        <details className="disclosure">
          <summary>Create a memory proposal</summary>
          <div className="stack disclosure-content">
        <label htmlFor="memory-statement">Memory statement</label>
        <textarea id="memory-statement" rows={3} value={statement} onChange={(event) => setStatement(event.target.value)} />

        <label htmlFor="memory-source-kind">Source kind</label>
        <select id="memory-source-kind" value={sourceKind} onChange={(event) => setSourceKind(event.target.value as typeof sourceKind)}>
          {SOURCE_KINDS.map((kind) => (
            <option key={kind} value={kind}>
              {kind.replace('_', ' ').toLowerCase()}
            </option>
          ))}
        </select>

        <label htmlFor="memory-source-record-type">Source record type</label>
        <select
          id="memory-source-record-type"
          value={sourceRecordType}
          onChange={(event) => setSourceRecordType(event.target.value as typeof sourceRecordType)}
        >
          {SOURCE_RECORD_TYPES.map((type) => (
            <option key={type} value={type}>
              {type.toLowerCase()}
            </option>
          ))}
        </select>

        <label htmlFor="memory-source-record-id">Source record id</label>
        <input id="memory-source-record-id" value={sourceRecordId} onChange={(event) => setSourceRecordId(event.target.value)} />

        <label htmlFor="memory-source-excerpt">Source excerpt</label>
        <textarea
          id="memory-source-excerpt"
          rows={2}
          value={sourceExcerpt}
          onChange={(event) => setSourceExcerpt(event.target.value)}
        />

        <button disabled={!canCreate || createMemoryProposal.isPending} onClick={() => createMemoryProposal.mutate()}>
          Save proposed memory
        </button>
        {createMemoryError ? <p role="alert">{createMemoryError}</p> : null}
          </div>
        </details>
      </section>

      <section className="card stack">
        <h2>Proposed memories</h2>
        {memoryQuery.data.length > 0 ? (
          <div className="stack split-grid">
            <div className="stack">
              {memoryQuery.data.map((item) => (
                <button
                  key={item.id}
                  className={item.id === activeMemoryId ? 'secondary-button active-item' : 'secondary-button'}
                  onClick={() => {
                    setSelectedMemoryId(item.id)
                    setRevisionStatement(item.statement)
                    setRevisionExcerpt(item.sourceExcerpt ?? '')
                  }}
                >
                  {item.statement}
                </button>
              ))}
            </div>

            <div className="stack">
              {selectedDetail && (
                <>
                  <h3>Source provenance</h3>
                  <p>
                    {selectedDetail.proposal.sourceKind.toLowerCase()} from {selectedDetail.proposal.sourceRecordType.toLowerCase()}
                  </p>
                  <p className="muted">Source record: {selectedDetail.proposal.sourceRecordId}</p>
                  {selectedDetail.proposal.sourceExcerpt ? <p>{selectedDetail.proposal.sourceExcerpt}</p> : <p>No excerpt provided.</p>}

                  <h3>Review</h3>
                  <label htmlFor="memory-review-reason">Review note</label>
                  <textarea id="memory-review-reason" rows={2} value={reviewReason} onChange={(event) => setReviewReason(event.target.value)} />
                  <div className="row">
                    <button onClick={() => reviewMemoryProposal.mutate('accept')} disabled={!reviewReason.trim() || reviewMemoryProposal.isPending}>
                      Accept
                    </button>
                    <button onClick={() => reviewMemoryProposal.mutate('reject')} disabled={!reviewReason.trim() || reviewMemoryProposal.isPending}>
                      Reject
                    </button>
                    <button onClick={() => deleteMemoryProposal.mutate()} disabled={deleteMemoryProposal.isPending}>
                      Delete
                    </button>
                  </div>

                  <details className="disclosure">
                    <summary>Edit this proposal</summary>
                    <div className="stack disclosure-content">
                  <label htmlFor="memory-revision-statement">Updated statement</label>
                  <textarea
                    id="memory-revision-statement"
                    rows={3}
                    value={revisionStatement}
                    onChange={(event) => setRevisionStatement(event.target.value)}
                  />
                  <label htmlFor="memory-revision-excerpt">Updated source excerpt</label>
                  <textarea
                    id="memory-revision-excerpt"
                    rows={2}
                    value={revisionExcerpt}
                    onChange={(event) => setRevisionExcerpt(event.target.value)}
                  />
                  <label htmlFor="memory-revision-reason">Reason for edit</label>
                  <textarea
                    id="memory-revision-reason"
                    rows={2}
                    value={revisionReason}
                    onChange={(event) => setRevisionReason(event.target.value)}
                  />
                  <button
                    onClick={() => reviseMemoryProposal.mutate()}
                    disabled={!revisionStatement.trim() || !revisionReason.trim() || reviseMemoryProposal.isPending}
                  >
                    Save revision
                  </button>
                    </div>
                  </details>

                  <details className="nested-disclosure">
                    <summary>Revision history ({selectedDetail.revisions.length})</summary>
                    <div className="disclosure-content">
                      {selectedDetail.revisions.length > 0 ? (
                        <ul>
                          {selectedDetail.revisions.map((revision) => (
                            <li key={revision.id}>{revision.previousStatus} to {revision.newStatus}: {revision.reason}</li>
                          ))}
                        </ul>
                      ) : <p>No revisions yet.</p>}
                    </div>
                  </details>
                </>
              )}
            </div>
          </div>
        ) : (
          <p>No memory proposals yet.</p>
        )}
      </section>
    </div>
  )
}
