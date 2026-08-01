# ADR-018 AI-derived memory proposal candidates

- Status: Accepted
- Date: 2026-08-02
- Amends: ADR-006 (narrows AI-optionality for this generative surface, same pattern as ADR-016)

## Context
Memory governance (ADR-008) has always supported an `AI_DERIVED` source kind, but nothing in the
app ever generated a candidate memory statement — the entire Memory workspace was a manual-entry
tool with no contextual trigger. Phase 6 closes that gap: after a reflection is saved, Helix now
offers an AI-generated candidate "worth remembering" statement, distinct from the existing
deterministic wisdom-capture card (wisdom captures a *lesson* from the experiment; memory captures
a durable *fact/pattern about the user* worth carrying into future context).

## Decision
Add `AiAssistantPort.proposeMemory(String context)`, implemented across all three adapters
(OpenAI, Ollama, NoOp), following the same required-not-optional convention ADR-016 established
for suggestions, the weekly retrospective narrative, and experiment drafts: when a real AI provider
is configured and healthy, the proposed statement comes from a live model call; the
`deterministicFallback` flag distinguishes that from an outage/no-provider response.

Unlike the other three ADR-016 surfaces, there is no meaningful deterministic *content* fallback
here — a templated "fact about you" would be actively misleading, not just less personalized. So
`NoAiAssistantAdapter.proposeMemory` (and the outage-fallback path in the live adapters) returns a
`null` statement rather than placeholder text; the caller (`TodayPage`) simply doesn't show a
memory-proposal card in that case, the same way the app already handles "there's nothing here yet"
elsewhere.

The model is also explicitly allowed to decide there's nothing durable worth proposing for a given
reflection (a one-off event, not a pattern) — it's prompted to respond `NONE` in that case, which
the adapter maps to a `null` statement with `deterministicFallback: false` (a legitimate live
answer, not a failure).

New `MemoryProposalService.proposeFromReflection(UUID reflectionId)` builds context from the
reflection and its experiment and calls the port — nothing is persisted by this call. The existing,
unmodified `POST /api/v1/memory/proposals` endpoint remains the only way a proposal actually lands
as `PROPOSED`; the new `POST /api/v1/memory/proposals/draft` endpoint only prefills what the user
is asked to review, exactly the same propose → review → accept shape ADR-008 already requires and
Slices A/C/D already used for suggestions, experiment drafts, and reflection structuring.

## Alternatives
- Deterministic prefill (like wisdom capture), reusing the reflection's own text directly.
  Rejected: a memory statement is supposed to be an inference *about the user* (a pattern,
  preference, or durable fact), not a restatement of what happened — that inference step is exactly
  what makes AI a better fit here than a priority-ordered field copy.
- Trigger memory proposals from the weekly retrospective instead of per-reflection.
  Rejected for this slice: per-reflection triggering matches the existing wisdom-capture card's
  cadence and gives faster feedback loops; retrospective-level memory synthesis remains a
  reasonable future enhancement, not mutually exclusive with this.

## Consequences
- Memory's fully-built governance workspace (propose/revise/accept/reject, source validation) now
  has a real contextual entry point instead of being reachable only through manual data entry.
- A fourth AI-required generative surface exists alongside suggestions, retrospective, and
  experiment drafts — `HELIX-BR-001`'s no-AI-workflow guarantee is narrowed further, consistently
  with ADR-016.
- Memory and wisdom remain two distinct, separately-triggered proposals on Today after a reflection
  save — this doubles the number of contextual cards a user may see per reflection, which is a
  UX tradeoff worth watching, not just a technical one.

## Risks
- Two AI calls now fire after every reflection save (suggestion generation plus this), increasing
  latency and provider cost per reflection; `AiProperties.timeoutSeconds`/`retryMaxAttempts` remain
  unused by any adapter (pre-existing gap, still not fixed).
- A user could find two proposal cards (wisdom + memory) after one reflection overwhelming rather
  than helpful; worth a UX check-in once this ships.

## Reconsideration Triggers
- User feedback that showing both a wisdom card and a memory card per reflection is noisy —
  would motivate merging them or making one opt-in.
- A future move to synthesize memory proposals from the weekly retrospective instead of (or in
  addition to) individual reflections.

## Related Requirements
HELIX-FR-017, HELIX-AI-001, HELIX-AI-002, HELIX-BR-001
