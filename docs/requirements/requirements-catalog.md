# Requirements Catalog

## Functional Requirements
- HELIX-FR-001: Create and view transformations.
- HELIX-FR-002: Create experiments under a transformation.
- HELIX-FR-003: Record reflections for an experiment.
- HELIX-FR-004: Provide one optional deterministic suggestion after reflection.
- HELIX-FR-005: Accept, dismiss, or replace suggestion.
- HELIX-FR-006: View recent reflection and suggestion history.
- HELIX-FR-007: Provide Today summary for active experiment.
- HELIX-FR-008: Create and view beliefs linked to a transformation.
- HELIX-FR-009: Record evidence that supports or challenges a belief.
- HELIX-FR-010: Revise beliefs and inspect revision history.
- HELIX-FR-011: View an evidence timeline and a non-scored progress narrative.
- HELIX-FR-012: Generate a weekly retrospective draft with deterministic assistance and reflection summaries.
- HELIX-FR-013: Save weekly retrospective snapshots for longitudinal review.
- HELIX-FR-014: Create, view, and revise user-accepted wisdom entries.
- HELIX-FR-015: Link wisdom entries to supporting source records.
- HELIX-FR-016: Search structured records by keyword across reflection, belief, evidence, retrospective, and wisdom data.
- HELIX-FR-017: Create and govern user-reviewed memory proposals with source provenance and accept, reject, edit, and delete actions.
- HELIX-FR-018: Provide hybrid keyword and semantic retrieval with explicit source citations and deterministic local fallback behavior.

## Business Rules
- HELIX-BR-001: AI suggestions must not be mandatory for core workflow.
- HELIX-BR-002: AI-derived information must preserve provenance.
- HELIX-BR-003: No silent memory promotion without user governance.
- HELIX-BR-004: Personal growth views must avoid reducing progress to a single score.
- HELIX-BR-005: Wisdom entries must preserve source linkage to supporting records.

## Non-Functional
- HELIX-NFR-001: Browser never accesses database directly.
- HELIX-NFR-002: Saved reflections survive single process restart.
- HELIX-NFR-003: MVP architecture remains a modular monolith.

## Security and Privacy
- HELIX-SEC-001: Sensitive reflective content must not be logged by default.
- HELIX-SEC-002: External AI provider use requires explicit configuration/consent.

## AI
- HELIX-AI-001: AI invoked through provider-agnostic application port.
- HELIX-AI-002: Deterministic no-AI fallback must exist.

## UX
- HELIX-UX-001: Calm and nonjudgmental language in critical flow.
- HELIX-UX-002: Keyboard-navigable core forms and actions.
