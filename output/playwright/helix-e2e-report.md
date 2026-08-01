# Helix end-to-end browser QA report

## 1. Environment

- Date: 2026-08-01; macOS; repository `/Users/derektaylor/projects/helix`.
- Backend: OpenJDK 21.0.10, Spring Boot 4.0.0, PostgreSQL 16.14 in the repository Docker Compose service.
- Frontend: bundled Node.js 24.14.0, Vite 8.1.5, React 19.
- Browser: Google Chrome/Chromium 150.0.7871.187 driven through Playwright 1.62.0.
- Viewports: 1440×1000/1200 desktop and 390×844 mobile.
- AI modes: full deterministic/fallback setup pass with `HELIX_AI_PROVIDER=none`, followed by an explicitly authorized live OpenAI pass using the configured key and `gpt-4o-mini`.
- Test data was synthetic. The run began with an empty product database and an isolated browser context.
- No permanent test suite was added. Throwaway scripts, JSON logs, screenshots, and this report are under `output/playwright/`.
- Not fully exercised: suggestion rejection/replacement, per-experiment draft isolation with two active experiments, wisdom revision history, Memory accept/reject/revise lifecycle, and a complete keyboard-only traversal of every page. Memory creation could not reach a valid proposal during this pass.

Requirements most directly exercised: HELIX-FR-004, FR-012, FR-014, FR-016, FR-017, plus ADR-006, ADR-007, ADR-008, ADR-009, ADR-010, ADR-016, and ADR-017.

## 2. Summary

The central journey works from onboarding through transformation, experiment, live conversational reflection, AI-generated structured review, saved reflection, suggested action, and weekly retrospective. The live model produced relevant follow-ups and clearly marked editable output. Knowledge/evidence/revision, hybrid search, and manual wisdom creation also work.

The most important failure is the missing contextual wisdom-capture card after reflection save, despite the reflection containing populated evidence, noticed, and surprise fields. Memory proposal creation also fails without useful visible feedback when provenance cannot be resolved. A nonsense search can return a very low-confidence semantic result rather than an empty state. Overall health is good but the full promised loop does not reach contextual Evidence/Wisdom cleanly.

## 3. Functional findings

### A. First use / onboarding

Working:

- Empty Today shows the welcome explanation and “Begin my first transformation” CTA. [Screenshot](welcome.png)
- Primary navigation is exactly Today / Journey / Library. More exposes Search, Knowledge, Memory, and Settings.
- Skip-to-content is the first tab stop and moves focus to `#main-content`; active Today styling is clear.
- Tested form controls had associated labels; Today contained two `aria-live` regions.

Issue — blank transformation gives no explanatory validation (minor).

- Action: left the guided form empty.
- Expected: clear inline required-field guidance.
- Actual: Save is disabled, so invalid data is safely blocked, but no text explains what is required. [Screenshot](transformation-created.png)
- Likely owner: `apps/web/src/pages/TransformationsPage.tsx`.

### B. Journey / transformations

Working:

- Created “Respond to feedback with calm curiosity” with purpose, desired identity, and obstacle; it appeared in Journey and opened correctly. [Screenshot](transformation-created.png)

### C. Experiment creation

Working:

- Manual title, hypothesis, next action, cadence, and evidence saved; Today surfaced the experiment. [Screenshot](today-active-experiment.png)
- Drafting then navigating away did not persist an experiment.
- Draft values were editable and Save remained the persistence boundary.

Issue — deterministic draft is incomplete and provenance is not visible (major).

- Action: clicked “Draft this for me” with AI disabled.
- Expected: title, hypothesis, and next action prefilled, plus a status that identifies fallback/provider provenance.
- Actual: only title and next action were filled; hypothesis stayed blank. Status said only “Drafted a starting point. Review and edit before saving,” with no visible fallback/provider label. [Screenshot](experiment-draft-fallback.png)
- Likely owner: `apps/web/src/pages/TransformationDetailPage.tsx` around the draft status rendering and the no-AI draft response mapping.

Observation — after saving, the detail form clears but does not list the saved experiment. The save succeeded and Today confirmed it, but local confirmation is weak. [Screenshot](experiment-saved.png)

### D. Today / reflection and suggestion loop

Working:

