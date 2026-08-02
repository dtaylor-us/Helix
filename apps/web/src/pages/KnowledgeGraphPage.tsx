import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link, useParams } from '@tanstack/react-router'
import { useMemo, useState } from 'react'
import type { GraphEdge, GraphNode, KnowledgeNodeType } from '../../../../packages/contracts/src/index'
import { api } from '../api/http'

const NODE_TYPE_LABELS: Record<KnowledgeNodeType, string> = {
  TRANSFORMATION: 'Transformation',
  BELIEF: 'Belief',
  EXPERIMENT: 'Experiment',
  EVIDENCE: 'Evidence',
  REFLECTION: 'Reflection',
  WISDOM: 'Wisdom',
  MEMORY: 'Memory',
}

const ALL_NODE_TYPES = Object.keys(NODE_TYPE_LABELS) as KnowledgeNodeType[]

/**
 * Phase 11C (ADR-020): a bounded, focus-node-centered exploration view. Renders a calm radial
 * diagram (no external graph library — the scoping doc flagged bundle size as a concern for a
 * capability most sessions won't touch) alongside a fully accessible structured-list alternative,
 * per the brief's accessibility requirement that the graph never be the only way to see this data.
 */
export function KnowledgeGraphPage() {
  const { nodeType, sourceRecordId } = useParams({ from: '/knowledge-graph/$nodeType/$sourceRecordId' })
  const queryClient = useQueryClient()
  const [selectedNodeId, setSelectedNodeId] = useState<string | null>(null)
  const [hiddenTypes, setHiddenTypes] = useState<Set<KnowledgeNodeType>>(new Set())
  const [viewMode, setViewMode] = useState<'diagram' | 'list'>('diagram')

  const focusType = nodeType as KnowledgeNodeType

  const graphQuery = useQuery({
    queryKey: ['knowledge-graph', focusType, sourceRecordId],
    queryFn: () => api.getGraphFocus(focusType, sourceRecordId),
  })

  const rebuild = useMutation({
    mutationFn: () => api.rebuildGraph(),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['knowledge-graph', focusType, sourceRecordId] })
    },
  })

  const confirmEdge = useMutation({
    mutationFn: (edgeId: string) => api.confirmGraphEdge(edgeId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['knowledge-graph', focusType, sourceRecordId] }),
  })
  const rejectEdge = useMutation({
    mutationFn: (edgeId: string) => api.rejectGraphEdge(edgeId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['knowledge-graph', focusType, sourceRecordId] }),
  })
  const hideEdge = useMutation({
    mutationFn: (edgeId: string) => api.hideGraphEdge(edgeId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['knowledge-graph', focusType, sourceRecordId] }),
  })

  const view = graphQuery.data
  const visibleNodes = useMemo(
    () => (view ? view.nodes.filter((n) => n.id === view.focusNodeId || !hiddenTypes.has(n.type)) : []),
    [view, hiddenTypes],
  )
  const visibleNodeIds = useMemo(() => new Set(visibleNodes.map((n) => n.id)), [visibleNodes])
  const visibleEdges = useMemo(
    () => (view ? view.edges.filter((e) => visibleNodeIds.has(e.sourceNodeId) && visibleNodeIds.has(e.targetNodeId)) : []),
    [view, visibleNodeIds],
  )

  const selectedNode = visibleNodes.find((n) => n.id === selectedNodeId) ?? null

  function toggleType(type: KnowledgeNodeType) {
    setHiddenTypes((prev) => {
      const next = new Set(prev)
      if (next.has(type)) next.delete(type)
      else next.add(type)
      return next
    })
  }

  return (
    <div className="stack">
      <section className="card">
        <h2>{view?.title ?? 'Connections'}</h2>
        {view?.description && <p className="muted">{view.description}</p>}
        {view?.truncated && (
          <p className="muted" role="status">
            This view is limited to the closest connections. There may be more than what&rsquo;s shown here.
          </p>
        )}
        <div className="row">
          <button
            type="button"
            className={viewMode === 'diagram' ? 'secondary-button active-item' : 'secondary-button'}
            onClick={() => setViewMode('diagram')}
          >
            Diagram
          </button>
          <button
            type="button"
            className={viewMode === 'list' ? 'secondary-button active-item' : 'secondary-button'}
            onClick={() => setViewMode('list')}
          >
            List
          </button>
          <button type="button" className="secondary-button" onClick={() => rebuild.mutate()} disabled={rebuild.isPending}>
            {rebuild.isPending ? 'Refreshing…' : 'Refresh connections'}
          </button>
        </div>
      </section>

      {graphQuery.isLoading && (
        <section className="card">
          <p>Loading connections…</p>
        </section>
      )}

      {graphQuery.isError && (
        <section className="card">
          <p className="muted">
            We couldn&rsquo;t load this view. This can happen if connections haven&rsquo;t been built yet.
          </p>
          <button type="button" onClick={() => rebuild.mutate()} disabled={rebuild.isPending}>
            {rebuild.isPending ? 'Building…' : 'Build connections'}
          </button>
        </section>
      )}

      {view && (
        <>
          <section className="card">
            <h3>Filter by type</h3>
            <div className="row" role="group" aria-label="Filter connections by type">
              {ALL_NODE_TYPES.map((type) => (
                <label key={type} className="muted" style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-2)' }}>
                  <input
                    type="checkbox"
                    checked={!hiddenTypes.has(type)}
                    onChange={() => toggleType(type)}
                  />
                  {NODE_TYPE_LABELS[type]}
                </label>
              ))}
            </div>
          </section>

          {visibleNodes.length <= 1 && (
            <section className="card">
              <p>No connections yet for this record.</p>
            </section>
          )}

          {visibleNodes.length > 1 && viewMode === 'diagram' && (
            <section className="card">
              <GraphDiagram
                nodes={visibleNodes}
                edges={visibleEdges}
                focusNodeId={view.focusNodeId}
                selectedNodeId={selectedNodeId}
                onSelectNode={setSelectedNodeId}
              />
            </section>
          )}

          {visibleNodes.length > 1 && viewMode === 'list' && (
            <section className="card">
              <h3>Connections</h3>
              <ul className="stack" style={{ listStyle: 'none', padding: 0 }}>
                {visibleEdges.map((edge) => {
                  const source = visibleNodes.find((n) => n.id === edge.sourceNodeId)
                  const target = visibleNodes.find((n) => n.id === edge.targetNodeId)
                  if (!source || !target) return null
                  return (
                    <li key={edge.id} className="timeline-item">
                      <p>
                        <strong>{source.label}</strong> — {edge.displayLabel.toLowerCase()} — <strong>{target.label}</strong>
                      </p>
                      {edge.explanation && <p className="muted">{edge.explanation}</p>}
                      <EdgeGovernanceActions
                        edge={edge}
                        onConfirm={() => confirmEdge.mutate(edge.id)}
                        onReject={() => rejectEdge.mutate(edge.id)}
                        onHide={() => hideEdge.mutate(edge.id)}
                      />
                    </li>
                  )
                })}
              </ul>
            </section>
          )}

          {selectedNode && (
            <section className="card">
              <h3>{selectedNode.label}</h3>
              <p className="muted">{NODE_TYPE_LABELS[selectedNode.type]}</p>
              {selectedNode.summary && <p>{selectedNode.summary}</p>}
              {selectedNode.status && <p className="muted">Status: {selectedNode.status.toLowerCase()}</p>}
              {selectedNode.sourceRoute && (
                <Link to={selectedNode.sourceRoute} className="secondary-button">
                  View full record
                </Link>
              )}
            </section>
          )}
        </>
      )}
    </div>
  )
}

