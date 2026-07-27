# ADR-003 Java and Spring Boot backend

- Status: Accepted
- Date: 2026-07-26

## Context
Need mature ecosystem for modular architecture, validation, migrations, and testing.

## Decision
Use Java 21 and Spring Boot backend with Gradle Kotlin DSL.

## Alternatives
- Node.js backend.
- Python backend service first.

## Consequences
Strong typing, mature testing stack, and straightforward modular discipline.

## Risks
Initial development speed can feel slower than dynamic runtimes.

## Reconsideration Triggers
Hard requirements that are significantly better served by another runtime.

## Related Requirements
HELIX-NFR-003
