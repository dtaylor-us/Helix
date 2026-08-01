-- Phase 5 slice B: AI-authored weekly retrospective narrative (ADR-016).
-- Same provenance pattern as V8 for suggestions: records whether a saved retrospective snapshot's
-- summary/assistance text came from a live AI provider call or a deterministic template/fallback,
-- and which provider and model produced it. Existing rows predate this feature and are backfilled
-- as DETERMINISTIC with no provider/model recorded.

alter table weekly_retrospectives
    add column source varchar(20) not null default 'DETERMINISTIC',
    add column ai_provider varchar(50),
    add column ai_model varchar(100);
