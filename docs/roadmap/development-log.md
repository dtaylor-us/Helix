# Development Log

This log is updated at the end of significant delivery sessions.

## 2026-08-01 Session - Product Experience Realignment, Phase 5 Slice A: AI-Generated Suggestions

Summary:
- The user asked for a major scope change from the original external-review-derived plan: real AI reasoning driving suggestions, retrospectives, and experiment drafting, plus a more conversational (less form-heavy) UX. They explicitly chose the broadest first-slice scope, explicitly chose to drop the previously-mandatory deterministic no-AI fallback requirement, and chose OpenAI as the provider.
- This session's slice ("Phase 5 slice A") wires real AI into the one generative surface that's small enough to ship end-to-end in one session: the "Suggested Small Action" shown after a reflection is saved. The other three pieces the user asked for (AI weekly retrospective, AI-drafted experiments, conversational reflection) are scoped in `docs/roadmap/product-experience-realignment-plan.md` under Phase 5 slices B-D but not yet built — the conversational reflection flow in particular is a large enough UX change that it needs its own scoping pass before implementation.
- Investigation confirmed (via full-codebase read of `apps/api/src/main/java/com/helix/api/ai/`) that the AI adapter layer (`AiAssistantPort`, `OpenAiAssistantAdapter`, `OllamaAssistantAdapter`, `NoAiAssistantAdapter`, `AiProviderFactory`, `AiOrchestrationService`) was fully built and tested but had zero callers anywhere in the live application — every "suggestion" and "retrospective" the user saw was pure string-template selection, no model call involved.

Changes:
- `AiAssistantPort` gained `suggestNextAction(String context)` alongside the existing `suggestReflectiveQuestion`; implemented in all three adapters with an action-specific coaching prompt and a distinct fallback string.
- `ReflectionService.create(...)` now builds a context string from the active experiment (title/hypothesis/nextAction) and the just-saved reflection (content/noticed/evidenceNoted/surprise/previous-attempt count), calls `aiAssistantPort.suggestNextAction(...)`, and persists the result via a new `SuggestionService.createFromAi(...)` instead of `createDeterministic(...)`.
- `SuggestionEntity` gained `source` (`AI`/`DETERMINISTIC`), `ai_provider`, and `ai_model` columns (Flyway `V8__suggestion_ai_provenance.sql`), with a backward-compatible legacy constructor defaulting existing callers to `DETERMINISTIC`/no-provenance. `SuggestionController`, `ReflectionController`, and `TodayController` DTOs all now expose these three fields.
- `packages/contracts/src/index.ts`'s `Suggestion` type gained the same three fields; `TodayPage.tsx` shows an "(AI suggested — openai)" badge next to the suggestion text when `source === 'AI'`.
- New ADR-016 ("AI required for generative suggestion content") narrows ADR-006's "core workflows must work without AI" mandate specifically for this feature (and the Slice B/C features to come), while leaving ADR-007 (provider selection) and ADR-008 (propose → review → accept governance — AI suggestions still land as `PROPOSED`) untouched. ADR-006 was annotated with a pointer to ADR-016 rather than rewritten.
- `docs/requirements/traceability-matrix.md` (HELIX-FR-004, HELIX-BR-001) and `docs/running-app.md` (AI provider section) updated to stop claiming this flow works identically without AI — it now documents AI as the default content source and fallback-during-outage as a degraded, not equal, experience.

**Governance (ADRs)**:
- ADR-016 (new): AI required for generative suggestion content; amends ADR-006's scope for this feature.
- ADR-006: unchanged in substance, annotated to point at ADR-016.
- ADR-007, ADR-008: unchanged and still govern provider selection and user-acceptance-gating respectively.

Verification:
- Backend changes (`apps/api`) could not be compiled or run in this sandbox — no JDK 21 available (only JDK 11), consistent with every prior phase's backend work in this engagement. All Java changes were hand-reviewed for correctness, including a check that no other production or test call site broke (`grep` for `new SuggestionEntity(`, `new ReflectionService(`, `createDeterministic` across `apps/api`). `./scripts/test-backend` and `./scripts/verify-architecture` (ArchUnit layering check — confirmed the new `ReflectionService → AiAssistantPort` dependency doesn't violate either existing layering rule, since both are application-layer, not domain or adapter.in.http/adapter.out.persistence) still need to run in CI or on the user's machine before merging.
- Frontend verified for real in a scratch clone (`/tmp/helix-work/repo`, synced from the mounted repo, fresh `npm install` due to the native-binding/arm64 mismatch documented in earlier sessions): `npm run typecheck`, `npm run lint`, `npx vitest run` (13/13 tests across 7 files, including the new AI-badge assertion), and `npm run build` all passed. `./scripts/check-docs` also passed.

Known limitations / follow-ups:
- `AiProperties.timeoutSeconds`/`retryMaxAttempts`/`retryDelayMs` remain unused by any adapter (pre-existing gap, not introduced this session) — a slow OpenAI response blocks the reflection-save request rather than failing fast; flagged in ADR-016, not fixed here.
- Slices B (AI weekly retrospective), C (AI-drafted experiments), and D (conversational reflection) are scoped in the plan doc but not implemented.
- No live end-to-end verification against a real OpenAI API key was possible in this sandbox (network egress to `api.openai.com` isn't available here); the adapter's existing circuit-breaker/fallback behavior was exercised, not a real model response.

## 2026-07-27 Fix - Add missing CORS configuration to apps/api

Summary:
- The user reported the web app failing to load Today with a browser CORS error (`No 'Access-Control-Allow-Origin' header is present`) when running the API and web app locally against each other, after separately resolving an unrelated local `.env` port-mismatch issue.
- Investigation found `apps/api` had **no CORS configuration anywhere** — no `CorsConfigurationSource` bean, no `@CrossOrigin`, no CORS-related properties. This is a pre-existing gap that predates this session's work; it would affect anyone running the web app against the API cross-origin (including the documented local dev setup in `docs/running-app.md`), not just this user.
- Fixed by adding a `CorsConfigurationSource` bean and `.cors(Customizer.withDefaults())` to the existing `SecurityConfig` (`apps/api/src/main/java/com/helix/api/identity/config/SecurityConfig.java`), configured via a new `helix.web.allowed-origins` property (`application.properties`), defaulting to `http://localhost:5173` (the Vite dev server) and overridable via `HELIX_WEB_ALLOWED_ORIGINS`.
- Updated `.env.example` and `docs/running-app.md` (new "CORS errors" troubleshooting entry) to document the new variable.

**Governance (ADRs)**:
- ADR-005/ADR-010 (browser communicates with backend via REST over HTTP) — this fix makes that communication actually functional cross-origin; no ADR change needed, this closes an implementation gap rather than changing the architecture.
- No ADR superseded.

Verification run:
- Hand-reviewed against Spring Security conventions; this execution sandbox has no JDK 21, so `./scripts/test-backend` and `./scripts/verify-architecture` were **not run** here. **Please run them locally before relying on this change.** The fix was applied directly to the user's local working tree so they could restart `./scripts/dev-api` and unblock immediately; confirmation that it resolves the browser CORS error is still pending.

Known limitations:
- The default allowed origin is a single dev-server URL; deployed environments will need `HELIX_WEB_ALLOWED_ORIGINS` set explicitly (comma-separated for multiple origins).
