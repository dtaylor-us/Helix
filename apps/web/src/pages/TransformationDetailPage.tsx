import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useParams } from '@tanstack/react-router'
import { useState } from 'react'
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
        <h2>Create Experiment</h2>
        <label htmlFor="exp-title">Experiment</label>
        <input id="exp-title" value={title} onChange={(e) => setTitle(e.target.value)} />
        <label htmlFor="exp-hypothesis">Hypothesis</label>
        <textarea id="exp-hypothesis" rows={3} value={hypothesis} onChange={(e) => setHypothesis(e.target.value)} />
        <label htmlFor="exp-next">Suggested next action</label>
        <input id="exp-next" value={nextAction} onChange={(e) => setNextAction(e.target.value)} />
        <button disabled={!title.trim()} onClick={() => createExperiment.mutate()}>
          Save experiment
        </button>
      </section>
    </div>
  )
}
