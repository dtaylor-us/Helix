# ADR-009 Relational knowledge graph initially

- Status: Accepted
- Date: 2026-07-26

## Context
Knowledge graph needs provenance and governance but MVP must stay simple.

## Decision
Represent graph using relational typed nodes/edges in PostgreSQL initially.

## Alternatives
- Dedicated graph database from day one.

## Consequences
Lower complexity and consistent backup/restore behavior.

## Risks
Future graph queries may require optimization.

## Reconsideration Triggers
Sustained graph-query patterns that exceed relational approach practicality.

## Related Requirements
HELIX-FR-006
