-- Phase 3: progressive reflection capture.
-- Adds optional structured follow-up fields alongside the existing required
-- `content` field ("what happened"), so existing reflections remain valid
-- and the deterministic suggestion flow (which only reads `content` and
-- experiment.next_action) is unaffected.

alter table reflections
    add column attempted boolean,
    add column noticed text,
    add column evidence_noted text,
    add column surprise text;
