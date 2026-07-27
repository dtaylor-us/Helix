import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { api } from '../api/http'

export function SearchPage() {
  const [query, setQuery] = useState('')

  const searchQuery = useQuery({
    queryKey: ['search', query],
    queryFn: () => api.search(query),
    enabled: query.trim().length > 1,
  })

  return (
    <section className="card stack">
      <h2>Search records</h2>
      <p className="muted">Search across reflections, beliefs, evidence, retrospectives, and wisdom entries.</p>
      <label htmlFor="search-query">Keyword</label>
      <input
        id="search-query"
        value={query}
        onChange={(event) => setQuery(event.target.value)}
        placeholder="Try: momentum, consistency, challenge"
      />

      {query.trim().length <= 1 && <p>Enter at least two characters to search.</p>}
      {searchQuery.isLoading && <p>Searching...</p>}
      {searchQuery.data && (
        <>
          <p className="muted">{searchQuery.data.note}</p>
          {searchQuery.data.results.length > 0 ? (
            <ul>
              {searchQuery.data.results.map((result) => (
                <li key={`${result.recordType}-${result.recordId}`}>
                  <strong>{result.recordType}</strong> ({result.matchType.toLowerCase()}, {result.score.toFixed(2)}): {result.snippet}
                </li>
              ))}
            </ul>
          ) : (
            <p>No matches found.</p>
          )}
        </>
      )}
    </section>
  )
}
