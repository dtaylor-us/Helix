-- Phase 11B: knowledge graph projection foundation (ADR-020).
-- Purely a projection over authoritative domain records -- this schema owns no business data, only
-- graph structure and provenance. Rebuildable from scratch at any time via a full-rebuild operation
-- (see KnowledgeGraphProjectionService); node/edge ids are NOT stable across rebuilds, but each
-- node's (node_type, source_record_id) pair is, and that pair is what the app should key off for
-- anything long-lived (URLs, bookmarks, etc.).

create table knowledge_node (
    id uuid primary key,
    node_type varchar(32) not null,
    source_record_id uuid not null,
    display_label text not null,
    summary text,
    lifecycle_status varchar(32),
    created_at timestamptz not null,
    updated_at timestamptz not null,
    constraint uq_knowledge_node_type_record unique (node_type, source_record_id)
);

create table knowledge_edge (
    id uuid primary key,
    source_node_id uuid not null references knowledge_node(id) on delete cascade,
    target_node_id uuid not null references knowledge_node(id) on delete cascade,
    relationship_type varchar(64) not null,
    origin varchar(32) not null,
    status varchar(32) not null,
    confidence varchar(20) not null,
    explanation text not null,
    created_at timestamptz not null,
    effective_from timestamptz,
    effective_to timestamptz,
    ai_invocation_id uuid,
    confirmed_at timestamptz,
    rejected_at timestamptz,
    superseded_by_edge_id uuid references knowledge_edge(id) on delete set null
);

-- Supporting authoritative records for an edge (a single edge may be justified by more than one
-- source record -- e.g. a two-hop deterministic derivation cites both linking records).
create table knowledge_edge_source (
    id uuid primary key,
    knowledge_edge_id uuid not null references knowledge_edge(id) on delete cascade,
    record_type varchar(32) not null,
    record_id uuid not null
);

-- One row per source module, recording when it was last folded into the projection. Used only for
-- a freshness indicator in the first release -- the first release does full rebuilds only, not
-- incremental per-module sync (see docs/product/knowledge-graph-scoping.md Section 12, Q16).
create table knowledge_projection_checkpoint (
    id uuid primary key,
    source_module varchar(64) not null,
    last_projected_at timestamptz not null,
    constraint uq_knowledge_checkpoint_module unique (source_module)
);

create index idx_knowledge_node_type on knowledge_node(node_type);
create index idx_knowledge_edge_source_node on knowledge_edge(source_node_id);
create index idx_knowledge_edge_target_node on knowledge_edge(target_node_id);
create index idx_knowledge_edge_status on knowledge_edge(status);
create index idx_knowledge_edge_source_edge on knowledge_edge_source(knowledge_edge_id);
