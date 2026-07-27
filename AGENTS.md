# Helix Agent Operating Guide

## Product Purpose

Helix is a private personal-growth system that helps one user turn vision into experiments, reflections, evidence, and wisdom.

## Non-Clinical Boundary

Helix is not therapy, diagnosis, crisis intervention, or medical advice.

## Architecture Style

- Modular monolith backend
- Explicit domain boundaries
- REST API between web and backend
- Browser never accesses database directly
- AI behind application port and optional adapters

## Module Rules

- Domain layer has no framework dependency
- Application layer orchestrates use cases and ports
- Adapters implement ports and call application services
- Cross-module access occurs through public module APIs/events

## Privacy Rules

- No personal journal/reflection content in logs by default
- Explicit user control for export, deletion, AI provider use
- AI-derived records preserve provenance and confidence

## AI and Memory Rules

- AI output cannot silently become persistent fact
- Memory must be proposed, reviewed, and accepted
- AI unavailability must not break core workflow

## Definition of Done

1. Identify affected requirement IDs and ADRs.
2. Implement the smallest coherent change.
3. Add/update tests.
4. Update docs and traceability.
5. Run verification commands.
6. Report remaining risks or incomplete work.
