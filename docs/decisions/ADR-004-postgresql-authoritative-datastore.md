# ADR-004 PostgreSQL as authoritative datastore

- Status: Accepted
- Date: 2026-07-26

## Context
Helix requires durable, queryable, long-lived personal records.

## Decision
Use PostgreSQL as authoritative datastore for domain records, provenance, and search foundations.

## Alternatives
- Document store only.
- Separate graph database at MVP.

## Consequences
Strong consistency and flexible relational modeling.

## Risks
Schema evolution requires migration discipline.

## Reconsideration Triggers
Requirements that cannot be met efficiently with relational-plus-extension approach.

## Related Requirements
HELIX-NFR-002, HELIX-FR-006
