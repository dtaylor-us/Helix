# .github Governance

This directory defines repository automation and agentic engineering behavior.

## Structure
- `copilot-instructions.md`: universal coding guardrails.
- `agents/`: role-specific operating charters.
- `prompts/`: reusable workflow prompts.
- `workflows/`: CI and policy automation.

## Agentic Best-Practice Principles
1. Requirement and ADR traceability before implementation.
2. Smallest coherent change with explicit assumptions.
3. Deterministic fallback for AI-assisted behavior.
4. Privacy and safety checks as default, not optional.
5. Verification evidence attached to each change.

## Minimum Evidence for Changes
- Affected requirement IDs.
- Affected ADR IDs.
- Test and lint results.
- Documentation updates for behavior/architecture changes.
- Residual risk statement.
