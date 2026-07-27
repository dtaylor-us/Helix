# Contributing

## Definition of Done

- Acceptance criteria met
- Module and layer boundaries preserved
- Tests added or updated
- Documentation and traceability updated
- No sensitive content logging introduced
- CI checks pass

## Local Workflow

1. ./scripts/bootstrap
2. ./scripts/dev
3. Implement the smallest coherent change
4. ./scripts/test
5. ./scripts/lint
6. ./scripts/check-docs

## Guardrails

- No browser-to-database access
- No mandatory AI dependency for core workflows
- No speculative infrastructure (no Kubernetes for MVP)
- No clinical claims in product copy or behavior
