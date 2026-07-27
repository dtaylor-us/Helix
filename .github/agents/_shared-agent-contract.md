# Shared Agent Contract

All role agents in this repository must follow this protocol.

## Required Inputs
- Objective and user outcome.
- Relevant requirement IDs.
- Relevant ADR IDs.
- Scope boundaries and constraints.

## Required Output Format
1. Summary of changes.
2. Assumptions made.
3. Verification commands run.
4. Test/documentation impact.
5. Residual risks and deferred work.

## Mandatory Guardrails
- No direct browser-to-database data access.
- No speculative infrastructure (including Kubernetes) for MVP scope.
- No mandatory AI dependency for core workflows.
- No sensitive reflective content in logs or telemetry.
- No clinical framing or diagnostic claims.

## Quality Gates
- Code compiles.
- Relevant tests pass.
- Documentation and traceability updated when needed.
- Architecture boundaries preserved.
