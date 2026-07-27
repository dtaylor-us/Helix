# Required Planning Artifacts

## 1. Repository Tree Proposal

```text
helix/
├── apps/
│   ├── web/
│   └── api/
├── packages/
│   ├── contracts/
│   ├── design-system/
│   └── test-support/
├── docs/
├── infra/
├── scripts/
└── .github/
```

## 2. Technology Decision Summary

- Web: React + TypeScript + Vite PWA + TanStack Router + React Query.
- Backend: Java 21 + Spring Boot + Gradle Kotlin DSL.
- Data: PostgreSQL with Flyway migrations.
- AI: optional adapter model with deterministic fallback.

## 3. Corrected Architecture Summary

- No browser-to-database access.
- Modular monolith, no microservices/Kubernetes for MVP.
- AI is adapter-based and optional.
- Cost-aligned low-complexity deployment baseline.

## 4. Domain Module Catalog

See docs/architecture/module-decomposition.md.

## 5. Initial Conceptual Data Model

Core concepts:
- UserProfile
- Transformation
- Experiment
- Reflection
- Suggestion
- Evidence
- Belief
- WisdomEntry
- MemoryProposal and Memory
- KnowledgeNode and KnowledgeEdge
- AIInvocation and PromptDefinition

## 6. First Vertical-Slice Sequence

See docs/architecture/runtime-view.md.

## 7. Threat-Model Summary

See docs/security/threat-model.md.

## 8. Initial Requirement Catalog

See docs/requirements/requirements-catalog.md.

## 9. Requirement-to-Roadmap Traceability

See docs/requirements/traceability-matrix.md.

## 10. Implementation Roadmap

See docs/roadmap/implementation-roadmap.md.

## 11. Risk Register

See docs/roadmap/risks-and-assumptions.md.

## 12. Open Question Log

See docs/requirements/open-questions.md.

## 13. Proposed First Ten Backlog Issues

See docs/roadmap/backlog.md.
