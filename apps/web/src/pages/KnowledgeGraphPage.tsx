import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link, useParams } from '@tanstack/react-router'
import { useMemo, useState } from 'react'
import type { GraphEdge, GraphEdgeHistory, GraphNode, KnowledgeNodeType } from '../../../../packages/contracts/src/index'
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

// One color per node type so the diagram, the legend, and the filter checkboxes all agree on what
// each type looks like — previously every non-focus node shared the same fill, so the only way to
// tell a belief from a reflection was reading truncated label text under overlapping lines.
const NODE_TYPE_COLORS: Record<KnowledgeNodeType, { solid: string; soft: string }> = {
  TRANSFORMATION: { solid: 'var(--node-transformation)', soft: 'var(--node-transformation-soft)' },
  BELIEF: { solid: 'var(--node-belief)', soft: 'var(--node-belief-soft)' },
  EXPERIMENT: { solid: 'var(--node-experiment)', soft: 'var(--node-experiment-soft)' },
  EVIDENCE: { solid: 'var(--node-evidence)', soft: 'var(--node-evidence-soft)' },
  REFLECTION: { solid: 'var(--node-reflection)', soft: 'var(--node-reflection-soft)' },
  WISDOM: { solid: 'var(--node-wisdom)', soft: 'var(--node-wisdom-soft)' },
  MEMORY: { solid: 'var(--node-memory)', soft: 'var(--node-memory-soft)' },
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
  const [discoveryStatusText, setDiscoveryStatusText] = useState<string | null>(null)

  const focusType = nodeType as KnowledgeNodeType

  const graphQuery = useQuery({
    queryKey: ['knowledge-graph', focusType, sourceRecordId],
    queryFn: () => api.getGraphFocus(focusType, sourceRecordId),
    // Bug fix (QA finding KG-4): a missing/stale focus node is a deterministic 404, not a transient
    // failure -- retrying it just delays the "Build connections" recovery action by several seconds
    // for no benefit.
    retry: false,
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

  // Phase 11E: a bounded, manually triggered pass comparing beliefs with no existing connection.
  // Anything found lands as a PROPOSED edge for review via the confirm/reject/hide actions below —
  // it never appears already-confirmed.
  const discoverRelationships = useMutation({
    mutationFn: () => api.discoverGraphRelationships(),
    onSuccess: (result) => {
      setDiscoveryStatusText(
        result.proposalsCreated > 0
          ? `Found ${result.proposalsCreated} possible connection${result.proposalsCreated === 1 ? '' : 's'} to review, out of ${result.pairsEvaluated} pair${result.pairsEvaluated === 1 ? '' : 's'} checked.`
          : `Checked ${result.pairsEvaluated} pair${result.pairsEvaluated === 1 ? '' : 's'} of beliefs — nothing new to review.`,
      )
      queryClient.invalidateQueries({ queryKey: ['knowledge-graph', focusType, sourceRecordId] })
    },
    onError: () => {
      setDiscoveryStatusText('Could not check for new connections right now.')
    },
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
      <section className="card kg-header">
        <div className="kg-header-copy">
          <h2>{view?.title ?? 'Connections'}</h2>
          {view?.description && <p className="muted">{view.description}</p>}
        </div>
        {view?.truncated && (
          <p className="muted" role="status">
            This view is limited to the closest connections. There may be more than what&rsquo;s shown here.
          </p>
        )}
        <div className="row kg-actions">
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
          <button
            type="button"
            className="secondary-button"
            onClick={() => discoverRelationships.mutate()}
            disabled={discoverRelationships.isPending}
          >
            {discoverRelationships.isPending ? 'Checking…' : 'Check for new connections'}
          </button>
        </div>
        {discoveryStatusText && (
          <p role="status" aria-live="polite" className="muted">
            {discoveryStatusText}
          </p>
        )}
        {view && (
          <div className="kg-filter" role="group" aria-label="Filter connections by type">
            <span className="kg-filter-label">Show</span>
            {ALL_NODE_TYPES.map((type) => (
              <label key={type} className="kg-filter-option">
                <input
                  type="checkbox"
                  checked={!hiddenTypes.has(type)}
                  onChange={() => toggleType(type)}
                />
                <ColorSwatch type={type} />
                {NODE_TYPE_LABELS[type]}
              </label>
            ))}
          </div>
        )}
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
          {visibleNodes.length <= 1 && (
            <section className="card">
              <p>No connections yet for this record.</p>
            </section>
          )}

          {visibleNodes.length > 1 && viewMode === 'diagram' && (
            <div className="kg-workspace">
              <section className="card kg-canvas-card">
                <GraphDiagram
                  nodes={visibleNodes}
                  edges={visibleEdges}
                  focusNodeId={view.focusNodeId}
                  selectedNodeId={selectedNodeId}
                  onSelectNode={setSelectedNodeId}
                />
              </section>
              <NodeInspector node={selectedNode} />
            </div>
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
                      <EdgeHistoryLine history={edge.history} />
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

        </>
      )}
    </div>
  )
}

function NodeInspector({ node }: { node: GraphNode | null }) {
  return (
    <aside className="card kg-inspector" aria-live="polite" aria-label="Selected connection details">
      {node ? (
        <>
          <div className="kg-inspector-heading">
            <ColorSwatch type={node.type} />
            <p className="kg-eyebrow">{NODE_TYPE_LABELS[node.type]}</p>
          </div>
          <h3>{node.label}</h3>
          {node.summary && <p>{node.summary}</p>}
          {node.status && <p className="muted">Status: {node.status.toLowerCase()}</p>}
          {node.sourceRoute && <RecordLink route={node.sourceRoute} />}
        </>
      ) : (
        <>
          <p className="kg-eyebrow">Details</p>
          <h3>Select a connection</h3>
          <p className="muted">Choose any node to see its summary, status, and link to the full record.</p>
        </>
      )}
    </aside>
  )
}

// Phase 11F: a lightweight, non-animated history line — just the dates this edge actually has, not
// a fabricated richer timeline. effectiveFrom/effectiveTo are reserved for a future feature and are
// typically absent today.
function EdgeHistoryLine({ history }: { history: GraphEdgeHistory }) {
  const parts: string[] = [`Noticed ${formatDate(history.createdAt)}`]
  if (history.confirmedAt) parts.push(`confirmed ${formatDate(history.confirmedAt)}`)
  if (history.rejectedAt) parts.push(`rejected ${formatDate(history.rejectedAt)}`)
  if (history.effectiveFrom) parts.push(`effective from ${formatDate(history.effectiveFrom)}`)
  if (history.effectiveTo) parts.push(`effective until ${formatDate(history.effectiveTo)}`)

  return <p className="muted">{parts.join(' · ')}</p>
}

function formatDate(iso: string): string {
  const parsed = new Date(iso)
  return Number.isNaN(parsed.getTime()) ? iso : parsed.toLocaleDateString()
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
  const size = 520
  const center = size / 2
  // More neighbors need more room to keep labels from colliding — grow the ring radius (capped)
  // rather than keeping it fixed regardless of how crowded the view is.
  const others = nodes.length - 1
  const radius = Math.min(size / 2 - 70, 130 + Math.max(0, others - 6) * 10)

  const positions = useMemo(() => {
    const otherNodes = nodes.filter((n) => n.id !== focusNodeId)
    const map = new Map<string, { x: number; y: number }>()
    map.set(focusNodeId, { x: center, y: center })
    otherNodes.forEach((node, index) => {
      const angle = (2 * Math.PI * index) / Math.max(otherNodes.length, 1) - Math.PI / 2
      map.set(node.id, {
        x: center + radius * Math.cos(angle),
        y: center + radius * Math.sin(angle),
      })
    })
    return map
  }, [nodes, focusNodeId, center, radius])

  const typesPresent = useMemo(() => Array.from(new Set(nodes.map((n) => n.type))), [nodes])

  function nodeRadius(node: GraphNode) {
    return node.id === focusNodeId ? 22 : 15
  }

  return (
    <div className="kg-diagram">
      <svg
        role="img"
        aria-label="Diagram of connections. Use the List view above for a fully accessible version of this information."
        viewBox={`0 0 ${size} ${size}`}
        width="100%"
        className="kg-diagram-svg"
      >
        <defs>
          <marker id="kg-arrow" viewBox="0 0 10 10" refX="8" refY="5" markerWidth="7" markerHeight="7" orient="auto-start-reverse">
            <path d="M 0 0 L 10 5 L 0 10 z" fill="var(--muted)" />
          </marker>
        </defs>
        {edges.map((edge) => {
          const from = positions.get(edge.sourceNodeId)
          const to = positions.get(edge.targetNodeId)
          const targetNode = nodes.find((n) => n.id === edge.targetNodeId)
          if (!from || !to || !targetNode) return null
          // Stop the line (and its arrowhead) at the target circle's edge, not its center, so the
          // arrow is actually visible instead of hiding under the node.
          const end = shortenToEdge(from, to, nodeRadius(targetNode) + 4)
          return (
            <line
              key={edge.id}
              x1={from.x}
              y1={from.y}
              x2={end.x}
              y2={end.y}
              stroke="var(--border)"
              strokeWidth={2}
              markerEnd="url(#kg-arrow)"
            />
          )
        })}
        {nodes.map((node) => {
          const position = positions.get(node.id)
          if (!position) return null
          const isFocus = node.id === focusNodeId
          const isSelected = node.id === selectedNodeId
          const colors = NODE_TYPE_COLORS[node.type]
          const r = nodeRadius(node)
          return (
            <g
              key={node.id}
              transform={`translate(${position.x}, ${position.y})`}
              role="button"
              tabIndex={0}
              aria-label={`${isFocus ? 'Focus, ' : ''}${NODE_TYPE_LABELS[node.type]}: ${node.label}`}
              aria-pressed={isSelected}
              onClick={() => onSelectNode(node.id)}
              onKeyDown={(event) => {
                if (event.key === 'Enter' || event.key === ' ') {
                  event.preventDefault()
                  onSelectNode(node.id)
                }
              }}
              style={{ cursor: 'pointer' }}
            >
              <title>{`${isFocus ? 'Focus — ' : ''}${NODE_TYPE_LABELS[node.type]}: ${node.label}`}</title>
              {isFocus && <circle r={r + 6} fill="none" stroke="var(--accent)" strokeWidth={2} />}
              {isSelected && <circle r={r + (isFocus ? 11 : 5)} fill="none" stroke="var(--accent)" strokeWidth={2} strokeDasharray="3 3" />}
              <circle
                r={r}
                fill={colors.soft}
                stroke={colors.solid}
                strokeWidth={isFocus ? 3 : 2}
              />
              {/* Text drawn with a canvas-colored halo (paint-order trick) so labels stay legible
                  where an edge line crosses behind them, without needing to measure text width. */}
              <text
                y={r + 16}
                textAnchor="middle"
                fontSize={isFocus ? 12 : 11}
                fontWeight={isFocus ? 650 : 550}
                fill="var(--ink)"
                stroke="var(--surface)"
                strokeWidth={4}
                strokeLinejoin="round"
                paintOrder="stroke"
              >
                {truncateLabel(node.label)}
              </text>
            </g>
          )
        })}
      </svg>
      <div className="kg-legend" aria-hidden="true">
        {typesPresent.map((type) => (
          <span key={type} className="kg-legend-item">
            <ColorSwatch type={type} />
            {NODE_TYPE_LABELS[type]}
          </span>
        ))}
        <span className="kg-legend-item">
          <span style={{ width: '0.75rem', height: '0.75rem', flex: '0 0 0.75rem', borderRadius: '50%', border: '2px solid var(--accent)', display: 'inline-block' }} />
          Focus
        </span>
        <span className="kg-legend-item">
          <span style={{ width: '0.75rem', height: '0.75rem', flex: '0 0 0.75rem', borderRadius: '50%', border: '2px dashed var(--accent)', display: 'inline-block' }} />
          Selected
        </span>
      </div>
    </div>
  )
}

// Moves the line's endpoint back along the source→target vector so it stops at the target node's
// visible edge (plus a small gap for the arrowhead) instead of running under the circle.
function shortenToEdge(from: { x: number; y: number }, to: { x: number; y: number }, pullback: number) {
  const dx = to.x - from.x
  const dy = to.y - from.y
  const distance = Math.sqrt(dx * dx + dy * dy) || 1
  const ratio = Math.max(0, (distance - pullback) / distance)
  return { x: from.x + dx * ratio, y: from.y + dy * ratio }
}

function ColorSwatch({ type }: { type: KnowledgeNodeType }) {
  const colors = NODE_TYPE_COLORS[type]
  return (
    <span
      aria-hidden="true"
      style={{
        display: 'inline-block',
        width: '0.75rem',
        height: '0.75rem',
        flex: '0 0 0.75rem',
        borderRadius: '50%',
        background: colors.soft,
        border: `2px solid ${colors.solid}`,
      }}
    />
  )
}

// Bug fix (QA finding KG-3): sourceRoute can carry a query string (e.g. "/knowledge?beliefId=...")
// so the target page can select the specific record the graph meant. Passing that raw string
// straight into Link's `to` prop would treat the "?..." as a literal, unencoded path segment
// instead of search params, so it's split here and passed via Link's `search` prop instead.
function RecordLink({ route }: { route: string }) {
  const [pathname, search] = route.split('?')
  const searchParams = search ? Object.fromEntries(new URLSearchParams(search)) : undefined

  return (
    <Link to={pathname} search={searchParams} className="secondary-button">
      View full record
    </Link>
  )
}

function truncateLabel(label: string): string {
  return label.length > 22 ? `${label.slice(0, 21)}…` : label
}
