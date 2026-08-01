# Helix end-to-end browser regression report

## 1. Environment

- Run date: 2026-08-01 on macOS, commit `4cb9291`.
- Backend: OpenJDK 21.0.10, Spring Boot 4.0.0, PostgreSQL 16.13 in an isolated fresh Docker volume. All nine Flyway migrations were applied to an empty schema.
- Frontend: bundled Node.js 24.14.0, Vite 8.1.5, React 19.
- Browser: Google Chrome/Chromium 150 driven by Playwright 1.62.0.
- Viewports: 1440×1000/1100 desktop and 390×844 mobile.
- AI modes: deterministic fallback (`HELIX_AI_PROVIDER=none`) followed by live OpenAI (`gpt-4o-mini`) using the configured key.
- Browser console warnings/errors, failed requests, HTTP responses >=400, API output, and database startup/migration output were inspected throughout. Throwaway scripts and evidence are contained in this directory.
- Not completed in this rerun: suggestion Accept/Not-this-one, wisdom “Not now,” draft isolation across two active experiments, every async route's transient loading state, or a full mobile traversal. Those should not be inferred as passes.

## 2. Summary

The applied fixes materially improve Helix. The full happy path now works from a zero-data welcome screen through transformation, experiment, live conversational reflection, editable structured review, contextual wisdom capture, weekly retrospective, saved snapshot, wisdom revision, Knowledge, Search, and governed Memory transitions.

The earlier major wisdom-capture and Memory-feedback defects are fixed. Two Today issues remain: replacing a suggestion records `REPLACED` but continues displaying the original AI suggestion, and the weekly teaser is stale immediately after saving the first reflection until Today is reloaded. No unexpected console/network failures occurred in successful flows.

## 3. Functional findings

### A. First use / onboarding

Working:

- Empty Today displayed the growth-loop welcome state and “Begin my first transformation.” [Screenshot](welcome-fixed.png)
- Primary navigation was Today / Journey / Library. More exposed Search, Knowledge graph, Memories, and Settings.
- In a fresh page, the skip link was the first tab stop, had a visible 3px focus outline, and moved focus to `#main-content`. Today carried `aria-current="page"`. All inspected main-form controls had accessible labels, and two polite `aria-live` regions were present. [Screenshot](keyboard-focus.png)

### B. Journey

Working:

- Blank title was blocked and now explains: “Add a title to save your transformation.”
- Title, purpose, desired identity, and obstacle saved and rendered correctly. [Screenshot](transformation-created-fixed.png)

The prior blank-validation issue is fixed in `apps/web/src/pages/TransformationsPage.tsx`.

### C. Experiment creation

Working:

- Fallback “Draft this for me” populated title, hypothesis, next action, cadence, and evidence. It clearly said “Fallback draft — none.” [Screenshot](experiment-draft-fixed.png)
- Navigating away without saving persisted nothing.
- Manual save surfaced the experiment on Today and now gives strong local confirmation plus a Current active experiment card on the transformation detail page. [Screenshot](experiment-confirmation-fixed.png)

The prior incomplete fallback/provenance and weak-confirmation issues are fixed.

### D. Today — reflection and suggestion loop

Working:

- Unsent chat text survived reload.
- Live OpenAI follow-ups appeared progressively; the second appeared only after the second user turn. [Screenshot](ai-conversation.png)
- “I'm done” produced an editable structured review with a visible OpenAI badge. [Screenshot](ai-review.png)
- Saving created a provider-marked suggestion, reflection history, and the contextual wisdom card.
- Wisdom prefill correctly prioritized `evidenceNoted`, remained editable, saved only after explicit confirmation, and showed “Wisdom saved to your Library.” [Screenshot](reflection-saved-wisdom-card.png)
- The weekly teaser and OpenAI badge appeared after reload and linked to Library. [Screenshot](today-weekly-replaced.png)

Issue — replacement text is not displayed (major).

- Action: entered “Take one breath before replying” and selected “Use this instead.”
- Expected: the user-authored replacement becomes the displayed current action.
- Actual: status changed immediately and persisted as `REPLACED`, but the visible action remained the original AI suggestion. [Screenshot](today-weekly-replaced.png)
- Likely owner: `apps/web/src/pages/TodayPage.tsx:268`, which always renders `latestSuggestion.text` and does not use `replacementText` from the persisted suggestion.

Issue — weekly teaser is stale after first reflection save (minor).

- Action: saved the first reflection and waited through the suggestion/wisdom response.
- Expected: “This week” appears once the reflection exists.
- Actual: it was absent in the same session, then appeared immediately after reload.
- Likely owner: `apps/web/src/pages/TodayPage.tsx:96-116`; reflection success invalidates `['today']` but not `['weekly-retrospective-draft']`.
- Evidence: [immediate post-save](reflection-saved-wisdom-card.png), [after reload](today-weekly-replaced.png).

