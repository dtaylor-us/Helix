# ADR-016 AI required for generative suggestion content

- Status: Accepted
- Date: 2026-08-01
- Amends: ADR-006 (narrows its "core workflows must work without AI" mandate)

## Context
ADR-006 required every workflow to remain fully functional with no AI provider configured, using
deterministic string-templated content as a permanent, first-class fallback. Product direction has
since shifted: the "Suggested Small Action" after a reflection (and, in subsequent slices, the
weekly retrospective narrative and experiment drafting) are meant to be genuinely AI-authored,
context-aware content, not template selection. Treating deterministic templating as a co-equal,
indefinitely-maintained path for these specific features works against that goal and was assessed
by the user as no longer worth the design cost.

## Decision
For the generative content surfaces explicitly in scope (post-reflection suggestion generation now;
experiment drafting now, and weekly retrospective narrative in a later slice), AI becomes the required
content source. `AiAssistantPort` is still the abstraction boundary (ADR-007's provider selection,
including `HELIX_AI_PROVIDER=none`, is untouched), but the `NoAiAssistantAdapter` / any adapter's
circuit-breaker fallback response is no longer a supported steady-state experience for these
features — it is what the user sees during a transient provider outage or when `none` is configured,
not a target design state maintained with equal care to the AI path. `SuggestionEntity` now records
whether a given suggestion's text came from a live model call or a fallback (`source`,
`ai_provider`, `ai_model` columns), so this distinction is visible in data and to the user rather
than silently blurred.

For experiment drafting specifically, both live-model and fallback responses must still populate the
core editable fields (`title`, `hypothesis`, and `nextAction`) so the user always has a complete
starting point to review before choosing whether to save it.

ADR-008 (user-governed AI memory: proposal → review → explicit acceptance before anything persists
as accepted fact) is explicitly **not** relaxed by this decision. AI-authored suggestions still land
with `status=PROPOSED` and require the existing accept/dismiss/replace flow.

## Alternatives
- Keep deterministic templating as a permanently-maintained equal fallback (status quo per ADR-006).
  Rejected: doubles the design/test surface for content that's explicitly meant to feel intelligent,
  and the user weighed that cost against the benefit and chose to drop it.
- Remove the non-AI code paths entirely (delete `NoAiAssistantAdapter`, `helix.ai.provider=none`).
  Rejected: `none` and per-provider circuit-breaker fallbacks remain useful as an outage/dev-offline
  behavior; the change here is about design intent (what we optimize for), not deleting a safety net.

## Consequences
- Suggestion quality now depends on a configured, reachable AI provider (OpenAI by default per
  ADR-007's cloud-secondary/local-first framing, though OpenAI is the provider actually being used
  operationally right now).
- `running-app.md`'s "core flows still work without AI" claim needed correcting for this feature;
  it now documents suggestion generation as AI-required with an explicit fallback-during-outage note.
- Suggestion provenance (AI vs. fallback, which provider/model) is now persisted and can be surfaced
  in the UI, which ADR-006 didn't require since there was no meaningful distinction to show before.

## Risks
- No timeout/retry is currently wired into the OpenAI/Ollama adapters (`AiProperties.timeoutSeconds`,
  `retryMaxAttempts`, `retryDelayMs` are defined but unused) — a slow provider response blocks the
  reflection-save request rather than failing fast into the circuit breaker. Flagged as follow-up
  work, not blocking this slice, since the existing 30s circuit-breaker window still bounds repeated
  failures.
- Users without an `OPENAI_API_KEY` configured (or with `HELIX_AI_PROVIDER=none`) will now
  consistently see fallback-quality suggestions rather than an occasional degraded case; this is a
  deliberate, not accidental, consequence of this ADR.

## Reconsideration Triggers
Revisit if AI provider cost, latency, or reliability makes deterministic templating necessary again
as a genuinely-maintained parallel path, or if a future privacy/offline-first requirement reinstates
ADR-006's original "must work without AI" scope for these features.

## Related Requirements
HELIX-FR-002, HELIX-FR-004, HELIX-AI-001, HELIX-AI-002, HELIX-BR-001, HELIX-BR-002
