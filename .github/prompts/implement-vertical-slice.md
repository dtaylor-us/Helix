# Implement Vertical Slice

## Required Inputs
- Requirement IDs
- Domain modules impacted
- Acceptance criteria
- UX notes and constraints
- ADR references

## Procedure
1. Restate target outcome and assumptions.
2. Map backend, frontend, data, and contract impacts.
3. Implement smallest coherent change.
4. Add migration/tests/docs updates.
5. Run verification commands.

## Required Output
- Changed files grouped by concern.
- Verification results.
- Residual risks/deferred items.
- Traceability matrix updates.

## Quality Gates
- Core flow works without AI dependency.
- Privacy/logging constraints respected.
- Boundary tests pass when architecture impacted.
