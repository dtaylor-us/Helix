import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import type { WeeklyRetrospective, WisdomSourceLinkRequest } from '../../../../packages/contracts/src'
import { api } from '../api/http'

type SourceMode = 'REFLECTION' | 'RETROSPECTIVE'

export function WisdomPage() {
  const queryClient = useQueryClient()
  const [statement, setStatement] = useState('')
  const [sourceMode, setSourceMode] = useState<SourceMode>('REFLECTION')
  const [sourceNote, setSourceNote] = useState('')
  const [selectedWisdomId, setSelectedWisdomId] = useState('')
  const [revisionStatement, setRevisionStatement] = useState('')
  const [revisionReason, setRevisionReason] = useState('')
  const [retrospectiveStatusText, setRetrospectiveStatusText] = useState('')

  const draftQuery = useQuery({
    queryKey: ['weekly-retrospective-draft'],
    queryFn: api.getWeeklyRetrospectiveDraft,
  })

  const retrospectivesQuery = useQuery({
    queryKey: ['retrospectives'],
    queryFn: api.listRetrospectives,
  })

  const wisdomQuery = useQuery({
    queryKey: ['wisdom'],
    queryFn: api.listWisdom,
  })

  const activeWisdomId = selectedWisdomId || wisdomQuery.data?.[0]?.id || ''

  const wisdomDetailQuery = useQuery({
    queryKey: ['wisdom', activeWisdomId],
    queryFn: () => api.getWisdom(activeWisdomId),
    enabled: activeWisdomId.length > 0,
  })

  const saveRetrospective = useMutation({
    mutationFn: api.saveWeeklyRetrospective,
    onSuccess: (savedRetrospective) => {
      setRetrospectiveStatusText('Weekly snapshot saved to history.')
      queryClient.setQueryData<WeeklyRetrospective[]>(['retrospectives'], (current) => {
        const existing = current ?? []
        return [savedRetrospective, ...existing.filter((entry) => entry.id !== savedRetrospective.id)]
      })
      queryClient.invalidateQueries({ queryKey: ['retrospectives'] })
    },
  })

  const createWisdom = useMutation({
    mutationFn: () => {
      const source = buildSource()
      return api.createWisdom({
        statement,
        retrospectiveId: sourceMode === 'RETROSPECTIVE' ? source.sourceRecordId : undefined,
        sources: [source],
      })
    },
    onSuccess: (entry) => {
      setStatement('')
      setSourceNote('')
      queryClient.invalidateQueries({ queryKey: ['wisdom'] })
      setSelectedWisdomId(entry.id)
    },
  })

  const reviseWisdom = useMutation({
    mutationFn: () => api.reviseWisdom(activeWisdomId, { statement: revisionStatement, reason: revisionReason }),
    onSuccess: () => {
      setRevisionReason('')
      queryClient.invalidateQueries({ queryKey: ['wisdom'] })
      queryClient.invalidateQueries({ queryKey: ['wisdom', activeWisdomId] })
    },
  })

  if (draftQuery.isLoading || retrospectivesQuery.isLoading || wisdomQuery.isLoading) {
    return <p>Loading wisdom workspace...</p>
  }

  if (!draftQuery.data || !retrospectivesQuery.data || !wisdomQuery.data) {
    return <p>Wisdom workspace is unavailable right now.</p>
  }

  const reflectionSourceId = draftQuery.data.reflectionSummaries[0]?.reflectionId
  const retrospectiveSourceId = retrospectivesQuery.data[0]?.id

  function buildSource(): WisdomSourceLinkRequest {
    if (sourceMode === 'RETROSPECTIVE' && retrospectiveSourceId) {
      return {
        sourceType: 'RETROSPECTIVE',
        sourceRecordId: retrospectiveSourceId,
        note: sourceNote || undefined,
      }
    }

    if (!reflectionSourceId) {
      throw new Error('A weekly reflection source is required for reflection-linked wisdom.')
    }

    return {
      sourceType: 'REFLECTION',
      sourceRecordId: reflectionSourceId,
      note: sourceNote || undefined,
    }
  }

  const hasSource = sourceMode === 'REFLECTION' ? Boolean(reflectionSourceId) : Boolean(retrospectiveSourceId)
  const canCreateWisdom = Boolean(statement.trim()) && hasSource
  const selectedDetail = wisdomDetailQuery.data

  return (
    <div className="stack">
      <section className="card stack">
        <h2>Weekly retrospective</h2>
        <p>{draftQuery.data.summary}</p>
        <p className="muted">{draftQuery.data.assistance}</p>
        <button onClick={() => saveRetrospective.mutate()} disabled={saveRetrospective.isPending}>
          Save weekly snapshot
        </button>
        <p role="status" aria-live="polite" className="muted">
          {retrospectiveStatusText}
        </p>
        <h3>Reflection summaries</h3>
        {draftQuery.data.reflectionSummaries.length > 0 ? (
          <ul>
            {draftQuery.data.reflectionSummaries.map((summary) => (
              <li key={summary.reflectionId}>{summary.summary}</li>
            ))}
          </ul>
        ) : (
          <p>No reflections recorded in the past week yet.</p>
        )}
        <h3>Saved snapshots</h3>
        {retrospectivesQuery.data.length > 0 ? (
          <ul>
            {retrospectivesQuery.data.map((retrospective) => (
              <li key={retrospective.id}>
                <strong>
                  {retrospective.periodStart} to {retrospective.periodEnd}
                </strong>
                : {retrospective.summary}
              </li>
            ))}
          </ul>
        ) : (
          <p>No snapshots saved yet.</p>
        )}
      </section>

      <section className="card stack">
        <h2>Accepted wisdom</h2>
        <label htmlFor="wisdom-statement">Wisdom statement</label>
        <textarea id="wisdom-statement" rows={3} value={statement} onChange={(event) => setStatement(event.target.value)} />

        <label htmlFor="wisdom-source-mode">Source type</label>
        <select id="wisdom-source-mode" value={sourceMode} onChange={(event) => setSourceMode(event.target.value as SourceMode)}>
          <option value="REFLECTION">Recent reflection</option>
          <option value="RETROSPECTIVE">Saved retrospective</option>
        </select>

        <label htmlFor="wisdom-source-note">Source note</label>
        <textarea id="wisdom-source-note" rows={2} value={sourceNote} onChange={(event) => setSourceNote(event.target.value)} />

        <button disabled={!canCreateWisdom || createWisdom.isPending} onClick={() => createWisdom.mutate()}>
          Save wisdom
        </button>
      </section>

      <section className="card stack">
        <h2>Wisdom library</h2>
        {wisdomQuery.data.length > 0 ? (
          <div className="stack split-grid">
            <div className="stack">
              {wisdomQuery.data.map((item) => (
                <button
                  key={item.id}
                  className={item.id === activeWisdomId ? 'secondary-button active-item' : 'secondary-button'}
                  onClick={() => {
                    setSelectedWisdomId(item.id)
                    setRevisionStatement(item.statement)
                  }}
                >
                  {item.statement}
                </button>
              ))}
            </div>
            <div className="stack">
              {selectedDetail && (
                <>
                  <h3>Sources</h3>
                  <ul>
                    {selectedDetail.sources.map((source) => (
                      <li key={source.id}>
                        {source.sourceType.toLowerCase()} - {source.sourceRecordId}
                        {source.note ? ` (${source.note})` : ''}
                      </li>
                    ))}
                  </ul>

                  <h3>Revise wisdom</h3>
                  <label htmlFor="revision-statement">Updated statement</label>
                  <textarea
                    id="revision-statement"
                    rows={3}
                    value={revisionStatement}
                    onChange={(event) => setRevisionStatement(event.target.value)}
                  />
                  <label htmlFor="revision-reason">Reason</label>
                  <textarea
                    id="revision-reason"
                    rows={2}
                    value={revisionReason}
                    onChange={(event) => setRevisionReason(event.target.value)}
                  />
                  <button
                    onClick={() => reviseWisdom.mutate()}
                    disabled={!revisionStatement.trim() || !revisionReason.trim() || reviseWisdom.isPending}
                  >
                    Save revision
                  </button>

                  <h3>Revision history</h3>
                  {selectedDetail.revisions.length > 0 ? (
                    <ul>
                      {selectedDetail.revisions.map((revision) => (
                        <li key={revision.id}>
                          {revision.previousStatement} {'->'} {revision.newStatement}
                        </li>
                      ))}
                    </ul>
                  ) : (
                    <p>No revisions yet.</p>
                  )}
                </>
              )}
            </div>
          </div>
        ) : (
          <p>No wisdom saved yet.</p>
        )}
      </section>
    </div>
  )
}
