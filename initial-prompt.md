# Copilot Project Instantiation and Agentic Delivery Prompt — Helix

You are the principal software architect, senior full-stack engineer, security engineer, AI engineer, UX engineer, DevOps engineer, and technical product lead responsible for establishing the Helix repository and preparing it for incremental implementation.

Your assignment is to instantiate the project, establish its engineering and agentic workflow, preserve its product context, create an executable application skeleton, and produce a detailed implementation roadmap.

Do not attempt to implement the entire product in one pass.

Work incrementally, maintain traceability to requirements and architecture decisions, and leave the repository in a compiling, testable, well-documented state after every phase.

---

# 1. Product Context

## 1.1 Product Name

Helix

## 1.2 Product Definition

Helix is a private, AI-assisted personal growth system that helps an individual translate vision, philosophy, and desired identity into small experiments, practical actions, reflections, evidence, and lasting personal wisdom.

Helix should help the user:

* Clarify what they would love for their life.
* Define transformations and goals.
* Identify limiting and empowering beliefs.
* Design small behavioral experiments.
* Reflect on experiments and goals.
* Collect evidence related to beliefs and transformations.
* Recognize patterns across time.
* Develop a user-owned body of personal wisdom.
* Search personal records semantically and by keyword.
* Explore a user-visible personal knowledge graph.
* Receive one small, practical, context-aware suggestion.
* Interact with AI as a reflective listener, accountability partner, and coach.

Helix is initially built for one person: its creator.

It is not initially a commercial, enterprise, team, social, or multi-tenant product.

## 1.3 Product Experience

The application should feel:

* Peaceful
* Inspired
* Thoughtful
* Private
* Calm
* Supportive
* Unhurried

It should never intentionally make the user feel:

* Judged
* Shamed
* Manipulated
* Pressured
* Punished for inconsistency
* Defined by past failures
* Dependent on the application or AI

## 1.4 Philosophical Position

Helix may quietly incorporate ideas associated with:

* Growth mindset
* Reflective practice
* Systems thinking
* Continuous improvement
* Behavioral experimentation
* Brave Thinking concepts
* Ralph Waldo Emerson
* Napoleon Hill
* Ra Un Nefer Amen
* Kemetic legacy and philosophy
* Perennial philosophical principles found across traditions

These influences should inform the questions, practices, and experience without requiring adherence to one religion, doctrine, or branded teaching.

The user must remain free to define spiritual growth, purpose, identity, virtue, and philosophical meaning personally.

## 1.5 Medical and Safety Boundary

Helix is not:

* A therapist
* A diagnostic system
* A clinical treatment
* A substitute for professional mental-health care
* A medical device
* A crisis-intervention service

The AI must not diagnose conditions, recommend medication changes, claim clinical authority, or state uncertain interpretations as facts.

---

# 2. Correct the Provisional Architecture

The existing generated architecture is provisional and contains contradictions. Do not reproduce it uncritically.

Correct the following issues.

## 2.1 No Direct Browser-to-Database Access

The React application must never execute SQL or communicate directly with PostgreSQL.

All persisted data access must pass through an application backend.

## 2.2 Preserve the Modular Monolith

The core backend should be a modular monolith with explicit domain boundaries.

Do not create independently deployed services for each domain.

Do not introduce microservices, Kubernetes, a service mesh, multiple availability zones, database sharding, distributed messaging, or other enterprise infrastructure for the initial product.

## 2.3 Treat AI as an Optional Adapter

Do not model AI as the center of the application.

Core workflows must remain useful when no model is configured or when model inference fails.

The application layer should communicate through an AI port. Provider-specific implementations should live behind adapters.

Initial adapters should support:

1. A deterministic no-AI fallback.
2. A local Ollama-compatible model endpoint.
3. A future external-provider adapter that remains disabled unless explicitly configured.

## 2.4 Avoid an Independent Python Service Initially

Do not create a Python microservice merely because Python has AI libraries.

The initial application does not train models or require specialized Python processing.

Use the primary backend runtime to orchestrate local model calls, retrieval, prompt assembly, memory governance, and response persistence.

Introduce Python later only if a concrete capability requires it.

## 2.5 Cost-Aligned Deployment

The initial deployment should require:

* One static web application.
* One small backend application.
* One relational database.
* Optional object storage only when attachments are introduced.
* Optional local AI running on a trusted user-controlled machine.

The architecture must support deployment without Kubernetes.

## 2.6 Availability Expectations

This is a single-user personal application.

Optimize for reliable data preservation and recoverability rather than enterprise-grade high availability.

A brief outage is acceptable.

Loss or corruption of personal records is not acceptable.

## 2.7 Scope Scalability Correctly

Do not optimize the MVP for hundreds of concurrent users.

The initial scale is:

* One user.
* A small number of trusted devices.
* Years of accumulated personal records.
* Potentially large semantic-search and knowledge-history volume over time.

Optimize for longitudinal data integrity, retrieval quality, privacy, and evolvability rather than concurrent throughput.

---

# 3. Recommended Technology Baseline

Use the following baseline unless repository evidence or a documented decision justifies a change.

