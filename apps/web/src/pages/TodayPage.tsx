import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { api } from '../api/http'

const DRAFT_KEY = 'helix:reflection-draft'

export function TodayPage() {
  const queryClient = useQueryClient()
  const [draft, setDraft] = useState(() => localStorage.getItem(DRAFT_KEY) ?? '')
  const [errorText, setErrorText] = useState<string | null>(null)
  const [replacementText, setReplacementText] = useState('')

  const todayQuery = useQuery({ queryKey: ['today'], queryFn: api.getToday })

  const reflectMutation = useMutation({
    mutationFn: (payload: { experimentId: string; content: string }) =>
      api.createReflection(payload.experimentId, { content: payload.content }),
    onSuccess: () => {
      localStorage.removeItem(DRAFT_KEY)
      setDraft('')
      queryClient.invalidateQueries({ queryKey: ['today'] })
    },
    onError: () => {
      setErrorText('Reflection could not be saved. Your draft is kept on this device.')
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

  if (todayQuery.isLoading) return <p>Loading your active context...</p>
  if (todayQuery.isError) return <p>Unable to load today view right now.</p>

  const data = todayQuery.data
  if (!data || !data.hasActiveExperiment || !data.activeExperiment) {
    return (
      <section className="card">
        <h2>Today</h2>
        <p>No active experiment yet. Create a transformation, then add one small experiment.</p>
      </section>
    )
  }

  const latestSuggestion = data.suggestionHistory[0]

  return (
    <div className="stack">
      <section className="card">
        <h2>Current Direction</h2>
        <p>Active experiment: {data.activeExperiment.title}</p>
        <p className="muted">Hypothesis: {data.activeExperiment.hypothesis || 'No hypothesis yet.'}</p>
      </section>

      <section className="card">
        <h2>Reflect</h2>
        <label htmlFor="reflection">What happened today?</label>
        <textarea
          id="reflection"
          value={draft}
          onChange={(e) => {
            setDraft(e.target.value)
            localStorage.setItem(DRAFT_KEY, e.target.value)
          }}
          rows={5}
        />
        <div className="row">
          <button
            onClick={() => reflectMutation.mutate({ experimentId: data.activeExperiment!.id, content: draft })}
            disabled={draft.trim().length === 0 || reflectMutation.isPending}
          >
            Save reflection
          </button>
          <span className="muted">Draft saved locally while you type.</span>
        </div>
        {errorText && <p role="alert">{errorText}</p>}
      </section>

      <section className="card">
        <h2>Suggested Small Action</h2>
        {latestSuggestion ? (
          <>
            <p>{latestSuggestion.text}</p>
            <p className="muted">Status: {latestSuggestion.status}</p>
            <div className="row">
              <button onClick={() => suggestionAction.mutate({ action: 'accept', id: latestSuggestion.id })}>Accept</button>
              <button onClick={() => suggestionAction.mutate({ action: 'dismiss', id: latestSuggestion.id })}>Dismiss</button>
            </div>
            <div className="row">
              <input
                aria-label="Replacement suggestion"
                value={replacementText}
                onChange={(e) => setReplacementText(e.target.value)}
                placeholder="Replace with your own wording"
              />
              <button
                onClick={() =>
                  suggestionAction.mutate({ action: 'replace', id: latestSuggestion.id, replacement: replacementText })
                }
                disabled={!replacementText.trim()}
              >
                Replace
              </button>
            </div>
          </>
        ) : (
          <p>No suggestion yet. Save a reflection first.</p>
        )}
      </section>

      <section className="card">
        <h2>History</h2>
        <h3>Recent reflections</h3>
        <ul>
          {data.reflectionHistory.slice(0, 5).map((item) => (
            <li key={item.id}>{item.content}</li>
          ))}
        </ul>
      </section>

      <section className="card">
        <h2>Placeholders</h2>
        <p>Recent Insight: coming in a later increment.</p>
        <p>Continue Conversation: coming in a later increment.</p>
      </section>
    </div>
  )
}
