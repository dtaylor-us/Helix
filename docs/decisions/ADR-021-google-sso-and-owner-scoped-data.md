# ADR-021 Google SSO authentication and owner-scoped data isolation

- Status: Accepted
- Date: 2026-08-02
- Supersedes: ADR-013 (finalizes the deferred decision now that hosting-agnostic requirements are known)
- Amends: ADR-001 (Helix is no longer single-user only — a small invite-only group of users is now in scope)

## Context
Every table in the current schema is global and unscoped, and `SecurityConfig` permits all requests
unauthenticated (`anyRequest().permitAll()`). The product is expanding from "just me" to "me plus a
few invited people," and the data involved (reflections, beliefs, private wisdom entries) is
personal-journal-grade sensitive. Two prior decisions bear on this: ADR-013 deferred committing to an
auth vendor until production hosting was known; ADR-001 scoped the whole backend as "single-user."
Hosting is still undecided (same-origin vs. split-origin deployment both remain possible), so the auth
mechanism needs to work under either.

A reference implementation exists in a sibling project (`dtaylor-us/axiom`): each of its several
independently-deployable services validates its own self-issued JWT (username/password sign-up,
`PasswordEncoder`, a hand-rolled `JwtService`, `JwtAuthFilter` on every request), because no two of
axiom's services can share a session cookie. Helix is a single deployable Spring Boot monolith with
one API — that constraint doesn't apply, so replicating axiom's pattern here would mean owning
password storage, JWT secret rotation, and token refresh logic for no corresponding benefit.

## Decision
**Authentication:** Google OAuth2 login (`spring-boot-starter-oauth2-client`, `oauth2Login()`).
Google is the identity provider; Helix never stores or handles a password. On successful OAuth2
callback, the user's email is checked against an `authorized_users` allowlist table (invite-only —
this is not a self-service-signup product) before a local `users` row is created/reused and a
session is established. Session is a standard `HttpSession`-backed cookie (`HttpOnly`, `Secure`,
`SameSite` configurable via `helix.web.allowed-origins`-adjacent property so it works same-origin
today and can be loosened to `SameSite=None` if a split-origin deployment is chosen later — see
Risks). `SecurityConfig` now requires an authenticated session for every `/api/v1/**` route except
`/api/v1/auth/**` and `/api/v1/health`.

**Authorization / data isolation:** every persisted table gets an `owner_id` column (`FK -> users.id`,
`NOT NULL`) rather than relying on derived/transitive ownership through foreign-key chains. This was
evaluated and rejected — see Alternatives — because several entities (`weekly_retrospectives`,
`memory_proposals`, `knowledge_nodes`, `semantic_search_documents`) don't have a clean single FK back
to a root aggregate; some reference their source polymorphically (`sourceRecordId` + a type
discriminator resolved at runtime, not a DB-level FK at all). A denormalized `owner_id` on every table
is simpler, cannot silently break when an unrelated FK chain changes shape, and is checked the same
way everywhere: a `CurrentUserProvider` resolves the authenticated session to a `User.id`; every
repository query is `...AndOwnerId(...)`-scoped; every service's `get()` 404s (not 403 — to avoid
confirming a record exists) when the row's `owner_id` doesn't match the caller.

## Alternatives
- **Copy axiom's per-service JWT pattern.** Rejected: designed for multiple independently-deployable
  services that can't share a cookie; Helix has one deployable API, so a session cookie is strictly
  simpler and removes an entire class of self-owned risk (secret rotation, refresh-token theft,
  password storage) that axiom's architecture requires but Helix's doesn't.
- **Derive ownership transitively through existing FK chains** (e.g. an `Evidence` row's owner is
  whoever owns the `Belief`/`Experiment`/`Reflection` it points at) instead of denormalizing
  `owner_id`. Rejected: several entities have no clean chain (see Decision), and even where a chain
  exists (e.g. `Suggestion.experimentId`), every read path would need to re-walk it, which is both
  slower and a larger surface for a missed-edge-case authorization bug than one indexed column.
- **Username/password local accounts.** Rejected outright given "SSO if it's easy to set up" was the
  user's explicit preference, and it is easy: Spring Security's `oauth2Login()` needs a client
  ID/secret and almost no custom code, versus building and securing our own credential store.

## Consequences
- No password storage, reset flow, or credential-stuffing surface — that risk is Google's to carry.
- One new migration touches every existing table; every repository and service gains an
  owner-scoping obligation that must be upheld by every future entity too (this should become an
  architecture-test candidate, similar to how ADR-001's module boundaries are enforced elsewhere).
- Login requires outbound network access to Google at runtime (consistent with the AI-adapter
  ADRs' existing acceptance of network dependence — see ADR-006/ADR-017 — so this isn't a new class
  of constraint for this app).
- Invite-only via an allowlist table (not a config property) so adding a person is a data change,
  not a redeploy.

## Risks
- **Cookie behavior under split-origin hosting is not yet exercised.** If production ends up as two
  separate domains, `SameSite=None; Secure` plus explicit CORS `allow-credentials` will be needed;
  this ADR's session design supports that but it hasn't been tested against a real split-origin
  deployment yet.
- **Retrofit completeness.** Given the size of the schema (19 tables), this decision is being
  implemented in slices; the development log entry accompanying this ADR states precisely which
  tables are owner-scoped as of a given commit and which are not — treat any table not listed there
  as still globally readable/writable by any authenticated user until it appears in that list.
- **Single Spring instance session store.** In-memory `HttpSession` doesn't survive a restart or
  scale past one instance. Acceptable for "a few invited people," but if this ever needs horizontal
  scaling, a shared session store (Redis via `spring-session-data-redis`) becomes a prerequisite —
  noted here so it isn't a surprise later, not because it's needed now.

## Reconsideration Triggers
- Production hosting settles on a split-origin topology (frontend and API on different domains).
- User count grows beyond "a few invited people" such that self-service signup or a non-Google
  identity provider becomes necessary.
- Horizontal scaling of the API becomes necessary (session store must move out of process memory).

## Related Requirements
HELIX-SEC-002
