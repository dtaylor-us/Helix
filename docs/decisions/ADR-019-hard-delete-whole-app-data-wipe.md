# ADR-019 Hard delete, whole-app scope, for data deletion (amends ADR-015)

- Status: Accepted
- Date: 2026-08-02
- Amends: ADR-015 (data export and deletion as foundational capabilities — decided *that* this
  must exist, but not *how*)

## Context
ADR-015 committed Helix to treating data export and deletion as foundational, but left two
questions open that block any implementation: what happens to a record's revision history when the
record is deleted (beliefs, wisdom entries, and memory proposals all keep append-only revision
trails), and what the deletion endpoint's scope is, given Helix has no auth yet (ADR-013 defers it
behind a port) and is effectively single-user today.

## Decision
**Hard delete.** Deleting data removes the rows — records and their revision history — permanently.
No `deleted_at`/tombstone column is added to any table. Rationale: Helix is a single-user personal
tool today with no compliance or audit-trail requirement driving retention, and every revision
trail's purpose (beliefs, wisdom, memory proposals) is to let *the same user* see how their own
thinking changed over time — once the user asks to delete everything, there is no longer a "same
user" for that history to serve. Soft-delete would mean adding a deleted/excluded state to every
module's domain model and every read query for a benefit (recoverability) nobody has asked for.

**Whole-app scope.** `DELETE /api/v1/data` deletes every record across every module. There is
currently no per-user boundary to delete a *subset* of — "delete my account" and "delete everything
in the database" are the same operation right now. Per-transformation deletion was considered and
rejected for this ADR (see Alternatives); it's a different, additive feature, not what ADR-015 was
about.

**Export excludes the semantic search index.** `semantic_search_documents` holds pgvector/hash
embeddings derived from other records' text, not user-authored content. It's regenerated on demand
via the existing `POST /api/v1/search/index/rebuild` endpoint, so exporting it would just be
exporting a derived cache, not returning something the user owns that can't be reconstructed. It
*is* deleted as part of the whole-app wipe, since leaving stale embeddings for since-deleted records
around would be a bug, not a feature.

**Deletion requires an explicit confirmation flag in the request body** (`{"confirm": true}`) rather
than accepting a bare `DELETE` with no body. This doesn't add real security — the caller fully
controls the body — but it does mean a client can't destroy all data with a reflexive, no-body
`DELETE` call; the caller has to construct a request that says what it's doing.

**Onboarding state is reset, not deleted**, back to `NOT_STARTED` — the singleton row still needs
to exist (Phase 7's `OnboardingService.get()` bootstraps it if missing anyway), and "wipe everything"
should put a user back at the true first-use welcome screen, which is exactly what `NOT_STARTED`
means.

## Alternatives
- Soft delete / tombstone rows. Rejected: see Decision above — no retention requirement exists to
  justify the added complexity across every module.
- Per-transformation deletion instead of (or in addition to) whole-app wipe. Rejected for this ADR:
  it's a reasonable future feature but a different scope than what ADR-015 committed to, and adds
  design surface (what happens to beliefs/wisdom/memory that reference more than one transformation
  indirectly) this ADR doesn't need to resolve to close the original commitment.
- Exporting the semantic search index alongside user data. Rejected: it's a derived cache with its
  own rebuild endpoint, not a distinct source of truth.

## Consequences
- `DELETE /api/v1/data` is irreversible. There is no undo, no trash, no grace period.
- Once auth exists, this ADR's "whole-app" framing will need to be revisited and re-scoped to
  per-user — noted explicitly as a reconsideration trigger below so it isn't missed.
- The export bundle is a complete, human-readable JSON snapshot of everything exportable; it is the
  *only* backup path today (no automated backups exist), so users relying on it for recovery need to
  actually run it before calling delete.

## Risks
- No confirmation beyond the request-body flag — a scripted or automated client could still delete
  everything without a human in the loop. Explicitly accepted given the single-user, no-auth context;
  should be revisited if this ever runs multi-tenant.
- Hard delete means a bug in the delete-everything code path is unrecoverable in production with no
  backup strategy in place yet. Mitigated by ordering deletes leaf-first (children before parents)
  even though most FKs already cascade, and by covering the path with tests before this ships.

## Reconsideration Triggers
- Auth (ADR-013) gets implemented — this ADR's whole-app scope must be revisited immediately;
  shipping per-user auth without also re-scoping deletion would be a serious bug, not just a gap.
- A future requirement for audit trails, compliance retention, or multi-tenant use — any of these
  would invalidate the hard-delete decision and require soft-delete/tombstone instead.
- Automated backups get built — would reduce (but not eliminate) the "irreversible with no safety
  net" risk noted above.

## Related Requirements
HELIX-FR-006, HELIX-NFR-002
