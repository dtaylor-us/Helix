-- ADR-021: Google SSO authentication and owner-scoped data isolation.
-- Helix is moving from single-user to "a few invited people" -- every existing table is currently
-- global/unscoped. This migration (1) introduces the identity model (users + an invite-only
-- allowlist) and (2) retrofits every existing table with an owner_id.
--
-- owner_id is added NOT NULL with a temporary default of the bootstrap user's id, so any pre-existing
-- dev/test data (there is no real production data yet -- this is pre-deployment) is attributed to the
-- bootstrap account rather than left orphaned. The default is then dropped: every INSERT going
-- forward must supply owner_id explicitly, so any application code path that forgets to set it fails
-- loudly (a NOT NULL violation) instead of silently leaving a row unscoped/unowned.

create table users (
    id uuid primary key,
    email varchar(320) not null,
    display_name text,
    google_sub varchar(255) not null,
    created_at timestamptz not null,
    constraint uq_users_email unique (email),
    constraint uq_users_google_sub unique (google_sub)
);

-- Invite-only allowlist checked at OAuth2 login before a users row is created or reused. A row here
-- does not itself grant an account -- it just permits one to be created on next successful Google
-- login. Managed as data (insert a row to invite someone), not as a config property, so inviting a
-- person doesn't require a redeploy.
create table authorized_users (
    email varchar(320) primary key,
    invited_at timestamptz not null,
    note text
);

-- Bootstrap: the product owner's own account is pre-authorized and pre-created so the app isn't
-- locked out on first deploy. google_sub is a placeholder overwritten by the real Google subject id
-- on first successful login (see AuthService) -- it is never trusted for lookups until then, only
-- email is used to match the allowlist on first login.
insert into authorized_users (email, invited_at, note)
values ('derektaylor.us@gmail.com', now(), 'Bootstrap owner account, pre-authorized at migration time.');

insert into users (id, email, display_name, google_sub, created_at)
values ('00000000-0000-0000-0000-0000000000b1', 'derektaylor.us@gmail.com', 'Derek Taylor',
        'pending-first-login-00000000-0000-0000-0000-0000000000b1', now());

do $$
declare
    bootstrap_user_id uuid := '00000000-0000-0000-0000-0000000000b1';
    t text;
    tables text[] := array[
        'transformations', 'experiments', 'reflections', 'suggestions',
        'beliefs', 'belief_revisions', 'evidence',
        'weekly_retrospectives', 'wisdom_entries', 'wisdom_revisions', 'wisdom_source_links',
        'memory_proposals', 'memory_proposal_revisions',
        'knowledge_node', 'knowledge_edge', 'knowledge_edge_source', 'knowledge_projection_checkpoint',
        'semantic_search_documents'
    ];
begin
    foreach t in array tables loop
        execute format(
            'alter table %I add column owner_id uuid not null default %L references users(id)',
            t, bootstrap_user_id
        );
        execute format('alter table %I alter column owner_id drop default', t);
        execute format('create index %I on %I (owner_id)', 'idx_' || t || '_owner', t);
    end loop;
end
$$;

-- onboarding_state was a singleton row (fixed id) under the single-user assumption (see V10's own
-- comment anticipating this). It becomes one row per user: the bootstrap row is re-keyed to the
-- bootstrap user's id-as-onboarding-id isn't meaningful anymore, so instead owner_id is added the
-- same way and the app now looks up onboarding state by owner_id, not by the fixed singleton id.
alter table onboarding_state add column owner_id uuid references users(id);
update onboarding_state set owner_id = '00000000-0000-0000-0000-0000000000b1';
alter table onboarding_state alter column owner_id set not null;
create unique index idx_onboarding_state_owner on onboarding_state (owner_id);
