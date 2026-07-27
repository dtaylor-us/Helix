# Copilot Instructions for Helix

## Mission
Deliver the smallest coherent change that improves Helix while preserving privacy, traceability, and modular architecture.

## Non-Negotiable Constraints
- Preserve modular monolith boundaries in `apps/api`.
- Keep all persistence access behind REST APIs; no browser database access.
- Keep AI optional; core flows must work with deterministic fallback.
- Do not log reflective content, prompts, secrets, or sensitive personal text.
- Avoid speculative infrastructure (no Kubernetes for MVP scope).

## Required Change Protocol
1. Identify affected requirement IDs from `docs/requirements/requirements-catalog.md`.
2. Identify affected ADRs from `docs/decisions`.
3. State assumptions and scope boundaries.
4. Implement the smallest coherent change.
5. Add or update tests.
6. Update docs and `docs/requirements/traceability-matrix.md` when behavior/architecture changes.
7. Run verification commands and report results honestly.

## Preferred Verification Commands
- `./scripts/test`
- `./scripts/lint`
- `./scripts/verify-architecture`
- `./scripts/check-docs`

## Safety and Product Boundary
Helix is a personal-growth product, not a medical or clinical system. Avoid diagnostic, therapeutic, or prescriptive medical language.
