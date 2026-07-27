# Release Readiness

## Inputs
- Candidate change set
- Target environment constraints

## Procedure
1. Run tests/lint/type checks/build.
2. Run architecture and migration checks.
3. Verify rollback and recovery instructions.
4. Verify security/dependency/doc checks.

## Output Contract
- Pass/fail matrix by check
- Blockers and risk level
- Go/no-go recommendation
