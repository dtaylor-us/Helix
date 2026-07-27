# ADR-010 REST API for client communication

- Status: Accepted
- Date: 2026-07-26

## Context
Need explicit versioned boundary between web client and backend.

## Decision
Use REST API with /api/v1 versioned routes.

## Alternatives
- GraphQL-first API.

## Consequences
Simple contracts and easy testability.

## Risks
Potential endpoint growth without careful contract governance.

## Reconsideration Triggers
Compelling client orchestration needs that REST cannot satisfy efficiently.

## Related Requirements
HELIX-FR-001, HELIX-FR-007
