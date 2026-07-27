import { useQuery } from '@tanstack/react-query'
import { useParams } from '@tanstack/react-router'
import { api } from '../api/http'

export function ReflectionPage() {
  const { id } = useParams({ from: '/reflections/$id' })
  const reflection = useQuery({ queryKey: ['reflection', id], queryFn: () => api.getReflection(id) })

  if (reflection.isLoading) return <p>Loading reflection...</p>
  if (!reflection.data) return <p>Reflection not found.</p>

  return (
    <section className="card">
      <h2>Reflection</h2>
      <p>{reflection.data.content}</p>
      <p className="muted">Saved at: {new Date(reflection.data.createdAt).toLocaleString()}</p>
    </section>
  )
}
