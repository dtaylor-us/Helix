# ADR-006 AI as optional provider adapter

- Status: Accepted (narrowed by ADR-016 for specific generative-content features, 2026-08-01)
- Date: 2026-07-26

> **2026-08-01 amendment note:** ADR-016 narrows this ADR's "core workflows must work without AI"
> mandate for post-reflection suggestion generation (and, in later slices, weekly retrospective
> narrative and experiment drafting). The port/adapter abstraction, provider selection (ADR-007),
> and user-governance model (ADR-008) described below are unchanged and still apply. See ADR-016
> for the full rationale.

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
