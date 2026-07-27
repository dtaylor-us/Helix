create table beliefs (
    id uuid primary key,
    transformation_id uuid not null references transformations(id) on delete cascade,
    statement text not null,
    type varchar(32) not null,
    created_at timestamptz not null,
    revised_at timestamptz not null
);

create table belief_revisions (
    id uuid primary key,
    belief_id uuid not null references beliefs(id) on delete cascade,
    previous_statement text not null,
    new_statement text not null,
    previous_type varchar(32) not null,
    new_type varchar(32) not null,
    reason text not null,
    source_evidence_id uuid,
    created_at timestamptz not null
);

create table evidence (
    id uuid primary key,
    belief_id uuid not null references beliefs(id) on delete cascade,
    experiment_id uuid references experiments(id) on delete set null,
    reflection_id uuid references reflections(id) on delete set null,
    summary text not null,
    interpretation text,
    direction varchar(32) not null,
    provenance_source_kind varchar(32) not null,
    provenance_record_type varchar(32) not null,
    provenance_record_id uuid,
    provenance_excerpt text,
    created_at timestamptz not null
);

create index idx_beliefs_transformation_created on beliefs(transformation_id, created_at desc);
create index idx_belief_revisions_belief_created on belief_revisions(belief_id, created_at desc);
create index idx_evidence_belief_created on evidence(belief_id, created_at desc);

alter table belief_revisions
    add constraint fk_belief_revisions_source_evidence
    foreign key (source_evidence_id) references evidence(id) on delete set null;