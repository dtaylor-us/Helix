# ADR-014 Sensitive-content logging prohibition

- Status: Accepted
- Date: 2026-07-26

## Context
Reflections and personal notes are highly sensitive.

## Decision
Prohibit logging reflective text, prompt payloads, and secrets by default.

## Alternatives
- Verbose content logging for diagnostics.

## Consequences
Improved privacy protection and lower breach impact.

## Risks
Debugging can require secure local reproduction steps.

## Reconsideration Triggers
None.

## Related Requirements
HELIX-SEC-001