- Unsent reflection text survived reload through local storage.
- Live OpenAI conversation asked context-sensitive follow-ups about physical relaxation and future feedback interactions. [Screenshot](reflection-conversation.png)
- “I’m done” produced a fully editable structured review with main answer, tried-it value, noticed, evidence, and surprise fields. “AI suggested — openai” was prominent. [Screenshot](reflection-review.png)
- The reflection saved and appeared in History.
- A relevant suggested action appeared with provider badge and action controls (“I’ll try this,” “Not this one,” “Use this instead”). [Screenshot](today-reflection-saved.png)
- The “This week” teaser appeared, carried an OpenAI badge, and linked to Library.

Issue — contextual wisdom-capture card never appears (major).

- Action: saved a reflection containing non-empty evidence, noticed, surprise, and main-answer text.
- Expected: “This reflection may contain a lesson worth keeping,” prefilled using evidence first, with Save as wisdom / Not now.
- Actual: the saved reflection, suggestion, and retrospective appeared, but the wisdom card was absent both immediately after save and on reload. [Screenshot](today-reflection-saved.png)
- Likely owner: `apps/web/src/pages/TodayPage.tsx` state lifecycle around `setWisdomDraft` (lines 33, 59, and 92) and the render block around line 457. The state is transient and appears to be cleared or lost when Today refreshes after mutation.
- Traceability: HELIX-FR-014; ADR-006, ADR-009, ADR-010.

Not completed: suggestion rejection/replacement and second-experiment draft isolation.

### E. Library / wisdom

Working:

- Weekly retrospective contained a live-model narrative, assistance text, OpenAI badge, and reflection summary. [Screenshot](library-with-reflection.png)
- Manual wisdom creation from the reflection succeeded with a visible source UUID/note. [Screenshot](library-complete.png)

Observation — clicking “Save weekly snapshot” did not surface an obvious retrospective-history section or success message in the current view. The source picker continued to offer “Saved retrospective,” but this pass did not independently prove that a new history entry was created. Likely owner: `apps/web/src/pages/LibraryPage.tsx` / retrospective query invalidation.

Not completed: wisdom revision history. The form was visible, but the follow-up automation did not finish reliably enough to claim a pass.

### F. Knowledge, Search, Memory

Knowledge works:

- Created an empowering belief, added supporting evidence with interpretation and manual provenance, revised the statement with a reason, and saw both evidence timeline and revision history update immediately. [Screenshot](knowledge-evidence-revision.png)

Search works for positive retrieval:

- “curiosity” returned BELIEF and EVIDENCE keyword matches plus a REFLECTION hybrid match, each with match type and score. [Screenshot](search-match.png)

Issue — no-match query returns an extremely weak semantic result (minor; design/tuning question).

- Action: searched `zzzz-no-such-helix-record`.
- Expected: sane empty state.
- Actual: a reflection was returned as semantic score `0.09`.
- Likely owner: `apps/api/src/main/java/com/helix/api/shared/application/SemanticRetrievalService.java` and `StructuredSearchService.java`; no minimum relevance threshold is evident.
- Traceability: HELIX-FR-016/018, ADR-010.

Issue — Memory failure is not explained in the UI (major).

- Action: submitted a manual memory proposal whose source record could not be resolved.
- Expected: a clear source-validation message; with valid provenance, a proposal followed by accept/reject/revise controls.
- Actual: POST `/api/v1/memory/proposals` returned 404, the form silently remained on “No memory proposals yet,” and the browser logged a failed resource. The backend recorded `NoSuchElementException: Reflection not found`. [Screenshot](memory-accepted.png)
- Likely owners: `apps/web/src/pages/MemoryPage.tsx`, `apps/web/src/api/http.ts` lines 136–158, and `apps/api/src/main/java/com/helix/api/memory/adapter/in/http/MemoryProposalController.java` plus its source-validation service.
- Traceability: HELIX-FR-017, ADR-008/010.
- Because no proposal was created, accept/reject/revise state transitions remain unverified.

### G. Cross-cutting

Working:

- Async Today and Search displayed visible loading copy.
- With API stopped, Today retried, then displayed “Unable to load today view right now” rather than a blank screen. [Screenshot](api-down.png)
- No horizontal overflow at 390×844; controls remained reachable and readable. [Screenshot](mobile-today.png)
- All tested main-form controls were label-associated; skip link worked; visible focus was present.
- No unexpected console errors or failed requests occurred during successful core, Knowledge, Search, Library, or live-AI flows.

Mobile UX was usable, though the long Today page requires substantial scrolling after a reflection.

## 4. UX findings

