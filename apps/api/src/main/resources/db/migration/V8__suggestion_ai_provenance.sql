-- Phase 5 slice A: AI-generated "Suggested Small Action" (ADR-016).
-- Records whether a suggestion's text came from a live AI provider call or a deterministic
-- template / fallback response, and which provider and model produced it. Existing rows predate
-- this feature and are backfilled as DETERMINISTIC with no provider/model recorded.

alter table suggestions
    add column source varchar(20) not null default 'DETERMINISTIC',
    add column ai_provider varchar(50),
    add column ai_model varchar(100);