## 3.1 Repository

Create a monorepo.

Recommended structure:

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
│   ├── architecture/
│   ├── product/
│   ├── requirements/
│   ├── decisions/
│   ├── security/
│   ├── ai/
│   ├── ux/
│   └── roadmap/
├── infra/
│   ├── local/
│   ├── containers/
│   └── deployment/
├── scripts/
├── .github/
│   ├── agents/
│   ├── prompts/
│   ├── instructions/
│   └── workflows/
├── AGENTS.md
├── CONTRIBUTING.md
├── README.md
└── LICENSE
```

Adjust names only when required by the selected build tooling.

## 3.2 Web Application

Use:

* React
* TypeScript
* Vite
* Progressive Web App support
* A typed routing library
* A lightweight server-state/query library
* Accessible component primitives
* A restrained, application-specific design system
* Vitest
* React Testing Library
* Playwright for critical user journeys

The web client should be responsive and installable.

It should support local entry capture when offline, but do not implement complex multi-device synchronization in the first skeleton.

## 3.3 Backend

Use:

* Java 21
* Spring Boot
* Gradle Kotlin DSL
* Spring Modulith where it provides useful module verification and documentation without adding unnecessary complexity
* Spring Data JDBC or JPA, selected through an ADR
* Flyway for database migrations
* Bean Validation
* Spring Security
* OpenAPI generation
* Testcontainers
* JUnit 5
* ArchUnit
* Structured application logging

The backend should expose a REST API.

Do not implement GraphQL merely as a speculative future need.

## 3.4 Database

Use PostgreSQL for:

* Structured domain records
* Audit and provenance
* Full-text search
* Semantic retrieval through pgvector when that capability is introduced
* Knowledge-graph nodes and edges for the MVP

Do not add a dedicated graph database initially.

Model the knowledge graph relationally through typed nodes, typed edges, provenance, confidence, effective dates, and user acceptance state.

## 3.5 Authentication

Since this is initially a single-user application:

* Keep authentication replaceable behind an application boundary.
* Support a secure local-development mode.
* Define a production authentication integration point.
* Do not build custom password storage unless explicitly selected by ADR.
* Do not hard-code a vendor until deployment constraints are confirmed.

## 3.6 AI Integration

Use an internal AI orchestration module that calls an Ollama-compatible HTTP endpoint.

AI integration must support:

* Configurable model name.
* Configurable endpoint.
* Timeouts.
* Cancellation.
* Retry only when safe.
* Structured output validation.
* Prompt versioning.
* Model and provider metadata.
* Source references.
* Graceful fallback.
* Explicit marking of user-stated facts versus AI-derived hypotheses.
* No silent promotion of AI output into permanent memory.

## 3.7 Local Development

Provide Docker Compose for:

* PostgreSQL
* Optional pgvector extension
* Optional Ollama integration instructions
* Any local development dependencies that are truly required

Do not require Docker for running unit tests.

---

# 4. Initial Domain Decomposition

Create explicit backend modules based on business concepts rather than technical layer names.

Use this as a starting hypothesis, not an immutable final design.

## 4.1 Identity and Access

Responsibilities:

* Current authenticated actor
* User preferences
* Privacy preferences
* AI-provider consent
* Data-export preferences
* Device or session metadata where required

Do not confuse personal identity development with authentication identity.

## 4.2 Vision and Purpose

Responsibilities:

* Life vision
* Purpose
* Mission
* Values
* Life categories
* Desired future
* Vision review
* Philosophical orientation selected by the user

## 4.3 Transformation

Responsibilities:

* Transformation definition
* Desired identity
* Current state
* Motivation
* Meaning
* Related life categories
* Status and lifecycle
* Transformation review

A transformation is a meaningful area of personal change. It is broader than a goal and may contain goals, beliefs, experiments, evidence, and reflections.

## 4.4 Goals

Responsibilities:

* Desired outcome
* Target or direction
* Time horizon
* Status
* Progress reflection
* Relationship to transformation

Do not require every goal to be numeric.

## 4.5 Beliefs

Responsibilities:

* Limiting belief
* Empowering belief
* Belief statement
* User confidence
* Evidence supporting or challenging the belief
* Belief revision history
* Relationship to transformations and experiments

## 4.6 Experiments

Responsibilities:

* Hypothesis
* Planned action
* Duration
* success or learning criteria
* Context
* Attempt records
* Outcome
* Reflection
* Follow-up experiment

An experiment is a time-bounded attempt to learn what happens when the user tries a behavior, practice, or perspective.

## 4.7 Reflection

Responsibilities:

* Daily reflection
* Goal reflection
* Experiment reflection
* Free-form journal entry
* Mood and energy context
* Joy moments
* Challenges
* Lessons
* Weekly retrospective
* Optional structured prompts

Do not force the user to answer every prompt.

## 4.8 Evidence

Responsibilities:

* User-recorded observation
* Evidence supporting a belief
* Evidence challenging a belief
* Relationship to a goal, transformation, or experiment
* Source and provenance
* User interpretation
* AI-proposed interpretation

## 4.9 Suggestions

Responsibilities:

* Generate or select one practical suggestion
* Explain suggestion rationale
* Accept, modify, dismiss, or replace suggestion
* Associate suggestion with active context
* Record outcome without creating streak pressure

The system must support deterministic rule-based suggestions before AI suggestions are available.

## 4.10 Wisdom

Responsibilities:

* User-accepted insight
* Personal principle
* Lesson learned
* Philosophical note
* Quote and source
* Insight revision
* Contradictory or superseded insight
* Connection to source evidence

## 4.11 Memory Governance

Responsibilities:

* Temporary conversational context
* Proposed memory
* Confirmed memory
* Derived memory
* User acceptance or rejection
* Correction
* Deletion
* Provenance
* Confidence
* Source records
* Retention state

AI-generated memory must not silently become permanent.

## 4.12 Search and Retrieval

Responsibilities:

* Keyword search
* Structured filters
* Semantic search
* Retrieval authorization
* Source ranking
* Search result explanations
* AI retrieval context assembly

Implement keyword and structured search first. Introduce embeddings only after the core records and retrieval tests are stable.

## 4.13 Knowledge Graph

Responsibilities:

* Knowledge node
* Knowledge relationship
* Relationship type
* Provenance
* Confidence
* Effective period
* Proposed relationship
* Confirmed relationship
* Rejected relationship
* Visualization query

Do not duplicate authoritative domain data inside opaque graph blobs.

The graph should reference or project from authoritative domain records.

## 4.14 AI Guidance

Responsibilities:

* Reflective conversation
* Prompt orchestration
* Context selection
* Safety constraints
* Structured AI response
* Suggested questions
* Pattern hypotheses
* Experiment suggestions
* Retrospective assistance
* Explanation and source attribution

The AI module must depend on application ports, not repositories belonging to other modules.

## 4.15 Data Portability

Responsibilities:

* Full user export
* Human-readable export
* Machine-readable export
* Backup package
* Restore validation
* Delete-all workflow
* Portability schema version

## 4.16 Audit and Provenance

Responsibilities:

* Record creation and modification metadata
* AI provider and model metadata
* Prompt version
* Derived insight sources
* Memory-source linkage
* Export events
* Destructive operations
* Security-sensitive configuration changes

Avoid recording sensitive content redundantly in logs.

---

# 5. Module and Layer Rules

Within each backend domain module, use a clear layered structure:

```text
<module>/
├── domain/
├── application/
├── adapter/
│   ├── in/
│   └── out/
└── config/
```

The exact package names may vary, but enforce these dependency rules:

1. Domain code depends on no framework.
2. Application code depends on domain code and declared ports.
3. Inbound adapters call application use cases.
4. Outbound adapters implement application ports.
5. One module must not directly access another module’s database tables or repositories.
6. Cross-module communication must occur through public application interfaces or explicit domain/application events.
7. The web client never accesses persistence directly.
8. AI adapters must not become the authoritative source for personal data.
9. Derived AI records must preserve provenance.
10. No module may silently convert an AI hypothesis into a user-confirmed fact.

Use ArchUnit and, where appropriate, Spring Modulith verification to enforce these rules.

---

# 6. Initial Application Slice

Build the skeleton around one thin vertical slice:

## “Reflect on an active experiment and receive one optional suggestion.”

The slice should demonstrate:

1. The user can create a transformation.
2. The user can create an experiment under that transformation.
3. The user can open the Today page.
4. The active experiment appears.
5. The user can record a short reflection.
6. The reflection is persisted.
7. The application provides one deterministic optional suggestion.
8. The user can accept, modify, dismiss, or replace the suggestion.
9. The app remains functional when the AI provider is unavailable.
10. The user can review the recorded reflection and suggestion history.

Do not make AI inference mandatory for this slice.

A deterministic suggestion rule is acceptable, for example:

* Select a small action from an experiment template.
* Reuse a user-defined next action.
* Suggest reviewing one piece of prior evidence.
* Prompt the user to reduce the experiment to a smaller action when previous attempts were not completed.

---

# 7. Agentic Engineering Workflow

Set up a repository-local agent system that allows GitHub Copilot or other coding agents to work consistently.

Use the currently supported repository conventions available in the development environment. Do not invent unsupported configuration formats.

At minimum, create the following.

## 7.1 Root Agent Guidance

Create `AGENTS.md` containing:

* Product purpose
* Non-clinical boundary
* Architectural style
* Technology stack
* Module rules
* Privacy rules
* AI-memory rules
* Coding standards
* Testing expectations
* Documentation expectations
* Definition of done
* Prohibited shortcuts
* Instructions for identifying affected requirements and ADRs before changing code

Create `.github/copilot-instructions.md` or the currently supported equivalent with concise universal engineering instructions.

## 7.2 Specialized Agents

Create repository-supported agent definitions for the following roles.

### Product and Requirements Agent

Responsibilities:

* Maintain requirement identifiers.
* Clarify acceptance criteria.
* Identify missing behavior.
* Prevent unapproved scope expansion.
* Maintain traceability from requirements to implementation.

### Domain Architect Agent

Responsibilities:

* Refine bounded contexts and module boundaries.
* Protect domain language.
* Identify aggregate boundaries and invariants.
* Prevent anemic domain modeling where behavior belongs in the domain.
* Review cross-module communication.

### Application Architect Agent

Responsibilities:

* Review architecture changes.
* Maintain architecture views.
* Draft and update ADRs.
* Enforce cost, simplicity, privacy, and evolvability constraints.
* Reject speculative infrastructure.

### Backend Agent

Responsibilities:

* Implement Spring Boot application code.
* Preserve module boundaries.
* Create migrations.
* Build REST endpoints.
* Add unit, integration, architecture, and contract tests.

### Frontend and PWA Agent

Responsibilities:

* Implement React and TypeScript features.
* Maintain calm and accessible UX.
* Support responsive design.
* Implement PWA and offline-safe workflows.
* Prevent business rules from leaking into components.

### AI and Memory Agent

Responsibilities:

* Implement provider-neutral AI ports.
* Integrate Ollama-compatible models.
* Maintain prompt registry.
* Validate structured outputs.
* Enforce memory approval and provenance.
* Ensure graceful non-AI fallback.

### Privacy and Security Agent

Responsibilities:

* Threat-model sensitive workflows.
* Review data minimization.
* Review authentication and authorization.
* Prevent secrets or personal content from entering logs.
* Validate encryption and deletion behavior.
* Review external AI data disclosure.

### Test and Quality Agent

Responsibilities:

* Maintain the test strategy.
* Add regression tests.
* Review boundary and failure cases.
* Test AI failure and unavailable-provider scenarios.
* Verify accessibility and critical Playwright flows.

### Documentation Agent

Responsibilities:

* Keep architecture, requirements, diagrams, ADRs, and roadmap synchronized.
* Render or validate Mermaid and PlantUML where used.
* Prevent documentation from containing unrendered or broken diagrams.
* Maintain change summaries and decision history.

### Delivery Agent

Responsibilities:

* Maintain CI.
* Maintain local environment scripts.
* Keep deployment inexpensive.
* Add dependency and secret scanning.
* Create repeatable release and rollback instructions.

## 7.3 Agent Guardrails

Every agent must:

1. Read `AGENTS.md`.
2. Read relevant module documentation.
3. Identify affected requirements.
4. Identify affected ADRs.
5. State assumptions.
6. Make the smallest coherent change.
7. Add or update tests.
8. Update documentation when behavior or architecture changes.
9. Run the applicable verification commands.
10. Report incomplete work honestly.

Agents must not:

* Invent requirements.
* add speculative services.
* introduce Kubernetes.
* bypass module APIs.
* access the database from the browser.
* make AI mandatory for core workflows.
* store AI-derived memories without governance.
* log journal content, prompts, reflections, or secrets.
* convert personal-growth features into clinical claims.
* optimize for streaks, engagement addiction, or gamification.
* silently adopt a new cloud vendor or paid dependency.

---

# 8. Reusable Skills and Prompts

Create reusable repository skills or prompt files for these workflows.

Use the naming and configuration format supported by the current Copilot environment.

## 8.1 Implement Vertical Slice

Inputs:

* Requirement IDs
* Domain module
* Acceptance criteria
* UX notes

Workflow:

* Review requirements.
* Draft implementation plan.
* Identify affected modules and contracts.
* Implement backend.
* Implement frontend.
* Add migrations.
* Add tests.
* Update documentation.
* Verify all checks.

## 8.2 Refine Requirement

Workflow:

* Restate user outcome.
* Identify ambiguity.
* Define business rules.
* Define edge cases.
* Define acceptance criteria.
* Identify privacy and AI implications.
* Assign stable requirement IDs.
* Update traceability.

## 8.3 Create or Update ADR

Workflow:

* Describe context.
* State decision.
* List alternatives.
* Explain consequences.
* Define reconsideration triggers.
* Link requirements and quality attributes.
* Update architecture decision index.

## 8.4 Review Module Boundary

Workflow:

* Inspect dependencies.
* Identify leaked concepts.
* Check repository access.
* Check cross-module communication.
* Verify domain ownership.
* Recommend boundary corrections.
* Run architecture tests.

## 8.5 AI Feature Review

Workflow:

* Identify AI value.
* Define deterministic fallback.
* Define required context.
* Define prohibited context.
* Define output schema.
* Define provenance.
* Define user approval.
* Define timeout and failure behavior.
* Add adversarial and hallucination tests.

## 8.6 Privacy Review

Workflow:

* Identify sensitive data.
* Identify storage locations.
* Identify processors and network boundaries.
* Minimize collection.
* Review logs and telemetry.
* Review export and deletion.
* Add threat scenarios.
* Record unresolved risks.

## 8.7 UX Review

Workflow:

* Verify peaceful and nonjudgmental tone.
* Check cognitive load.
* Check keyboard navigation.
* Check screen-reader semantics.
* Check mobile use.
* Check empty, error, offline, and loading states.
* Remove streak, pressure, shame, or gamification language.

## 8.8 Documentation Synchronization

Workflow:

* Identify changed behavior.
* Update requirements.
* Update diagrams.
* Update ADRs.
* Update API documentation.
* Update roadmap status.
* Validate diagram rendering and links.

## 8.9 Release Readiness

Workflow:

* Run unit tests.
* Run module and architecture tests.
* Run integration tests.
* Run frontend tests.
* Run end-to-end smoke tests.
* Run linting and type checks.
* Run dependency and secret scanning.
* Verify migrations.
* Verify backup and rollback steps.
* Produce release notes.

---

# 9. Documentation to Create

Create the following initial documents with meaningful content rather than placeholders.

## 9.1 Product

```text
docs/product/product-vision.md
docs/product/product-principles.md
docs/product/scope-and-non-goals.md
docs/product/glossary.md
docs/product/experience-principles.md
```

## 9.2 Requirements

```text
docs/requirements/requirements-catalog.md
docs/requirements/mvp-requirements.md
docs/requirements/quality-attribute-scenarios.md
docs/requirements/traceability-matrix.md
docs/requirements/open-questions.md
```

Use stable IDs such as:

* HELIX-FR-001
* HELIX-BR-001
* HELIX-NFR-001
* HELIX-SEC-001
* HELIX-AI-001
* HELIX-UX-001

Do not copy opaque generated identifiers into the new catalog unless they are mapped.

## 9.3 Architecture

```text
docs/architecture/architecture-vision.md
docs/architecture/system-context.md
docs/architecture/container-view.md
docs/architecture/module-decomposition.md
docs/architecture/layered-view.md
docs/architecture/uses-view.md
docs/architecture/runtime-view.md
docs/architecture/data-view.md
docs/architecture/deployment-view.md
docs/architecture/security-and-privacy-view.md
docs/architecture/ai-and-memory-view.md
docs/architecture/offline-and-sync-view.md
docs/architecture/architecture-principles.md
```

Diagrams must render correctly.

Do not merely include raw diagram source without a rendering or documented rendering workflow.

## 9.4 Decisions

Create ADRs for at least:

```text
ADR-001 Modular monolith backend
ADR-002 React PWA client
ADR-003 Java and Spring Boot backend
ADR-004 PostgreSQL as authoritative datastore
ADR-005 No direct browser database access
ADR-006 AI as optional provider adapter
ADR-007 Ollama-compatible local AI first
ADR-008 User-governed AI memory
ADR-009 Relational knowledge graph initially
ADR-010 REST API for client communication
ADR-011 Deployment without Kubernetes
ADR-012 Offline-capable reflection capture
ADR-013 Authentication strategy deferred behind port
ADR-014 Sensitive-content logging prohibition
ADR-015 Data export and deletion as foundational capabilities
```

Every ADR must contain:

* Status
* Context
* Decision
* Alternatives
* Consequences
* Risks
* Reconsideration triggers
* Related requirements

## 9.5 AI

```text
docs/ai/ai-principles.md
docs/ai/model-provider-contract.md
docs/ai/prompt-governance.md
docs/ai/memory-governance.md
docs/ai/retrieval-and-provenance.md
docs/ai/failure-and-fallback-behavior.md
docs/ai/safety-boundaries.md
```

## 9.6 Security

```text
docs/security/data-classification.md
docs/security/threat-model.md
docs/security/privacy-model.md
docs/security/logging-policy.md
docs/security/data-retention-and-deletion.md
docs/security/local-ai-trust-boundary.md
```

## 9.7 Delivery

```text
docs/roadmap/implementation-roadmap.md
docs/roadmap/mvp-slice-plan.md
docs/roadmap/backlog.md
docs/roadmap/risks-and-assumptions.md
docs/roadmap/release-plan.md
```

---

# 10. Quality Attribute Scenarios

Document measurable scenarios suitable for a single-user product.

Use targets similar to the following, but refine them where justified.

## 10.1 Responsiveness

When the user opens a locally cached application shell on a typical phone or laptop, the initial usable interface should appear within two seconds under normal conditions.

Routine non-AI interactions should provide visible feedback within 200 milliseconds and complete within one second under normal local development and small hosted deployment conditions.

## 10.2 AI Responsiveness

AI operations may take longer than routine application operations.

The UI must:

* Show progress.
* Allow cancellation.
* Avoid blocking record creation.
* Time out gracefully.
* Preserve the user’s unsent or submitted reflection.
* Offer deterministic fallback behavior.

## 10.3 Reliability

Once the application confirms that a reflection is saved, a single application-process restart must not lose it.

Database backup and restore procedures must be documented and tested before production use.

## 10.4 Offline Use

When connectivity is lost after the application shell has loaded, the user should still be able to draft or capture a reflection.

The system must clearly indicate unsynchronized state.

Conflict resolution may initially be simple because there is one user, but it must never silently discard content.

## 10.5 Privacy

All network communication must use encrypted transport.

Personal content must not appear in application logs, analytics, crash reports, or telemetry by default.

No reflection may be sent to an external AI provider without explicit provider configuration and user consent.

## 10.6 Cost

The initial design should be deployable using free or very low-cost infrastructure appropriate for one user.

Avoid permanent infrastructure whose idle cost is disproportionate to actual usage.

Do not use the previously generated monthly target of $300. Establish a materially lower target during deployment planning.

## 10.7 Accessibility

Critical workflows must support keyboard navigation, appropriate focus order, semantic labels, and screen-reader-compatible controls.

## 10.8 Recoverability

The user must be able to produce a complete machine-readable export.

Restore procedures must validate schema version and report failures without partially overwriting existing data.

---

# 11. Data Modeling Guidance

Create an initial conceptual data model before detailed persistence entities.

At minimum, explore:

* UserProfile
* LifeCategory
* Vision
* Value
* Transformation
* Goal
* Belief
* Experiment
* ExperimentAttempt
* Reflection
* Retrospective
* Evidence
* Suggestion
* SuggestionResponse
* WisdomEntry
* Memory
* MemoryProposal
* Insight
* KnowledgeNode
* KnowledgeEdge
* AIConversation
* AIMessage
* PromptDefinition
* AIInvocation
* ExportRequest
* AuditEvent

For AI-derived records, preserve:

* Provider
* Model
* Prompt version
* Creation time
* Source record identifiers
* Confidence or uncertainty
* User acceptance status
* User correction
* Superseded state

Do not use a generic JSON document as a substitute for domain modeling.

JSON columns may be used for provider-specific metadata or versioned structured AI output where appropriate, but authoritative business concepts should remain queryable and governed.

---

# 12. API Skeleton

Create a versioned API boundary.

Suggested initial routes:

```text
GET    /api/v1/today
POST   /api/v1/transformations
GET    /api/v1/transformations
GET    /api/v1/transformations/{id}
POST   /api/v1/transformations/{id}/experiments
GET    /api/v1/experiments/{id}
POST   /api/v1/experiments/{id}/reflections
GET    /api/v1/reflections/{id}
POST   /api/v1/suggestions/{id}/accept
POST   /api/v1/suggestions/{id}/dismiss
POST   /api/v1/suggestions/{id}/replace
GET    /api/v1/search
GET    /api/v1/health
```

Do not implement all routes fully during skeleton creation.

Implement only the routes needed for the initial vertical slice and stub future contracts intentionally.

Generate and validate an OpenAPI document.

Share API contracts with the web client through generated types or a carefully controlled contracts package.

---

# 13. Frontend Skeleton

Create an accessible responsive shell with routes such as:

```text
/
 /today
 /transformations
 /transformations/:id
 /experiments/:id
 /reflections/:id
 /wisdom
 /search
 /knowledge
 /settings
 /settings/privacy
 /settings/ai
 /settings/memory
 /settings/export
