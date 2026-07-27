# ADR-013 Authentication strategy deferred behind port

- Status: Proposed
- Date: 2026-07-26

## Context
Production deployment environment and provider constraints are not finalized.

## Decision
Keep authentication replaceable behind application boundary; defer provider lock-in.

## Alternatives
- Immediate hard lock to one auth vendor.

## Consequences
Flexibility for deployment decisions.

## Risks
Temporary development-mode auth may differ from production behavior.

## Reconsideration Triggers
Production hosting and identity policy finalized.

## Related Requirements
HELIX-SEC-002
