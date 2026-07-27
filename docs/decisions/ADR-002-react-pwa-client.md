# ADR-002 React PWA client

- Status: Accepted
- Date: 2026-07-26

## Context
Helix needs responsive cross-device access and offline-friendly shell behavior.

## Decision
Use React + TypeScript + Vite with PWA support.

## Alternatives
- Server-rendered only UI.
- Native mobile-first implementation.

## Consequences
Fast iterative development and installable web experience.

## Risks
Offline synchronization complexity is deferred.

## Reconsideration Triggers
Strict native capability requirements beyond PWA constraints.

## Related Requirements
HELIX-UX-001, HELIX-UX-002
