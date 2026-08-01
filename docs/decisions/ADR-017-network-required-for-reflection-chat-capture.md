# ADR-017 Network required for conversational reflection capture

- Status: Accepted
- Date: 2026-08-01
- Amends: ADR-012 (narrows offline scope for reflection capture)

## Context
ADR-012 established offline-capable reflection drafting via local persistence. Phase 5 slice D
replaces the old structured reflection form with a live AI conversation: users send chat turns,
the assistant asks clarifying follow-ups, and a second AI call structures the finished transcript
into `{content, attempted, noticed, evidenceNoted, surprise}` for user review before save.

That interaction depends on a reachable AI provider, so the prior "fully offline reflection
capture" assumption is no longer valid for this surface.

## Decision
Conversational reflection capture now requires network connectivity for:
- sending a chat turn to the assistant, and
- finishing a chat to produce the structured reflection draft.

Offline support is narrowed (not removed entirely):
- unsent in-progress message text may still be buffered locally per experiment
  (`helix:reflection-chat-draft:<experimentId>`), so typing work is not lost,
- but sent transcript turns are not persisted for offline continuation,
- and finishing/structuring cannot proceed without a live connection.

ADR-008 remains in force: the AI-structured draft is only a proposal. Nothing is persisted until
the user explicitly reviews/edits and confirms save through the existing reflection-create flow.

## Alternatives
- Preserve full offline reflection capture by keeping the old deterministic form as a parallel path.
  Rejected: conflicts with the slice-D product decision to replace the form with a conversational
  AI flow instead of maintaining two capture experiences.
- Persist and sync full chat transcripts offline.
  Rejected for MVP scope: materially more storage/sync complexity and governance surface than
  required for this slice.

## Consequences
- Reflection capture quality improves through adaptive follow-up questioning.
- Reflection capture now has a hard online dependency at send/finish boundaries.
- UI must surface connection-required errors clearly (no silent fallback during chat turns).

## Risks
- Poor network conditions can interrupt reflection completion.
- Users may assume full offline continuity from earlier behavior unless docs/UI are explicit.

## Reconsideration Triggers
- Requirement to reintroduce full offline reflection completion.
- Sustained reliability/cost issues that require a deterministic non-AI capture mode again.

## Related Requirements
HELIX-FR-003, HELIX-QAS-OFF-001, HELIX-AI-001, HELIX-AI-002
