do $$
begin
    create extension if not exists vector;
exception
    when others then
        raise notice 'pgvector extension unavailable in current environment; using application-side semantic scoring fallback';
end
$$;

create table semantic_search_documents (
    id uuid primary key,
    record_type varchar(32) not null,
    record_id uuid not null,
    snippet text not null,
    embedding_values text not null,
    source_updated_at timestamptz not null,
    indexed_at timestamptz not null,
    constraint uq_semantic_search_record unique (record_type, record_id)
);

create index idx_semantic_search_record_type on semantic_search_documents(record_type);
create index idx_semantic_search_indexed_at on semantic_search_documents(indexed_at desc);