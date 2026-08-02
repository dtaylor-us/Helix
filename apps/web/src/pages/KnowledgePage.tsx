import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link } from '@tanstack/react-router'
import { useState } from 'react'
import type { BeliefDetail, CreateEvidenceRequest, TodayResponse } from '../../../../packages/contracts/src'
import { api } from '../api/http'

type BeliefType = 'LIMITING' | 'EMPOWERING'
type EvidenceDirection = 'SUPPORTS' | 'CHALLENGES'
type EvidenceSourceMode = 'MANUAL_ENTRY' | 'REFLECTION'

export function KnowledgePage() {
  const queryClient = useQueryClient()
  const [transformationId, setTransformationId] = useState('')
  const [beliefStatement, setBeliefStatement] = useState('')
  const [beliefType, setBeliefType] = useState<BeliefType>('LIMITING')
  const [selectedBeliefId, setSelectedBeliefId] = useState<string>('')

  const transformationsQuery = useQuery({
    queryKey: ['transformations'],
    queryFn: api.listTransformations,
  })

  const beliefsQuery = useQuery({
    queryKey: ['beliefs'],
    queryFn: api.listBeliefs,
  })

  const todayQuery = useQuery({
    queryKey: ['today'],
    queryFn: api.getToday,
  })

  const activeTransformationId = transformationId || transformationsQuery.data?.[0]?.id || ''
  const activeBeliefId = selectedBeliefId || beliefsQuery.data?.[0]?.id || ''

  const beliefDetailQuery = useQuery({
    queryKey: ['belief', activeBeliefId],
    queryFn: () => api.getBelief(activeBeliefId),
    enabled: activeBeliefId.length > 0,
  })

  const createBelief = useMutation({
    mutationFn: () => api.createBelief({ transformationId: activeTransformationId, statement: beliefStatement, type: beliefType }),
    onSuccess: (belief) => {
      setBeliefStatement('')
      queryClient.invalidateQueries({ queryKey: ['beliefs'] })
      setSelectedBeliefId(belief.id)
    },
  })

  if (transformationsQuery.isLoading || beliefsQuery.isLoading || todayQuery.isLoading) {
    return <p>Loading knowledge foundations...</p>
  }

  const selectedBelief = beliefDetailQuery.data

  return (
    <div className="stack knowledge-grid">
      <section className="card">
        <h2>Beliefs</h2>
        <p className="muted">Track beliefs as hypotheses you can challenge, update, and ground in evidence.</p>
        <div className="stack">
          <label htmlFor="belief-transformation">Transformation</label>
          <select id="belief-transformation" value={activeTransformationId} onChange={(event) => setTransformationId(event.target.value)}>
            {transformationsQuery.data?.map((item) => (
              <option key={item.id} value={item.id}>
                {item.title}
              </option>
            ))}
          </select>

          <label htmlFor="belief-statement">Belief statement</label>
          <textarea
            id="belief-statement"
            rows={4}
            value={beliefStatement}
            onChange={(event) => setBeliefStatement(event.target.value)}
          />

          <label htmlFor="belief-type">Belief type</label>
          <select id="belief-type" value={beliefType} onChange={(event) => setBeliefType(event.target.value as BeliefType)}>
            <option value="LIMITING">Limiting</option>
            <option value="EMPOWERING">Empowering</option>
          </select>

          <button disabled={!activeTransformationId || !beliefStatement.trim() || createBelief.isPending} onClick={() => createBelief.mutate()}>
            Save belief
          </button>
        </div>
      </section>

      <section className="card">
        <h2>Belief list</h2>
        {beliefsQuery.data && beliefsQuery.data.length > 0 ? (
          <div className="stack">
            {beliefsQuery.data.map((belief) => (
              <button
                key={belief.id}
                type="button"
                className={belief.id === activeBeliefId ? 'secondary-button active-item' : 'secondary-button'}
                onClick={() => setSelectedBeliefId(belief.id)}
              >
                <span>{belief.statement}</span>
                <span className="muted">{belief.type.toLowerCase()}</span>
              </button>
            ))}
          </div>
        ) : (
          <p>No beliefs recorded yet.</p>
        )}
      </section>

      <section className="card knowledge-detail">
        <h2>Evidence and revision trail</h2>
        {!activeBeliefId && <p>Select a belief to inspect its evidence and revisions.</p>}
        {selectedBelief && (
          <BeliefDetailPanel
            key={`${selectedBelief.belief.id}-${selectedBelief.belief.revisedAt}`}
            beliefDetail={selectedBelief}
            todayData={todayQuery.data}
          />
        )}
      </section>
    </div>
  )
}

