create extension if not exists "uuid-ossp";

create table transformations (
    id uuid primary key,
    title varchar(140) not null,
    purpose text,
    created_at timestamptz not null
);

create table experiments (
    id uuid primary key,
    transformation_id uuid not null references transformations(id) on delete cascade,
    title varchar(180) not null,
    hypothesis text,
    next_action text,
    status varchar(32) not null,
    created_at timestamptz not null
);

create table reflections (
    id uuid primary key,
    experiment_id uuid not null references experiments(id) on delete cascade,
    content text not null,
    created_at timestamptz not null
);

create table suggestions (
    id uuid primary key,
    experiment_id uuid not null references experiments(id) on delete cascade,
    reflection_id uuid references reflections(id) on delete set null,
    text text not null,
    status varchar(32) not null,
    replacement_text text,
    created_at timestamptz not null,
    responded_at timestamptz
);

create index idx_experiments_transformation on experiments(transformation_id);
create index idx_reflections_experiment_created on reflections(experiment_id, created_at desc);
create index idx_suggestions_experiment_created on suggestions(experiment_id, created_at desc);