- Tone is calm, nonjudgmental, and free of gamification. “Small experiment,” “notice,” and “wisdom” language consistently supports the product principles.
- User control is strong in the AI flow: conversation does not silently save; the structured review is editable; saving is explicit; suggested actions have accept/reject/replace controls.
- AI provenance is noticeable in structured review, suggestion, and retrospective. In contrast, deterministic experiment drafting is not identified as fallback and can look equivalent to generated assistance.
- The conversational reflection was the strongest part of the experience: follow-ups referenced the user’s actual words and the structured handoff was coherent without sounding clinical.
- The transformation-to-experiment handoff could be clearer because the detail page does not visibly confirm/list the newly saved experiment.
- Perceived responsiveness was good for local CRUD. Live-model operations took several seconds but had understandable task context; no major layout shift or flicker was observed.
- The missing contextual wisdom card is both a functional break and a dead end in the intended loop: the user sees evidence in History but is not invited to preserve it at the moment of reflection.

## 5. Console / network excerpts

Successful flows: no unexpected errors.

Backend runtime log review:

```text
WARN UserDetailsServiceAutoConfiguration:
Using generated security password: [redacted]
This generated password is for development use only. Your security configuration must be updated before running your application in production.

WARN HikariPool-1:
Thread starvation or clock leap detected (housekeeper delta=47s494ms).
```

- The generated-password warning occurred on each API startup. It did not block the tested endpoints, but it indicates Spring Boot's default user-details configuration is still active alongside Helix's security configuration. Treat this as a minor configuration/security-hygiene issue and verify that production profiles never rely on the generated credential. Likely owner: `apps/api/src/main/java/com/helix/api/identity/config/SecurityConfig.java` and `apps/api/src/main/resources/application.properties`.
- The Hikari warning occurred once. No associated database failure, slow visible operation, or connection error followed. This can be caused by host suspension or test-process scheduling, so it is recorded as an operational observation rather than a confirmed Helix defect. Monitor for recurrence under normal runtime load.

Memory failure:

```text
POST http://localhost:8080/api/v1/memory/proposals 404
Failed to load resource: the server responded with a status of 404
Backend: NoSuchElementException: Reflection not found
```

No backend 5xx response was captured. The Memory failure was a 404 produced by unresolved provenance and should still be translated into clear form-level validation rather than remaining a console-only failure.

Intentional API outage:

```text
GET http://localhost:8080/api/v1/today net::ERR_CONNECTION_REFUSED
GET http://localhost:8080/api/v1/transformations net::ERR_CONNECTION_REFUSED
UI after retry window: Unable to load today view right now.
```

During an initial noncanonical-origin probe, `http://127.0.0.1:5173` was correctly blocked by CORS because the documented allowed origin is `http://localhost:5173`; this was setup discovery, not counted as a product bug.

Post-run log audit note: the Compose container had already been removed by the requested cleanup, so historical PostgreSQL container stdout was no longer available through `docker logs`. During the run, PostgreSQL started cleanly, Flyway validated all 9 migrations, schema version 9 was current, and no database exception appeared in the API process output.

## 6. Screenshots

Representative working moments:

- [Welcome](welcome.png)
- [Active experiment on Today](today-active-experiment.png)
- [Live AI conversation](reflection-conversation.png)
- [Editable AI review](reflection-review.png)
- [Saved reflection, suggestion, and weekly teaser](today-reflection-saved.png)
- [Library retrospective](library-with-reflection.png)
- [Knowledge evidence and revision](knowledge-evidence-revision.png)
- [Mobile Today](mobile-today.png)

Bug screenshots are linked inline with their findings above.

## 7. Suggested next steps

1. Fix HELIX-FR-014’s Today wisdom proposal state so it survives the post-save query refresh and render cycle; add a browser-level regression around save → wisdom card.
2. Make Memory provenance validation actionable in the UI and verify the valid-source contract end to end before retesting accept/reject/revise.
3. Add a minimum semantic relevance threshold or explicit “weak semantic matches” policy so nonsense queries can produce an empty state.
4. Include deterministic/fallback provenance in experiment draft status and decide whether the hypothesis must always be populated.
5. Add inline required-field guidance on Journey and stronger local success confirmation/history visibility for experiment and weekly snapshot saves.
6. Complete the remaining interaction matrix: suggestion reject/replace, wisdom revision, two-experiment draft scoping, and a full keyboard-only mobile pass.

The conversational reflection flow was present and tested; its absence is not reported as a bug.