function BeliefDetailPanel({ beliefDetail, todayData }: { beliefDetail: BeliefDetail; todayData?: TodayResponse }) {
  const queryClient = useQueryClient()
  const [revisionStatement, setRevisionStatement] = useState(beliefDetail.belief.statement)
  const [revisionType, setRevisionType] = useState<BeliefType>(beliefDetail.belief.type)
  const [revisionReason, setRevisionReason] = useState('')
  const [sourceEvidenceId, setSourceEvidenceId] = useState('')
  const [evidenceSummary, setEvidenceSummary] = useState('')
  const [evidenceInterpretation, setEvidenceInterpretation] = useState('')
  const [evidenceDirection, setEvidenceDirection] = useState<EvidenceDirection>('SUPPORTS')
  const [evidenceSourceMode, setEvidenceSourceMode] = useState<EvidenceSourceMode>('MANUAL_ENTRY')
  const [selectedReflectionId, setSelectedReflectionId] = useState('')
  const [manualExcerpt, setManualExcerpt] = useState('')

  const availableReflections = todayData?.reflectionHistory ?? []
  const activeReflectionId = selectedReflectionId || availableReflections[0]?.id || ''

  const reviseBelief = useMutation({
    mutationFn: () =>
      api.reviseBelief(beliefDetail.belief.id, {
        statement: revisionStatement,
        type: revisionType,
        reason: revisionReason,
        sourceEvidenceId: sourceEvidenceId || undefined,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['beliefs'] })
      queryClient.invalidateQueries({ queryKey: ['belief', beliefDetail.belief.id] })
    },
  })

  const createEvidence = useMutation({
    mutationFn: () => api.createEvidence(beliefDetail.belief.id, buildEvidencePayload()),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['belief', beliefDetail.belief.id] })
    },
  })

  function buildEvidencePayload(): CreateEvidenceRequest {
    if (evidenceSourceMode === 'REFLECTION') {
      const reflection = availableReflections.find((item) => item.id === activeReflectionId)
      return {
        summary: evidenceSummary,
        interpretation: evidenceInterpretation || undefined,
        direction: evidenceDirection,
        experimentId: todayData?.activeExperiment?.id,
        reflectionId: reflection?.id,
        provenance: {
          sourceKind: 'REFLECTION',
          recordType: 'REFLECTION',
          recordId: reflection?.id,
          excerpt: reflection?.content,
        },
      }
    }

    return {
      summary: evidenceSummary,
      interpretation: evidenceInterpretation || undefined,
      direction: evidenceDirection,
      provenance: {
        sourceKind: 'MANUAL_ENTRY',
        recordType: 'MANUAL_ENTRY',
        excerpt: manualExcerpt || undefined,
      },
    }
  }

  return (
    <div className="stack">
      <div className="stack emphasis-block">
        <p><strong>Current belief:</strong> {beliefDetail.belief.statement}</p>
        <p className="muted">Type: {beliefDetail.belief.type.toLowerCase()}</p>
        <p>{beliefDetail.narrative}</p>
        <div className="row">
          <Link
            to="/knowledge-graph/$nodeType/$sourceRecordId"
            params={{ nodeType: 'BELIEF', sourceRecordId: beliefDetail.belief.id }}
            className="secondary-button"
          >
            Explore connections
          </Link>
        </div>
      </div>

      <div className="stack split-grid">
        <div className="stack">
          <h3>Revise belief</h3>
          <label htmlFor="revision-statement">Updated statement</label>
          <textarea
            id="revision-statement"
            rows={3}
            value={revisionStatement}
            onChange={(event) => setRevisionStatement(event.target.value)}
          />
          <label htmlFor="revision-type">Updated type</label>
          <select id="revision-type" value={revisionType} onChange={(event) => setRevisionType(event.target.value as BeliefType)}>
            <option value="LIMITING">Limiting</option>
            <option value="EMPOWERING">Empowering</option>
          </select>
          <label htmlFor="revision-reason">Why did this belief change?</label>
          <textarea
            id="revision-reason"
            rows={3}
            value={revisionReason}
            onChange={(event) => setRevisionReason(event.target.value)}
          />
          <label htmlFor="revision-source">Source evidence</label>
          <select id="revision-source" value={sourceEvidenceId} onChange={(event) => setSourceEvidenceId(event.target.value)}>
            <option value="">None</option>
            {beliefDetail.evidenceTimeline.map((evidence) => (
              <option key={evidence.id} value={evidence.id}>
                {evidence.summary}
              </option>
            ))}
          </select>
          <button
            disabled={!revisionStatement.trim() || !revisionReason.trim() || reviseBelief.isPending}
            onClick={() => reviseBelief.mutate()}
          >
            Save revision
          </button>
        </div>

        <div className="stack">
          <h3>Add evidence</h3>
          <label htmlFor="evidence-summary">Observation</label>
          <textarea
            id="evidence-summary"
            rows={3}
            value={evidenceSummary}
            onChange={(event) => setEvidenceSummary(event.target.value)}
          />
          <label htmlFor="evidence-interpretation">Interpretation</label>
          <textarea
            id="evidence-interpretation"
            rows={3}
            value={evidenceInterpretation}
            onChange={(event) => setEvidenceInterpretation(event.target.value)}
          />
          <label htmlFor="evidence-direction">Direction</label>
          <select
            id="evidence-direction"
            value={evidenceDirection}
            onChange={(event) => setEvidenceDirection(event.target.value as EvidenceDirection)}
          >
            <option value="SUPPORTS">Supports</option>
            <option value="CHALLENGES">Challenges</option>
          </select>
          <label htmlFor="evidence-source-mode">Source</label>
          <select
            id="evidence-source-mode"
            value={evidenceSourceMode}
            onChange={(event) => setEvidenceSourceMode(event.target.value as EvidenceSourceMode)}
          >
            <option value="MANUAL_ENTRY">Manual observation</option>
            <option value="REFLECTION">Today reflection</option>
          </select>

          {evidenceSourceMode === 'REFLECTION' ? (
            <>
              <label htmlFor="reflection-source">Reflection source</label>
              <select id="reflection-source" value={activeReflectionId} onChange={(event) => setSelectedReflectionId(event.target.value)}>
                {availableReflections.map((reflection) => (
                  <option key={reflection.id} value={reflection.id}>
                    {new Date(reflection.createdAt).toLocaleString()} - {reflection.content.slice(0, 60)}
                  </option>
                ))}
              </select>
            </>
          ) : (
            <>
              <label htmlFor="manual-excerpt">Provenance note</label>
              <textarea
                id="manual-excerpt"
                rows={2}
                value={manualExcerpt}
                onChange={(event) => setManualExcerpt(event.target.value)}
              />
            </>
          )}

          <button
            disabled={!evidenceSummary.trim() || createEvidence.isPending || (evidenceSourceMode === 'REFLECTION' && availableReflections.length === 0)}
            onClick={() => createEvidence.mutate()}
          >
            Save evidence
          </button>
        </div>
      </div>

      <div className="stack split-grid">
        <div className="stack">
          <h3>Evidence timeline</h3>
          {beliefDetail.evidenceTimeline.length > 0 ? (
            beliefDetail.evidenceTimeline.map((evidence) => (
              <article key={evidence.id} className="timeline-item">
                <p><strong>{evidence.direction === 'SUPPORTS' ? 'Supports' : 'Challenges'}:</strong> {evidence.summary}</p>
                {evidence.interpretation && <p className="muted">Interpretation: {evidence.interpretation}</p>}
                <p className="muted">
                  Provenance: {evidence.provenance.sourceKind.toLowerCase()} via {evidence.provenance.recordType.toLowerCase()}
                  {evidence.provenance.excerpt ? ` - ${evidence.provenance.excerpt}` : ''}
                </p>
              </article>
            ))
          ) : (
            <p>No evidence added yet.</p>
          )}
        </div>

        <div className="stack">
          <h3>Revision history</h3>
          {beliefDetail.revisions.length > 0 ? (
            beliefDetail.revisions.map((revision) => (
              <article key={revision.id} className="timeline-item">
                <p><strong>{revision.previousType.toLowerCase()}</strong> to <strong>{revision.newType.toLowerCase()}</strong></p>
                <p>{revision.reason}</p>
                <p className="muted">{revision.previousStatement}{' -> '}{revision.newStatement}</p>
              </article>
            ))
          ) : (
            <p>No revisions yet. The initial belief state is the current state.</p>
          )}
        </div>
      </div>
    </div>
  )
}