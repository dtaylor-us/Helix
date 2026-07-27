# ADR-005 No direct browser database access

- Status: Accepted
- Date: 2026-07-26

## Context
Direct client database access breaks security and modular boundaries.

## Decision
All persistence access must pass through backend application APIs.

## Alternatives
- Browser-to-database direct connections.

## Consequences
Centralized authorization, validation, and audit/provenance handling.

## Risks
Additional API implementation work.

## Reconsideration Triggers
None for this product scope.

## Related Requirements
HELIX-NFR-001, HELIX-SEC-001