### E. Library / Wisdom

Working:

- Weekly retrospective showed live-model summary, assistance, OpenAI provenance, and reflection summaries.
- “Save weekly snapshot” immediately announced success and added an entry under Saved snapshots.
- Contextual reflection-linked wisdom appeared in the library with provenance.
- Revising its statement with a reason updated the current entry and revision history. [Screenshot](library-final.png)

### F. Knowledge, Search, Memory

Working:

- Knowledge created a belief, attached reflection-sourced evidence, rendered provenance/narrative, and recorded a reasoned revision. [Screenshot](knowledge-regression.png)
- Search returned keyword and semantic result metadata in the form `TYPE (match type, score)`. A nonsense query now produced “No matches found,” fixing the prior weak-score result. [Screenshot](search-empty-regression.png)
- Memory created a reflection-backed proposal and updated without reload through accept (`PROPOSED → CONFIRMED`), revision (`CONFIRMED → PROPOSED`), and reject (`PROPOSED → REJECTED`). [Screenshot](memory-regression.png)
- Invalid provenance now returns HTTP 400 and a clear form-level message: “That source record couldn't be found — check the ID/type and try again.” [Screenshot](memory-invalid-visible.png)

The prior Memory silent-failure issue is fixed. The expected 400 still produces the browser's standard failed-resource console line; the user also receives actionable UI feedback.

### G. Cross-cutting

Working:

- Today displayed “Loading your active context...” while requests were pending.
- With the API intentionally stopped, it retried and then displayed “Unable to load today view right now,” not a blank screen. [Screenshot](api-down.png)
- Today had no horizontal overflow at 390×844 and controls remained reachable. [Screenshot](mobile-today-fixed.png)
- Successful fallback, live-AI, Library, Knowledge, Search, and valid Memory flows produced no unexpected console errors, warnings, failed requests, or HTTP >=400 responses.
- API startup was clean: the prior generated Spring Security password warning did not recur.

## 4. UX findings

- The tone remains calm, nonjudgmental, and free of scoring/gamification.
- AI provenance is noticeable on the reflection review, suggested action, and retrospective. Fallback drafting is now explicitly distinguishable from AI.
- User control is strong around reflection and wisdom: AI output is editable and nothing silently becomes persisted wisdom.
- The contextual wisdom handoff is now especially effective: it proposes the most evidence-like sentence at the exact moment it is useful.
- The replacement-action defect undermines control because Helix acknowledges `REPLACED` while continuing to foreground the AI wording.
- The weekly teaser's reload dependency creates a small dead spot after the first reflection.
- Local CRUD felt immediate. Live AI turns took several seconds but had stable surrounding UI and no observed flicker or damaging layout shift.
- Mobile Today is readable without overflow, though the post-reflection page remains long.

## 5. Console / network log excerpts

Successful flows: no unexpected events.

Expected invalid-Memory request:

```text
POST http://localhost:8080/api/v1/memory/proposals 400
UI: That source record couldn't be found — check the ID/type and try again.
API: Resolved [java.lang.IllegalArgumentException: That source record couldn't be found — check the ID/type and try again.]
```

Intentional API outage:

```text
GET http://localhost:8080/api/v1/today net::ERR_CONNECTION_REFUSED
GET http://localhost:8080/api/v1/transformations net::ERR_CONNECTION_REFUSED
UI before retries: Loading your active context...
UI after retries: Unable to load today view right now.
```

Database/API startup:

```text
Successfully applied 9 migrations to schema "public", now at version v9
AI Provider explicitly disabled (NONE). Using deterministic fallback.
AI Provider initialized: OpenAI (model: gpt-4o-mini)
```

The generated-security-password warning from the previous report did not recur.

## 6. Screenshots

Representative working states and all reported bugs are linked inline above. Machine-readable event/check logs are available as `fallback-regression.json`, `a11y-check.json`, `ai-core.json`, `surfaces.json`, `final-verification.json`, and `api-down.json` in this directory.

## 7. Suggested next steps

1. Render a replaced suggestion's persisted replacement value, and add a browser regression asserting the displayed text changes—not only its status.
2. Invalidate/refetch `['weekly-retrospective-draft']` after reflection save so the first weekly teaser appears without reload.
3. Complete the remaining interaction matrix: Accept, Not this one, wisdom Not now, two-experiment draft isolation, and loading-state checks for every async surface.
4. Run the full core loop at mobile width, not only the Today layout spot-check.

Conversational reflection exists and was exercised; its absence is not reported as a bug.
