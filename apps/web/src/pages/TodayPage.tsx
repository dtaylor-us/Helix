import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link } from '@tanstack/react-router'
import { useState } from 'react'
import { TermHint } from '../components/TermHint'
import { REFLECTION_FOLLOW_UP_QUESTIONS } from '../content/reflectionQuestions'
import { api } from '../api/http'

const DRAFT_KEY_PREFIX = 'helix:reflection-draft:'

export function TodayPage() {
  const queryClient = useQueryClient()
  const [errorText, setErrorText] = useState<string | null>(null)
  const [statusText, setStatusText] = useState<string | null>(null)
  const [replacementText, setReplacementText] = useState('')
  const [attempted, setAttempted] = useState<boolean | undefined>(undefined)
  const [followUps, setFollowUps] = useState<Record<string, string | undefined>>({})
  const [revealedFollowUps, setRevealedFollowUps] = useState(0)
  const [wisdomDraft, setWisdomDraft] = useState<{ reflectionId: string; statement: string } | null>(null)
  const [wisdomStatusText, setWisdomStatusText] = useState<string | null>(null)

  const todayQuery = useQuery({ queryKey: ['today'], queryFn: api.getToday })
  const transformationsQuery = useQuery({ queryKey: ['transformations'], queryFn: api.listTransformations })
  // Fetched here (rather than only inside the Wisdom workspace) so a weekly narrative retrospective
  // can be surfaced contextually on Today, per Phase 4. Only fetched once the user has at least one
  // transformation, so it doesn't fire on the true first-use welcome screen.
  const retrospectiveDraftQuery = useQuery({
    queryKey: ['weekly-retrospective-draft'],
    queryFn: api.getWeeklyRetrospectiveDraft,
    enabled: (transformationsQuery.data?.length ?? 0) > 0,
  })

  const activeExperimentId = todayQuery.data?.activeExperiment?.id
  const draftKey = activeExperimentId ? `${DRAFT_KEY_PREFIX}${activeExperimentId}` : null

  // Reflection drafts are namespaced per experiment so switching experiments never shows a
  // draft written for a different one. When the active experiment changes, load whatever was
  // saved locally for it (or start blank), and reset the progressive follow-up questions too.
  // This mirrors React's documented pattern for resetting state when a derived value changes,
  // without a synchronous setState-in-effect. Only the main "what happened" content is
  // persisted to localStorage; the optional follow-up answers are not (see known limitations
  // in the development log).
  const [draftExperimentId, setDraftExperimentId] = useState<string | undefined>(undefined)
  const [draft, setDraft] = useState('')
  if (activeExperimentId !== draftExperimentId) {
    setDraftExperimentId(activeExperimentId)
    setDraft(draftKey ? localStorage.getItem(draftKey) ?? '' : '')
    setAttempted(undefined)
    setFollowUps({})
    setRevealedFollowUps(0)
    setWisdomDraft(null)
    setWisdomStatusText(null)
  }

  const reflectMutation = useMutation({
    mutationFn: (payload: {
      experimentId: string
      content: string
      attempted?: boolean
      noticed?: string
      evidenceNoted?: string
      surprise?: string
    }) =>
      api.createReflection(payload.experimentId, {
        content: payload.content,
        attempted: payload.attempted,
        noticed: payload.noticed,
        evidenceNoted: payload.evidenceNoted,
        surprise: payload.surprise,
      }),
    onSuccess: (result, variables) => {
      if (draftKey) localStorage.removeItem(draftKey)
      setDraft('')
      setAttempted(undefined)
      setFollowUps({})
      setRevealedFollowUps(0)
      setErrorText(null)
      setStatusText('Reflection saved.')
      // Deterministically propose a wisdom statement from the most specific answer given,
      // falling back to the main answer. No AI involved (consistent with ADR-006); the user
      // can edit or dismiss this before anything is saved as wisdom.
      const proposedStatement = (
        variables.evidenceNoted || variables.noticed || variables.surprise || variables.content
      ).trim().slice(0, 500)
      setWisdomDraft(proposedStatement ? { reflectionId: result.reflection.id, statement: proposedStatement } : null)
      setWisdomStatusText(null)
      queryClient.invalidateQueries({ queryKey: ['today'] })
    },
    onError: () => {
      setStatusText(null)
      setErrorText('Reflection could not be saved. Your draft is kept on this device.')
    },
  })

  const captureWisdom = useMutation({
    mutationFn: (payload: { reflectionId: string; statement: string }) =>
      api.createWisdom({
        statement: payload.statement,
        sources: [{ sourceType: 'REFLECTION', sourceRecordId: payload.reflectionId }],
      }),
    onSuccess: () => {
      setWisdomDraft(null)
      setWisdomStatusText('Wisdom saved to your Library.')
      queryClient.invalidateQueries({ queryKey: ['wisdom'] })
    },
    onError: () => {
      setWisdomStatusText('Could not save wisdom. You can also add it from Library.')
    },
  })

  const suggestionAction = useMutation({
    mutationFn: async (params: { action: 'accept' | 'dismiss' | 'replace'; id: string; replacement?: string }) => {
      if (params.action === 'accept') return api.acceptSuggestion(params.id)
      if (params.action === 'dismiss') return api.dismissSuggestion(params.id)
      return api.replaceSuggestion(params.id, params.replacement ?? '')
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['today'] }),
  })

  if (todayQuery.isLoading || transformationsQuery.isLoading) {
    return <p>Loading your active context...</p>
  }

  if (todayQuery.isError) {
    return <p role="alert">Unable to load today view right now.</p>
  }

  const data = todayQuery.data
  const transformations = transformationsQuery.data ?? []

  // True first-use state: nothing has been started yet.
  if (transformations.length === 0) {
    return (
      <div className="stack">
        <section className="card intro-card">
          <h2>Welcome to Helix</h2>
          <p>
            Helix helps you turn who you want to become into small actions, reflection, and evidence of growth
            &mdash; one transformation at a time.
          </p>
          <ol className="journey-loop">
            <li>Start a transformation</li>
            <li>Run one small experiment</li>
            <li>Reflect on what happened</li>
            <li>Notice the evidence it leaves behind</li>
            <li>Keep what turns into wisdom</li>
          </ol>
          <div>
            <Link to="/transformations" className="cta-button">
              Begin my first transformation
            </Link>
          </div>
        </section>
      </div>
    )
  }

  // Has transformations, but nothing active to work on today.
  if (!data || !data.hasActiveExperiment || !data.activeExperiment) {
    const firstTransformation = transformations[0]
    return (
      <section className="card">
        <h2>Today</h2>
        <p>No active experiment yet. Choose a transformation and add one small experiment to it.</p>
        <div className="row">
          {firstTransformation && (
            <Link to="/transformations/$id" params={{ id: firstTransformation.id }} className="cta-button">
              Add an experiment to &ldquo;{firstTransformation.title}&rdquo;
            </Link>
          )}
          <Link to="/transformations" className="secondary-button">
            See all transformations
          </Link>
        </div>
      </section>
    )
  }

  const latestSuggestion = data.suggestionHistory[0]
  const hour = new Date().getHours()
  const isMorning = hour < 12
  const nextFollowUp = REFLECTION_FOLLOW_UP_QUESTIONS[revealedFollowUps]

  return (
    <div className="stack">
      <section className="card">
        <h2>Current Direction</h2>
        <p>Active experiment: {data.activeExperiment.title}</p>
        <p className="muted">Hypothesis: {data.activeExperiment.hypothesis || 'No hypothesis yet.'}</p>
        {data.activeExperiment.cadence && (
          <p className="muted">How often: {data.activeExperiment.cadence}</p>
        )}
        {data.activeExperiment.evidenceOfSuccess && (
          <p className="muted">Evidence to watch for: {data.activeExperiment.evidenceOfSuccess}</p>
        )}
        {data.activeExperiment.reviewAt && (
          <p className="muted">Review by: {data.activeExperiment.reviewAt}</p>
        )}
        <TermHint term="Experiment" />
      </section>

      <section className="card">
        <h2>Suggested Small Action</h2>
        {latestSuggestion ? (
          <>
            <p>{latestSuggestion.text}</p>
            <p className="muted">
              Why this: part of your active experiment, &ldquo;{data.activeExperiment.title}&rdquo;. Status:{' '}
              {latestSuggestion.status}.
            </p>
            <div className="row">
              <button
                onClick={() => suggestionAction.mutate({ action: 'accept', id: latestSuggestion.id })}
                disabled={suggestionAction.isPending}
              >
                I&rsquo;ll try this
              </button>
              <button
                onClick={() => suggestionAction.mutate({ action: 'dismiss', id: latestSuggestion.id })}
                disabled={suggestionAction.isPending}
              >
                Not this one
              </button>
            </div>
            <div className="row">
              <input
                aria-label="Replacement suggestion"
                value={replacementText}
                onChange={(e) => setReplacementText(e.target.value)}
                placeholder="Or write your own smaller version"
              />
              <button
                onClick={() =>
                  suggestionAction.mutate({ action: 'replace', id: latestSuggestion.id, replacement: replacementText })
                }
                disabled={!replacementText.trim() || suggestionAction.isPending}
              >
                Use this instead
              </button>
            </div>
            <TermHint term="Suggested Small Action" />
          </>
        ) : (
          <p>No suggestion yet. Save a reflection below and Helix will offer one small next step.</p>
        )}
      </section>

      <section className="card">
        <h2>{isMorning ? 'Morning check-in' : 'Evening review'}</h2>
        <p className="muted">
          {isMorning
            ? "Haven't tried today's action yet? That's alright — come back after you do, or jot down what's on your mind now."
            : 'What happened when you tried it today?'}
        </p>

        <div className="row" role="group" aria-label="Did you try it?">
          <span>Did you try it?</span>
          <button
            type="button"
            className={attempted === true ? 'secondary-button active-item' : 'secondary-button'}
            aria-pressed={attempted === true}
            onClick={() => setAttempted(true)}
          >
            Yes
          </button>
          <button
            type="button"
            className={attempted === false ? 'secondary-button active-item' : 'secondary-button'}
            aria-pressed={attempted === false}
            onClick={() => setAttempted(false)}
          >
            Not yet
          </button>
        </div>

        <label htmlFor="reflection">What happened &mdash; or what&rsquo;s on your mind?</label>
        <textarea
          id="reflection"
          value={draft}
          onChange={(e) => {
            setDraft(e.target.value)
            setStatusText(null)
            if (draftKey) localStorage.setItem(draftKey, e.target.value)
          }}
          rows={5}
        />

        {REFLECTION_FOLLOW_UP_QUESTIONS.slice(0, revealedFollowUps).map((question) => (
          <div key={question.id} className="stack">
            <label htmlFor={`reflection-${question.id}`}>{question.prompt}</label>
            <textarea
              id={`reflection-${question.id}`}
              rows={2}
              value={followUps[question.id] ?? ''}
              onChange={(e) => setFollowUps((prev) => ({ ...prev, [question.id]: e.target.value }))}
              placeholder={question.placeholder}
            />
          </div>
        ))}

        {draft.trim().length > 0 && nextFollowUp && (
          <div>
            <button type="button" className="secondary-button" onClick={() => setRevealedFollowUps((n) => n + 1)}>
              + {nextFollowUp.prompt}
            </button>
          </div>
        )}

        <div className="row">
          <button
            onClick={() =>
              reflectMutation.mutate({
                experimentId: data.activeExperiment!.id,
                content: draft,
                attempted,
                noticed: followUps.noticed || undefined,
                evidenceNoted: followUps.evidenceNoted || undefined,
                surprise: followUps.surprise || undefined,
              })
            }
            disabled={draft.trim().length === 0 || reflectMutation.isPending}
          >
            Save reflection
          </button>
          <span className="muted">Draft saved on this device while you type. Follow-up answers are not.</span>
        </div>
        <p role="status" aria-live="polite" className="muted">
          {statusText}{' '}
          {statusText === 'Reflection saved.' && (
            <Link to="/knowledge">This might be useful evidence &mdash; add it in Knowledge.</Link>
          )}
        </p>
        {errorText && <p role="alert">{errorText}</p>}
        <TermHint term="Reflection" />
      </section>

      {wisdomDraft && (
        <section className="card">
          <h2>This reflection may contain a lesson worth keeping</h2>
          <p className="muted">
            Edit it, or save it as-is. You can always refine or revise it later in Library.
          </p>
          <label htmlFor="wisdom-draft-statement">Proposed wisdom statement</label>
          <textarea
            id="wisdom-draft-statement"
            rows={3}
            maxLength={500}
            value={wisdomDraft.statement}
            onChange={(e) => setWisdomDraft({ ...wisdomDraft, statement: e.target.value.slice(0, 500) })}
          />
          <div className="row">
            <button
              onClick={() => captureWisdom.mutate(wisdomDraft)}
              disabled={!wisdomDraft.statement.trim() || captureWisdom.isPending}
            >
              Save as wisdom
            </button>
            <button type="button" className="secondary-button" onClick={() => setWisdomDraft(null)}>
              Not now
            </button>
          </div>
          <TermHint term="Wisdom" />
        </section>
      )}
      {wisdomStatusText && (
        <p role="status" aria-live="polite" className="muted">
          {wisdomStatusText}
        </p>
      )}

      {retrospectiveDraftQuery.data && retrospectiveDraftQuery.data.reflectionSummaries.length > 0 && (
        <section className="card">
          <h2>This week</h2>
          <p>{retrospectiveDraftQuery.data.summary}</p>
          <p className="muted">{retrospectiveDraftQuery.data.assistance}</p>
          <Link to="/library" className="secondary-button">
            See full weekly retrospective
          </Link>
        </section>
      )}

      <section className="card">
        <h2>History</h2>
        <h3>Recent reflections</h3>
        {data.reflectionHistory.length > 0 ? (
          <ul>
            {data.reflectionHistory.slice(0, 5).map((item) => (
              <li key={item.id}>
                {item.content}
                {item.noticed && <p className="muted">Noticed: {item.noticed}</p>}
                {item.evidenceNoted && <p className="muted">Evidence: {item.evidenceNoted}</p>}
                {item.surprise && <p className="muted">Surprise: {item.surprise}</p>}
              </li>
            ))}
          </ul>
        ) : (
          <p className="muted">Nothing recorded yet.</p>
        )}
      </section>
    </div>
  )
}
