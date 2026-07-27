# ADR-012 Offline-capable reflection capture

- Status: Accepted
- Date: 2026-07-26

## Context
User should be able to draft reflections during transient network loss.

## Decision
Implement local draft persistence in MVP; full outbox sync in later increment.

## Alternatives
- No offline support.

## Consequences
Improved reliability perception with minimal complexity.

## Risks
Draft-only approach is not full offline sync.

## Reconsideration Triggers
Need for robust multi-device offline synchronization.

## Related Requirements
HELIX-QAS-OFF-001
