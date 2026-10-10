# Design

## Context

See proposal.md — Why. The writer hand-emits `.bru` text. `collectionVariables` writes the `vars:pre-request { baseUrl: ... }` block into `collection.bru`; `requestContent` hardcodes `auth: none` inside each request's verb block; `writeEnvironments` writes `vars { baseUrl: ... }` per environment. Auth settings live in `BrunoGeneratorState`.

## Goals / Non-Goals

Goals:
- A single collection-level auth scheme chosen in settings, inherited by every request, with credentials parameterized per environment.

Non-Goals:
- Spring Security auto-detection (future), OAuth2 token exchange, per-endpoint auth exceptions (e.g. a public `/login`).

## Decisions

**D1 — `authMode` enum in settings, default `NONE`.**
`enum AuthMode { NONE, BEARER, BASIC, API_KEY }` on `BrunoGeneratorState`, default `NONE` so existing output is byte-identical until the user opts in. Credential *values* are never stored in settings — only the mode. Values live in the generated environment files for the user to fill.

**D2 — Auth lives at the collection level; requests inherit.**
Emit the auth block in `collection.bru` and switch each request to `auth: inherit` (vs `auth: none` for `NONE`). This centralizes the scheme, so regeneration never clobbers per-request auth the user set, and a credential change is a one-line env edit.
- *Alternative considered*: per-request auth blocks. Rejected — repetitive, and regeneration would overwrite user edits; inherit is the Bruno-idiomatic choice.

**D3 — Variables, not literals; provisioned per environment.**
The auth block references `{{token}}` / `{{username}}`+`{{password}}` / `{{apiKey}}`. `writeEnvironments` adds the active mode's credential keys (empty) next to `baseUrl`, so switching environment in Bruno swaps credentials. The generator never writes a secret literal.

**D4 — Confirm exact Bruno `.bru` auth syntax before emitting.**
The writer produces raw `.bru`, so the emitted auth block must match Bruno's grammar exactly (`auth { mode: bearer }` + `auth:bearer { token: {{token}} }`, and the api-key block's key/value/placement fields). Verify against current Bruno docs / a round-trip in the Bruno app as part of implementation, since a malformed block silently breaks the collection.

## Risks / Trade-offs

- [Guessing the api-key block's exact field names/placement] → Verify in the Bruno app before finalizing (task 4.x); the bearer/basic blocks are well-established, api-key is the one to confirm.
- [User expects a public endpoint to stay unauthenticated] → Out of scope here; `auth: inherit` applies uniformly. Documented as a known limitation; per-endpoint override is a future extension.
- [Interaction with the regeneration-mode change] → In Add-only mode, existing request files are not rewritten, so flipping auth mode won't update already-written requests; documented — a full regenerate (Overwrite) applies the auth change.

## Open Questions

None that affect the specs or task breakdown. The exact api-key `.bru` field spelling is an implementation detail resolved during coding (D4), not a spec-level question.
