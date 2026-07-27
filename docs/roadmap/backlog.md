# Initial Backlog (First Ten Issues)

1. HELIX-ISSUE-001 Setup monorepo foundation and scripts
- User outcome: Local bootstrap and verification in one command set.
- Requirements: HELIX-NFR-003
- Size: M
- Risks: Toolchain drift

2. HELIX-ISSUE-002 Implement transformation create/list/detail API
- User outcome: Define change area.
- Requirements: HELIX-FR-001
- Size: M
- Risks: Validation gaps

3. HELIX-ISSUE-003 Implement experiment creation under transformation
- User outcome: Define active behavioral experiment.
- Requirements: HELIX-FR-002
- Size: M
- Risks: lifecycle ambiguities

4. HELIX-ISSUE-004 Implement reflection capture endpoint and persistence
- User outcome: Save daily experiment reflection.
- Requirements: HELIX-FR-003
- Size: M
- Risks: content length and validation

5. HELIX-ISSUE-005 Add deterministic suggestion generation and response endpoints
- User outcome: Optional small next action with user agency.
- Requirements: HELIX-FR-004, HELIX-FR-005
- Size: M
- Risks: suggestion quality

6. HELIX-ISSUE-006 Build Today API aggregation and UI
- User outcome: One-screen active context and history.
- Requirements: HELIX-FR-006, HELIX-FR-007
- Size: M
- Risks: state consistency

7. HELIX-ISSUE-007 Add architecture boundary tests
- User outcome: Prevent structural regressions.
- Requirements: HELIX-NFR-001, HELIX-NFR-003
- Size: S
- Risks: false positives

8. HELIX-ISSUE-008 Add frontend accessibility and offline draft tests
- User outcome: reliable and accessible reflection capture.
- Requirements: HELIX-UX-001, HELIX-UX-002, HELIX-QAS-OFF-001
- Size: M
- Risks: browser-specific behavior

9. HELIX-ISSUE-009 Define AI provider port and no-AI adapter
- User outcome: optional AI support without core dependency.
- Requirements: HELIX-AI-001, HELIX-AI-002
- Size: L
- Risks: contract drift

10. HELIX-ISSUE-010 Draft export and deletion baseline design
- User outcome: data ownership and recoverability plan.
- Requirements: HELIX-NFR-002
- Size: M
- Risks: schema versioning complexity

11. HELIX-ISSUE-011 Implement beliefs and evidence foundation
- User outcome: inspect and revise beliefs with evidence and provenance.
- Requirements: HELIX-FR-008, HELIX-FR-009, HELIX-FR-010, HELIX-FR-011, HELIX-BR-002, HELIX-BR-004
- Size: L
- Risks: provenance contract drift and unclear revision rationale UX

12. HELIX-ISSUE-012 Implement weekly retrospective, wisdom, and structured search foundation
- User outcome: capture weekly reflection patterns, accept personal wisdom with linked sources, and find records quickly.
- Requirements: HELIX-FR-012, HELIX-FR-013, HELIX-FR-014, HELIX-FR-015, HELIX-FR-016, HELIX-BR-005
- Size: L
- Risks: search relevance drift and source-link UX ambiguity