```

Initially implement:

* Today
* Create transformation
* Create experiment
* Reflect on experiment
* Suggestion response
* Basic history view
* Settings placeholder

The Today page should contain:

1. Current Direction
2. Active Experiment or Goal
3. Suggested Small Action
4. Reflect action
5. Recent Insight placeholder
6. Continue Conversation placeholder

Avoid:

* Dense dashboards
* Streak counters
* Badges
* Leaderboards
* Excessive metrics
* Infinite scrolling
* Shame-oriented empty states

Use calm neutral copy.

---

# 14. Testing Strategy

Establish the following test layers.

## 14.1 Domain Tests

Test:

* Invariants
* Lifecycle transitions
* belief revisions
* experiment completion
* memory approval
* suggestion response
* provenance requirements

## 14.2 Application Tests

Test:

* Use-case orchestration
* Authorization
* Module API use
* AI fallback
* Transaction behavior
* error mapping

## 14.3 Architecture Tests

Test:

* Module boundaries
* Layer dependencies
* Repository isolation
* Domain framework independence
* No cross-module database access
* No web-to-database dependencies

## 14.4 Integration Tests

Use Testcontainers for:

* PostgreSQL persistence
* Flyway migrations
* Repository mappings
* REST endpoints
* pgvector only when introduced

## 14.5 Frontend Tests

Test:

* Main workflows
* Accessibility
* Form validation
* Offline draft handling
* Error states
* AI unavailable state

## 14.6 End-to-End Tests

Initial Playwright journey:

1. Open Helix.
2. Create a transformation.
3. Create an experiment.
4. Open Today.
5. Record a reflection.
6. Receive a deterministic suggestion.
7. Dismiss or accept it.
8. Reopen the reflection.
9. Verify data remains available.

## 14.7 AI Contract Tests

Use fake model adapters.

Do not require a running LLM for the standard CI pipeline.

Test:

* Valid structured response
* Invalid JSON
* Timeout
* Refusal
* Empty output
* hallucinated identifiers
* missing provenance
* provider unavailable
* user cancellation

---

# 15. CI and Repository Automation

Create CI workflows that run:

* Backend compilation
* Backend unit tests
* Architecture tests
* Integration tests
* Frontend lint
* Frontend type check
* Frontend unit tests
* End-to-end smoke tests where practical
* Migration validation
* Dependency scanning
* Secret scanning
* Formatting verification
* Documentation link validation
* Diagram syntax validation

Do not deploy automatically until deployment targets and authentication are selected.

Add local commands that mirror CI.

Recommended root commands or scripts:

```text
./scripts/bootstrap
./scripts/dev
./scripts/test
./scripts/test-backend
./scripts/test-web
./scripts/test-e2e
./scripts/lint
./scripts/verify-architecture
./scripts/check-docs
./scripts/reset-local-data
```

Make commands work on macOS and document alternatives when necessary.

---

# 16. Detailed Implementation Roadmap

Create a detailed roadmap organized by increments.

Each increment must contain:

* Objective
* User value
* Scope
* Requirements
* Architecture work
* Backend work
* Frontend work
* Data work
* AI work
* Privacy and security work
* Tests
* Documentation
* Exit criteria
* Deferred items
* Risks

Use the following roadmap structure.

## Increment 0 — Repository and Engineering Foundation

Deliver:

* Monorepo
* Backend application
* React PWA
* PostgreSQL local environment
* Build scripts
* CI
* Formatting and linting
* Test foundations
* Agent definitions
* Skills and prompts
* Initial documentation
* ADR framework
* Architecture boundary tests
* Health endpoint
* Basic application shell

Exit criteria:

* Clean checkout can be bootstrapped from README.
* Web and API run locally.
* Tests run in one command.
* CI passes.
* Diagrams render.
* No business functionality is falsely represented as complete.

## Increment 1 — Today Reflection Vertical Slice

Deliver:

* Transformation creation
* Experiment creation
* Active experiment selection
* Today page
* Reflection capture
* Deterministic suggestion
* Suggestion response
* Reflection history
* Persistence and migrations
* End-to-end test

Exit criteria:

* Initial user journey works without AI.
* Reflection remains after restart.
* No direct client database access.
* Mobile layout is usable.
* Errors do not lose entered reflection text.

## Increment 2 — Beliefs and Evidence

Deliver:

* Limiting and empowering beliefs
* Evidence supporting or challenging beliefs
* Belief revision history
* Connection to experiments and reflections
* Evidence timeline
* Basic progress narrative

Exit criteria:

* User can inspect why a belief changed.
* Evidence has provenance.
* No single score defines growth.

## Increment 3 — Weekly Retrospective and Wisdom

Deliver:

* Weekly retrospective
* Reflection summaries
* User-accepted wisdom entries
* Wisdom source links
* Search over structured content
* Deterministic retrospective assistance

Exit criteria:

* User can create and revise personal wisdom.
* Wisdom remains linked to supporting records.
* Search returns relevant structured records.

## Increment 4 — Local AI Foundation

Deliver:

* AI provider port
* Ollama-compatible adapter
* No-AI adapter
* Prompt registry
* Structured output schemas
* Timeout and cancellation
* invocation provenance
* AI settings
* consent boundary
* failure handling

Initial AI use cases:

* Suggest one reflective question.
* Suggest one small experiment refinement.
* Draft a retrospective summary.
* Propose, but do not confirm, one insight.

Exit criteria:

* Helix remains fully usable with AI disabled.
* AI output cannot silently update memory.
* Model invocation is traceable.
* Sensitive data is not logged.

## Increment 5 — User-Governed Memory

Deliver:

* Temporary context
* Proposed memory
* Confirmed memory
* Derived memory
* accept/reject/edit/delete
* source records
* memory search
* memory settings
* memory export

Exit criteria:

* User can inspect every lasting memory.
* User can understand why it exists.
* Derived patterns are labeled as hypotheses.
* Deleted memory no longer appears in retrieval.

## Increment 6 — Semantic Search

Deliver:

* Embedding abstraction
* Local embedding option
* pgvector migration
* indexing workflow
* hybrid keyword and semantic retrieval
* source citations
* retrieval-quality tests

Exit criteria:

* Search answers include source records.
* Search does not bypass deleted or private records.
* Retrieval can be rebuilt from authoritative data.

## Increment 7 — Personal Knowledge Graph

Deliver:

* Node and edge model
* Graph projections
* AI-proposed relationships
* user governance
* timeline support
* initial graph visualization
* navigation from graph to source record

Exit criteria:

* Every edge has provenance.
* Proposed edges are distinguishable from confirmed edges.
* Graph records do not replace authoritative domain data.

## Increment 8 — Offline Capture and Synchronization Hardening

Deliver:

* Offline reflection drafts
* Outbox
* synchronization state
* retry
* conflict detection
* no-silent-loss behavior
* browser storage protection appropriate to the threat model

Exit criteria:

* User can capture a reflection during a network outage.
* Sync resumes later.
* Duplicate submissions are handled idempotently.
* Conflicts are surfaced rather than silently overwritten.

## Increment 9 — Data Ownership and Recovery

Deliver:

* Full export
* Human-readable export
* backup package
* restore validation
* selective deletion
* delete-all
* retention settings
* recovery runbook

Exit criteria:

* Backup and restore are tested.
* Export includes schema version.
* Destructive actions require deliberate confirmation.
* Deletion behavior is documented.

## Increment 10 — Production Deployment

Deliver:

* Low-cost hosting configuration
* production authentication selection
* secrets management
* TLS
* database backup
* basic operational health
* privacy-preserving telemetry
* release workflow
* rollback instructions
* cost review

Exit criteria:

* Accessible from phone, home, and work browser where organizational policy permits.
* Monthly infrastructure is aligned with single-user usage.
* Backup is active.
* Production secrets are not in the repository.
* No Kubernetes is required.

---

# 17. Backlog Classification

Classify roadmap items as:

* MVP
* Post-MVP
* Research spike
* Architecture enabler
* Security enabler
* UX enabler
* Deferred
* Explicit non-goal

Do not claim that all previously listed functionality belongs in the MVP.

Recommended MVP boundary:

* Repository foundation
* Transformation
* Experiment
* Today view
* Reflection
* Deterministic suggestion
* Basic belief and evidence support
* Basic history
* Secure persistence
* Responsive PWA
* Data export baseline
* Optional local-AI proof of concept only after the non-AI flow works

---

# 18. Required Planning Artifacts

Before implementing substantial functionality, produce:

1. Repository tree proposal.
2. Technology decision summary.
3. Corrected architecture summary.
4. Domain module catalog.
5. Initial conceptual data model.
6. First vertical-slice sequence.
7. Threat-model summary.
8. Initial requirement catalog.
9. Requirement-to-roadmap traceability matrix.
10. Implementation roadmap.
11. Risk register.
12. Open-question log.
13. Proposed first ten backlog issues.

For each backlog issue include:

* Title
* User outcome
* Scope
* Acceptance criteria
* Dependencies
* Testing notes
* Documentation notes
* Requirement IDs
* Estimated relative size
* Risks

---

# 19. Important Architecture Decisions Still Requiring Validation

Record these as open or proposed decisions rather than silently treating them as settled:

* Hosting provider
* Production authentication provider
* PostgreSQL hosting option
* PWA offline-storage strategy
* Multi-device conflict resolution
* Whether the backend serves the web assets or they deploy separately
* Whether Spring Data JDBC or JPA is preferred
* Local-model hosting and secure remote access
* Embedding model
* External AI-provider policy
* Encryption model beyond provider-managed encryption at rest
* Attachment support
* Voice input
* Notification strategy
* Exact data-retention defaults

Do not block repository instantiation on these decisions.

Use replaceable ports, configuration, and documented assumptions.

---

# 20. Definition of Done

A change is complete only when:

* The user-facing outcome is implemented.
* Acceptance criteria are satisfied.
* Domain and module boundaries remain valid.
* Tests cover expected and failure behavior.
* Privacy implications are reviewed.
* AI failure behavior is defined where applicable.
* No sensitive content is logged.
* Documentation is updated.
* Requirement traceability is updated.
* Applicable ADRs are updated.
* CI passes.
* The implementation can be run by another engineer from repository instructions.

---

# 21. Execution Instructions

Proceed in this order:

## Step 1 — Inspect

Inspect the current repository.

Report:

* Existing files
* Existing technologies
* Existing build tools
* Existing agent configuration
* Conflicts with this prompt
* Reusable assets
* Missing prerequisites

Do not overwrite useful existing work.

## Step 2 — Plan

Create a concrete repository-instantiation plan.

List:

* Files to create
* Files to modify
* Generated code
* Dependencies
* Risks
* Verification commands

## Step 3 — Establish Documentation and Decisions

Create the core context, requirements, architecture, ADR, agent, and roadmap documents before or alongside skeleton code.

Avoid empty templates.

## Step 4 — Instantiate Skeleton

Create:

* React PWA
* Spring Boot API
* PostgreSQL local environment
* Shared API-contract approach
* Health checks
* Test foundations
* CI
* scripts
* Agent definitions
* Skills and prompts

## Step 5 — Implement Increment 1 Skeleton

Implement the thinnest coherent version of:

* Transformation
* Experiment
* Reflection
* Deterministic suggestion
* Today view

Do not implement advanced AI, semantic search, or knowledge-graph visualization yet.

## Step 6 — Verify

Run:

* Compilation
* Unit tests
* Architecture tests
* Integration tests
* Frontend tests
* Type checks
* Linting
* Documentation validation
* Diagram validation

## Step 7 — Report

Provide:

* What was created
* Architecture corrections made
* Commands to run the system
* Test results
* Known limitations
* Open decisions
* Recommended next issue
* Exact files requiring human review

Do not state that a command passed unless it was actually executed successfully.

---

# 22. Final Guiding Principle

Helix is not an AI chatbot with personal-development features attached.

It is a personal-growth system whose authoritative foundation consists of user-owned visions, transformations, beliefs, experiments, reflections, evidence, memories, and wisdom.

AI is an optional reflective capability layered over that foundation.

Every implementation decision should protect:

* Personal usefulness
* User ownership
* Privacy
* Nonjudgmental experience
* Longitudinal integrity
* Low operating cost
* Ease of access
* Simplicity
* Evolvability

When faced with a choice between speculative sophistication and a simpler design that supports the current user well, choose the simpler design and document the reconsideration trigger.
