# ADR-011 Deployment without Kubernetes

- Status: Accepted
- Date: 2026-07-26

## Context
Single-user product does not justify Kubernetes operational overhead.

## Decision
Deploy MVP without Kubernetes.

## Alternatives
- Kubernetes from inception.

## Consequences
Lower cost and lower ops complexity.

## Risks
Later migration effort if scale profile changes significantly.

## Reconsideration Triggers
Multi-team operations and sustained high workload growth.

## Related Requirements
HELIX-NFR-003
