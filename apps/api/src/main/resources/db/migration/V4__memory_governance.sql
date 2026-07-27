create table memory_proposals (
    id uuid primary key,
    statement text not null,
    status varchar(32) not null,
    source_kind varchar(32) not null,
    source_record_type varchar(32) not null,
    source_record_id uuid not null,
    source_excerpt text,
    created_at timestamptz not null,
    revised_at timestamptz not null
);

create table memory_proposal_revisions (
    id uuid primary key,
    memory_proposal_id uuid not null references memory_proposals(id) on delete cascade,
    previous_statement text not null,
    new_statement text not null,
    previous_status varchar(32) not null,
    new_status varchar(32) not null,
    reason text not null,
    created_at timestamptz not null
);

create index idx_memory_proposals_revised on memory_proposals(revised_at desc);
create index idx_memory_proposal_revisions_memory_created on memory_proposal_revisions(memory_proposal_id, created_at desc);