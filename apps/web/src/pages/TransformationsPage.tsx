import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link } from '@tanstack/react-router'
import { useState } from 'react'
import { TermHint } from '../components/TermHint'
import { api } from '../api/http'

export function TransformationsPage() {
  const queryClient = useQueryClient()
  const [title, setTitle] = useState('')
  const [purpose, setPurpose] = useState('')
  const [desiredIdentity, setDesiredIdentity] = useState('')
  const [obstacle, setObstacle] = useState('')

  const transformations = useQuery({
    queryKey: ['transformations'],
    queryFn: api.listTransformations,
  })

  const createMutation = useMutation({
    mutationFn: () => api.createTransformation({ title, purpose, desiredIdentity, obstacle }),
    onSuccess: () => {
      setTitle('')
      setPurpose('')
      setDesiredIdentity('')
      setObstacle('')
      queryClient.invalidateQueries({ queryKey: ['transformations'] })
    },
  })

  return (
    <div className="stack">
      <section className="card intro-card">
        <h2>Journey</h2>
        <p className="muted">
          A transformation is not a task to check off &mdash; it&rsquo;s a meaningful way you are choosing to grow.
          Each one holds the experiments you run and the evidence you gather along the way.
        </p>
        <TermHint term="Transformation" />
      </section>

      <section className="card">
        <h2>Start a transformation</h2>
        <div className="stack">
          <label htmlFor="title">What would you love to become or experience?</label>
          <input
            id="title"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            placeholder="e.g. Become more peaceful in the face of criticism"
          />
          <label htmlFor="purpose">Why does this matter to you right now?</label>
          <textarea id="purpose" rows={3} value={purpose} onChange={(e) => setPurpose(e.target.value)} />
          <label htmlFor="desired-identity">Who are you becoming through this? (optional)</label>
          <textarea
            id="desired-identity"
            rows={2}
            value={desiredIdentity}
            onChange={(e) => setDesiredIdentity(e.target.value)}
            placeholder="e.g. Someone who can hear feedback without spiraling"
          />
          <label htmlFor="obstacle">What currently gets in the way? (optional)</label>
          <textarea
            id="obstacle"
            rows={2}
            value={obstacle}
            onChange={(e) => setObstacle(e.target.value)}
            placeholder="e.g. I treat feedback as a verdict on who I am, not on what I did"
          />
          <div>
            <button disabled={!title.trim() || createMutation.isPending} onClick={() => createMutation.mutate()}>
              Save transformation
            </button>
          </div>
        </div>
      </section>

      <section className="card">
        <h2>Your transformations</h2>
        {transformations.isLoading && <p>Loading...</p>}
        {transformations.data && transformations.data.length > 0 && (
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
        {transformations.data && transformations.data.length === 0 && (
          <p className="muted">Nothing here yet &mdash; start one above.</p>
        )}
      </section>
    </div>
  )
}
