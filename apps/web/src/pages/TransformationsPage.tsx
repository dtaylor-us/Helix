import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link } from '@tanstack/react-router'
import { useState } from 'react'
import { api } from '../api/http'

export function TransformationsPage() {
  const queryClient = useQueryClient()
  const [title, setTitle] = useState('')
  const [purpose, setPurpose] = useState('')

  const transformations = useQuery({
    queryKey: ['transformations'],
    queryFn: api.listTransformations,
  })

  const createMutation = useMutation({
    mutationFn: () => api.createTransformation({ title, purpose }),
    onSuccess: () => {
      setTitle('')
      setPurpose('')
      queryClient.invalidateQueries({ queryKey: ['transformations'] })
    },
  })

  return (
    <div className="stack">
      <section className="card">
        <h2>Create Transformation</h2>
        <div className="stack">
          <label htmlFor="title">Title</label>
          <input id="title" value={title} onChange={(e) => setTitle(e.target.value)} />
          <label htmlFor="purpose">Purpose</label>
          <textarea id="purpose" rows={3} value={purpose} onChange={(e) => setPurpose(e.target.value)} />
          <button disabled={!title.trim()} onClick={() => createMutation.mutate()}>
            Save transformation
          </button>
        </div>
      </section>

      <section className="card">
        <h2>Transformations</h2>
        {transformations.isLoading && <p>Loading...</p>}
        {transformations.data && (
          <ul>
            {transformations.data.map((item) => (
              <li key={item.id}>
                <Link to="/transformations/$id" params={{ id: item.id }}>
                  {item.title}
                </Link>
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  )
}
