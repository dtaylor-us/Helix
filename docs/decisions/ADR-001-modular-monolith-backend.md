# ADR-001 Modular monolith backend

- Status: Accepted
- Date: 2026-07-26

## Context
Helix is a single-user product that requires strong domain boundaries without early distributed complexity.

## Decision
Use a modular monolith backend with explicit module boundaries.

## Alternatives
- Microservices per domain.
- Layered monolith without module boundaries.

## Consequences
Lower operational cost and complexity, with clear internal architecture constraints.

## Risks
Module boundaries can erode without architecture tests.

## Reconsideration Triggers
Multiple independent teams and divergent scaling requirements.

## Related Requirements
HELIX-NFR-003
