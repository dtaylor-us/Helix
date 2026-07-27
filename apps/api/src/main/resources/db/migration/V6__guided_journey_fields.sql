-- Phase 2: guided transformation and experiment creation.
-- Adds optional fields that support a guided setup flow without changing
-- existing required columns or breaking previously created records.

alter table transformations
    add column desired_identity text,
    add column obstacle text;

alter table experiments
    add column cadence varchar(200),
    add column evidence_of_success text,
    add column review_at date;
