# ADR-006 AI as optional provider adapter

- Status: Accepted
- Date: 2026-07-26

## Context
Core workflows must work without AI and remain reliable during provider failure.

## Decision
Model AI behind application ports with optional adapters and deterministic fallback.

## Alternatives
- AI-centric architecture where core workflows require model availability.

## Consequences
Improved resilience and user control.

## Risks
Dual-path behavior needs explicit test coverage.

## Reconsideration Triggers
None unless core business model changes away from personal data ownership.

## Related Requirements
HELIX-AI-001, HELIX-AI-002, HELIX-BR-001
