# Traceability Matrix

| Requirement ID | Current Implementation | Tests | ADRs |
|---|---|---|---|
| HELIX-FR-001 | POST /api/v1/transformations (title, purpose, optional desiredIdentity/obstacle), GET list/detail, guided Journey page | TransformationServiceTest, TransformationsPage.test.tsx | ADR-001, ADR-003, ADR-004, ADR-010 |
| HELIX-FR-002 | POST /api/v1/transformations/{id}/experiments (title, hypothesis, nextAction, optional cadence/evidenceOfSuccess/reviewAt), guided experiment form on TransformationDetailPage with local save confirmation/current active experiment summary | ExperimentServiceTest, TransformationDetailPage.test.tsx | ADR-001, ADR-003, ADR-004, ADR-010 |
| HELIX-FR-003 | POST /api/v1/experiments/{id}/reflections (content, optional attempted/noticed/evidenceNoted/surprise), progressive reflection UI on Today with morning/evening framing | ReflectionServiceTest, TodayPage.test.tsx | ADR-004, ADR-010 |
| HELIX-FR-004 | AI-generated suggestion after reflection via AiAssistantPort.suggestNextAction (OpenAI by default); suggestion records source (AI/DETERMINISTIC) and provider/model, surfaced as an "AI suggested" badge on Today; deterministic templating (SuggestionService.createDeterministic) remains as the underlying no-provider/outage fallback content, not a maintained parallel path | SuggestionEntityTest, ReflectionServiceTest, OpenAiAssistantAdapterTest/OllamaAssistantAdapterTest/NoAiAssistantAdapterTest (suggestNextAction), TodayPage.test.tsx (AI badge) | ADR-006, ADR-007, ADR-016 |
| HELIX-FR-005 | Suggestion accept/dismiss/replace endpoints | Suggestion domain test | ADR-006 |
| HELIX-FR-006 | Today history payload and UI section | Web Today view behavior | ADR-010, ADR-012 |
| HELIX-FR-008 | POST /api/v1/beliefs, GET /api/v1/beliefs, Knowledge belief form and list | BeliefService test, web knowledge page test | ADR-001, ADR-003, ADR-010 |
| HELIX-FR-009 | POST /api/v1/beliefs/{id}/evidence with provenance payload | EvidenceService test | ADR-004, ADR-009, ADR-010 |
| HELIX-FR-010 | POST /api/v1/beliefs/{id}/revisions and detail revision history | BeliefService test | ADR-001, ADR-009 |
| HELIX-FR-011 | GET /api/v1/beliefs/{id} evidence timeline + narrative, Knowledge detail page | Belief detail API compile path, web knowledge page test | ADR-009, ADR-010 |
| HELIX-FR-012 | GET /api/v1/wisdom/weekly-retrospective with reflection summaries and deterministic assistance; also surfaced contextually as a "This week" teaser on Today when there is one to show | WeeklyRetrospectiveService test, web wisdom page test, TodayPage.test.tsx (weekly retrospective teaser) | ADR-001, ADR-003, ADR-010 |
| HELIX-FR-013 | POST /api/v1/wisdom/weekly-retrospective and GET /api/v1/wisdom/retrospectives snapshot history | WeeklyRetrospectiveService test | ADR-004, ADR-010 |
| HELIX-FR-014 | POST /api/v1/wisdom, GET /api/v1/wisdom, GET detail, POST revision + Wisdom page; contextual "this reflection may contain a lesson worth keeping" prompt on Today (editable, deterministically prefilled, reflection-sourced) as the primary capture path, manual entry on the Wisdom page remains available | WisdomService test, web wisdom page test, TodayPage.test.tsx (contextual wisdom prompt) | ADR-001, ADR-009, ADR-010 |
| HELIX-FR-015 | Wisdom source links persisted with typed references (reflection, evidence, retrospective) | WisdomService test | ADR-009, ADR-010 |
| HELIX-FR-016 | GET /api/v1/search returns structured keyword results across modules, merged with semantic matches where available | StructuredSearchService test, web search page test | ADR-010 |
| HELIX-FR-017 | POST /api/v1/memory/proposals and review actions with provenance and lifecycle history | MemoryProposalServiceTest, web memory page test | ADR-008, ADR-010 |
| HELIX-FR-018 | Semantic index rebuild workflow and deterministic local embedding adapter with source-cited hybrid retrieval | SemanticIndexingServiceTest, LocalHashEmbeddingAdapterTest, StructuredSearchServiceTest | ADR-004, ADR-006, ADR-010 |
| HELIX-NFR-001 | Browser uses API client; no DB adapters in web | Architecture boundary tests | ADR-005 |
| HELIX-NFR-002 | Persistence via PostgreSQL with Flyway migrations | Database integration tests | ADR-004 |
| HELIX-NFR-003 | Modular monolith with explicit domain boundaries | Architecture boundary tests (ArchUnit) | ADR-001 |
| HELIX-SEC-001 | Logging policy docs and no explicit content logging | CI policy checks + review | ADR-014 |
| HELIX-SEC-002 | AI provider requires explicit configuration; consent via provider selection | Configuration docs (ai-provider-setup.md) and AiProperties | ADR-006, ADR-008 |
| HELIX-AI-001 | AiAssistantPort interface, factory pattern for provider selection, OpenAI/Ollama/NoOp adapters | AiProviderFactoryTest, OpenAiAssistantAdapterTest, OllamaAssistantAdapterTest | ADR-006, ADR-007, ADR-008 |
| HELIX-AI-002 | NoAiAssistantAdapter returns deterministic fallback; all providers gracefully degrade to NoOp on failure | NoAiAssistantAdapterTest, circuit breaker health checks, chaos tests | ADR-006, ADR-007 |
| HELIX-BR-001 | All workflows tested and functional without AI enabled (provider=none), **except** post-reflection suggestion generation (HELIX-FR-004), which requires AI per ADR-016 and only degrades to fallback text when provider=none or a provider call fails | Core flow integration tests with HELIX_AI_PROVIDER=none | ADR-006, ADR-016 |
| HELIX-BR-002 | Evidence provenance payload and detail rendering | EvidenceService test | ADR-009 |
| HELIX-BR-004 | Knowledge narrative is descriptive rather than scored | Web knowledge detail behavior | ADR-009 |
| HELIX-BR-005 | Wisdom creation requires linked supporting sources | WisdomService test | ADR-009 |
| HELIX-UX-001 | Calm and nonjudgmental language in suggestions and UI; first-use welcome/empty states replace inert or roadmap-facing copy | Manual review + deterministic suggestion phrasing; TodayPage.test.tsx (welcome state, no-roadmap-language assertion) | ADR-001 |
| HELIX-UX-002 | Keyboard-navigable core forms and actions; skip link, visible focus states, active-route styling, disabled-while-pending actions, aria-live save status | Web vitest keyboard navigation tests; AppLayout.test.tsx | ADR-002 |
