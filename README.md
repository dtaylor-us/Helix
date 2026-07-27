# Helix

Helix is a private, AI-assisted personal growth system for one user. It helps turn vision into small experiments, reflections, evidence, and practical wisdom.

## Architecture Baseline

- React + TypeScript + Vite PWA in apps/web
- Java 21 + Spring Boot modular monolith in apps/api
- PostgreSQL via Docker Compose for local persistence
- AI is optional via provider adapters; non-AI workflow stays functional

## Quick Start

1. ./scripts/bootstrap
2. docker compose -f infra/local/docker-compose.yml up -d
3. ./scripts/dev-api
4. ./scripts/dev-web
5. Open http://localhost:5173/today

For a complete local run guide, see docs/running-app.md.

## Verification

- ./scripts/test
- ./scripts/lint
- ./scripts/check-docs

## Safety Boundary

Helix is not a therapy or medical system. It must not diagnose, prescribe, or claim clinical authority.