function EdgeGovernanceActions({
  edge,
  onConfirm,
  onReject,
  onHide,
}: {
  edge: GraphEdge
  onConfirm: () => void
  onReject: () => void
  onHide: () => void
}) {
  // Phase 11D: only AI-proposed edges (Phase 11E) ever need review — every edge shipped in 11B/11C
  // is auto-confirmed, so this has nothing to show today, but the wiring is in place for 11E.
  if (edge.status !== 'PROPOSED') {
    return null
  }

  return (
    <div className="row">
      <p className="muted">Suggested connection — review before it&rsquo;s kept.</p>
      <button type="button" onClick={onConfirm}>
        Confirm
      </button>
      <button type="button" className="secondary-button" onClick={onReject}>
        Reject
      </button>
      <button type="button" className="secondary-button" onClick={onHide}>
        Hide
      </button>
    </div>
  )
}

function GraphDiagram({
  nodes,
  edges,
  focusNodeId,
  selectedNodeId,
  onSelectNode,
}: {
  nodes: GraphNode[]
  edges: GraphEdge[]
  focusNodeId: string
  selectedNodeId: string | null
  onSelectNode: (id: string) => void
}) {
  const size = 480
  const center = size / 2
  const radius = size / 2 - 64

  const positions = useMemo(() => {
    const others = nodes.filter((n) => n.id !== focusNodeId)
    const map = new Map<string, { x: number; y: number }>()
    map.set(focusNodeId, { x: center, y: center })
    others.forEach((node, index) => {
      const angle = (2 * Math.PI * index) / Math.max(others.length, 1) - Math.PI / 2
      map.set(node.id, {
        x: center + radius * Math.cos(angle),
        y: center + radius * Math.sin(angle),
      })
    })
    return map
  }, [nodes, focusNodeId, center, radius])

  return (
    <svg
      role="img"
      aria-label="Diagram of connections. Use the List view above for a fully accessible version of this information."
      viewBox={`0 0 ${size} ${size}`}
      width="100%"
      style={{ maxWidth: `${size}px`, display: 'block', margin: '0 auto' }}
    >
      {edges.map((edge) => {
        const from = positions.get(edge.sourceNodeId)
        const to = positions.get(edge.targetNodeId)
        if (!from || !to) return null
        return (
          <line
            key={edge.id}
            x1={from.x}
            y1={from.y}
            x2={to.x}
            y2={to.y}
            stroke="var(--border)"
            strokeWidth={2}
          />
        )
      })}
      {nodes.map((node) => {
        const position = positions.get(node.id)
        if (!position) return null
        const isFocus = node.id === focusNodeId
        const isSelected = node.id === selectedNodeId
        return (
          <g
            key={node.id}
            transform={`translate(${position.x}, ${position.y})`}
            onClick={() => onSelectNode(node.id)}
            style={{ cursor: 'pointer' }}
          >
            <circle
              r={isFocus ? 20 : 14}
              fill={isFocus ? 'var(--accent)' : isSelected ? 'var(--provenance)' : 'var(--surface-soft)'}
              stroke={isSelected ? 'var(--provenance)' : 'var(--border)'}
              strokeWidth={isSelected ? 3 : 1.5}
            />
            <text
              y={isFocus ? 36 : 30}
              textAnchor="middle"
              fontSize="11"
              fill="var(--ink)"
            >
              {truncateLabel(node.label)}
            </text>
          </g>
        )
      })}
    </svg>
  )
}

function truncateLabel(label: string): string {
  return label.length > 22 ? `${label.slice(0, 21)}…` : label
}
