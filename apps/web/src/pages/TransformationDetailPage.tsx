import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useParams } from '@tanstack/react-router'
import { useState } from 'react'
import { TermHint } from '../components/TermHint'
import { api } from '../api/http'

export function TransformationDetailPage() {
  const { id } = useParams({ from: '/transformations/$id' })
  const queryClient = useQueryClient()
  const [title, setTitle] = useState('')
  const [hypothesis, setHypothesis] = useState('')
  const [nextAction, setNextAction] = useState('')

  const transformation = useQuery({ queryKey: ['transformation', id], queryFn: () => api.getTransformation(id) })

  const createExperiment = useMutation({
    mutationFn: () => api.createExperiment(id, { title, hypothesis, nextAction }),
    onSuccess: () => {
      setTitle('')
      setHypothesis('')
      setNextAction('')
      queryClient.invalidateQueries({ queryKey: ['today'] })
    },
  })

  return (
    <div className="stack">
      <section className="card">
        <h2>Transformation</h2>
        <p>{transformation.data?.title}</p>
        <p className="muted">{transformation.data?.purpose}</p>
      </section>
      <section className="card">
        <h2>Add an experiment</h2>
        <p className="muted">
          A small, time-bounded attempt that helps you learn what actually moves this transformation forward.
        </p>
        <TermHint term="Experiment" />
        <label htmlFor="exp-title">Experiment</label>
        <input id="exp-title" value={title} onChange={(e) => setTitle(e.target.value)} />
        <label htmlFor="exp-hypothesis">What do you want to learn?</label>
        <textarea id="exp-hypothesis" rows={3} value={hypothesis} onChange={(e) => setHypothesis(e.target.value)} />
        <label htmlFor="exp-next">Smallest next action</label>
        <input id="exp-next" value={nextAction} onChange={(e) => setNextAction(e.target.value)} />
        <div>
          <button disabled={!title.trim() || createExperiment.isPending} onClick={() => createExperiment.mutate()}>
            Save experiment
          </button>
        </div>
      </section>
    </div>
  )
}
