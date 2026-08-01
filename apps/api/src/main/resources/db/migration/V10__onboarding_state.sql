-- Phase 7: server-persisted onboarding state (replaces the purely client-derived
-- transformations.length === 0 welcome-state check carried since Phase 1).
--
-- Helix is currently single-user with no auth (ADR-013 defers auth behind a port), so this is a
-- singleton row rather than a per-user table -- the same shape auth would later key by user_id.
-- Status values (application-enforced, not a DB enum, consistent with every other status column
-- in this schema): NOT_STARTED, FIRST_TRANSFORMATION_CREATED, COMPLETE.

create table onboarding_state (
    id uuid primary key,
    status varchar(40) not null,
    updated_at timestamptz not null
);

insert into onboarding_state (id, status, updated_at)
values ('00000000-0000-0000-0000-000000000001', 'NOT_STARTED', now());
