create table weekly_retrospectives (
    id uuid primary key,
    period_start timestamptz not null,
    period_end timestamptz not null,
    summary text not null,
    assistance text not null,
    created_at timestamptz not null
);

create table wisdom_entries (
    id uuid primary key,
    statement text not null,
    status varchar(32) not null,
    retrospective_id uuid references weekly_retrospectives(id) on delete set null,
    created_at timestamptz not null,
    revised_at timestamptz not null
);

create table wisdom_revisions (
    id uuid primary key,
    wisdom_id uuid not null references wisdom_entries(id) on delete cascade,
    previous_statement text not null,
    new_statement text not null,
    reason text not null,
    created_at timestamptz not null
);

create table wisdom_source_links (
    id uuid primary key,
    wisdom_id uuid not null references wisdom_entries(id) on delete cascade,
    source_type varchar(32) not null,
    source_record_id uuid not null,
    note text,
    created_at timestamptz not null
);

create index idx_weekly_retrospectives_created on weekly_retrospectives(created_at desc);
create index idx_wisdom_entries_revised on wisdom_entries(revised_at desc);
create index idx_wisdom_revisions_wisdom_created on wisdom_revisions(wisdom_id, created_at desc);
create index idx_wisdom_sources_wisdom on wisdom_source_links(wisdom_id);
