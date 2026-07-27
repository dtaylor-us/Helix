import { useQuery } from '@tanstack/react-query'
import { useParams } from '@tanstack/react-router'
import { api } from '../api/http'

export function ExperimentPage() {
  const { id } = useParams({ from: '/experiments/$id' })
  const experiment = useQuery({ queryKey: ['experiment', id], queryFn: () => api.getExperiment(id) })

  if (experiment.isLoading) return <p>Loading experiment...</p>
  if (!experiment.data) return <p>Experiment not found.</p>

  return (
    <section className="card">
      <h2>{experiment.data.title}</h2>
      <p>{experiment.data.hypothesis}</p>
      <p className="muted">Next action: {experiment.data.nextAction || 'none yet'}</p>
    </section>
  )
}
