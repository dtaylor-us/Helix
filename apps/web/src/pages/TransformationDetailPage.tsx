import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useParams } from '@tanstack/react-router'
import { useState } from 'react'
import type { Experiment } from '../../../../packages/contracts/src/index'
import { TermHint } from '../components/TermHint'
import { api } from '../api/http'

export function TransformationDetailPage() {
  const { id } = useParams({ from: '/transformations/$id' })
  const queryClient = useQueryClient()
  const [title, setTitle] = useState('')
  const [hypothesis, setHypothesis] = useState('')
  const [nextAction, setNextAction] = useState('')
  const [cadence, setCadence] = useState('')
  const [evidenceOfSuccess, setEvidenceOfSuccess] = useState('')
  const [reviewAt, setReviewAt] = useState('')
  const [draftStatusText, setDraftStatusText] = useState<string | null>(null)
  const [saveStatusText, setSaveStatusText] = useState<string | null>(null)
  const [savedExperiment, setSavedExperiment] = useState<Experiment | null>(null)

  const transformation = useQuery({ queryKey: ['transformation', id], queryFn: () => api.getTransformation(id) })

  const createExperiment = useMutation({
    mutationFn: () =>
      api.createExperiment(id, {
        title,
        hypothesis,
        nextAction,
        cadence,
        evidenceOfSuccess,
        reviewAt: reviewAt || undefined,
      }),
    onSuccess: (experiment) => {
      setTitle('')
      setHypothesis('')
      setNextAction('')
      setCadence('')
      setEvidenceOfSuccess('')
      setReviewAt('')
      setDraftStatusText(null)
      setSavedExperiment(experiment)
      setSaveStatusText(`Saved "${experiment.title}" as your current active experiment.`)
      queryClient.invalidateQueries({ queryKey: ['today'] })
    },
    onError: () => {
      setSaveStatusText('Could not save the experiment. Please try again.')
    },
  })

  // Prefills the form below with an AI-proposed experiment (ADR-016). Nothing is created until the
  // user reviews/edits these fields and presses "Save experiment" themselves (ADR-008).
  const proposeDraft = useMutation({
    mutationFn: () => api.proposeExperimentDraft(id),
    onSuccess: (draft) => {
      setTitle(draft.title ?? '')
      setHypothesis(draft.hypothesis ?? '')
      setNextAction(draft.nextAction ?? '')
      setCadence(draft.cadence ?? '')
      setEvidenceOfSuccess(draft.evidenceOfSuccess ?? '')
      setDraftStatusText(
        draft.source === 'AI'
          ? `Drafted by AI${draft.aiProvider ? ` (${draft.aiProvider})` : ''}. Review and edit before saving.`
          : `Fallback draft${draft.aiProvider ? ` — ${draft.aiProvider}` : ''}. Review and edit before saving.`,
      )
    },
    onError: () => {
      setDraftStatusText('Could not draft an experiment right now. You can still fill this in yourself.')
    },
  })

  return (
    <div className="stack">
      <section className="card">
        <h2>Transformation</h2>
        <p>{transformation.data?.title}</p>
        <p className="muted">{transformation.data?.purpose}</p>
        {transformation.data?.desiredIdentity && (
          <p className="muted">Who you&rsquo;re becoming: {transformation.data.desiredIdentity}</p>
        )}
        {transformation.data?.obstacle && (
          <p className="muted">What gets in the way: {transformation.data.obstacle}</p>
        )}
      </section>
      <section className="card">
        <h2>Add an experiment</h2>
        <p className="muted">
          A small, time-bounded attempt that helps you learn what actually moves this transformation forward.
        </p>
        <TermHint term="Experiment" />
        <div className="row">
          <button type="button" className="secondary-button" onClick={() => proposeDraft.mutate()} disabled={proposeDraft.isPending}>
            {proposeDraft.isPending ? 'Drafting…' : 'Draft this for me'}
          </button>
        </div>
        {draftStatusText && (
          <p role="status" aria-live="polite" className="muted">
            {draftStatusText}
          </p>
        )}
        <label htmlFor="exp-title">Experiment</label>
        <input id="exp-title" value={title} onChange={(e) => setTitle(e.target.value)} />
        <label htmlFor="exp-hypothesis">What do you want to learn?</label>
        <textarea id="exp-hypothesis" rows={3} value={hypothesis} onChange={(e) => setHypothesis(e.target.value)} />
        <label htmlFor="exp-next">Smallest next action</label>
        <input id="exp-next" value={nextAction} onChange={(e) => setNextAction(e.target.value)} />
        <label htmlFor="exp-cadence">How often will you try this? (optional)</label>
        <input
          id="exp-cadence"
          value={cadence}
          onChange={(e) => setCadence(e.target.value)}
          placeholder="e.g. Whenever I feel criticized"
        />
        <label htmlFor="exp-evidence">What would count as useful evidence? (optional)</label>
        <textarea
          id="exp-evidence"
          rows={2}
          value={evidenceOfSuccess}
          onChange={(e) => setEvidenceOfSuccess(e.target.value)}
          placeholder="e.g. Fewer moments of regretting how I responded"
        />
        <label htmlFor="exp-review">When will you review this? (optional)</label>
        <input id="exp-review" type="date" value={reviewAt} onChange={(e) => setReviewAt(e.target.value)} />
        <div>
          <button disabled={!title.trim() || createExperiment.isPending} onClick={() => createExperiment.mutate()}>
            Save experiment
          </button>
        </div>
        <p role="status" aria-live="polite" className="muted">
          {saveStatusText}
        </p>
        {savedExperiment && (
          <div>
            <h3>Current active experiment</h3>
            <p>{savedExperiment.title}</p>
            {savedExperiment.hypothesis && <p className="muted">{savedExperiment.hypothesis}</p>}
            {savedExperiment.nextAction && <p className="muted">Next action: {savedExperiment.nextAction}</p>}
          </div>
        )}
      </section>
    </div>
  )
}
