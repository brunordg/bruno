# Design

## Context

See proposal.md — Why. Today `BrunoWriter.requestFilePath(requestsDir, endpoint, seq)` builds `requestsDir/<firstPathSegment>/%02d-<slug(handlerName)>.bru`, where `seq = index + 1` over the **selected** endpoints (`writeCollection` and `computeDiff` both pass the loop index). `pruneStaleRequestFiles` deletes every `.bru` not in the expected set, and `writeCollection` always rewrites each file. The scanner already returns endpoints `distinctBy { "$method $path" }` and sorted by path then method, so method+path is a unique, stable key per endpoint.

## Goals / Non-Goals

Goals:
- A deterministic file identity per endpoint, and a regeneration mode that can preserve existing files.

Non-Goals:
- Per-file partial merge (managed sections inside an edited request). Add-only is whole-file.
- Changing the on-disk `.bru` format or the folder grouping (still first path segment).

## Decisions

**D1 — File name = `slug("<method> <path>")`, folder unchanged.**
Drop the `%02d-` numeric prefix from the file name. Derive the name from the endpoint's method and path, e.g. `GET /users/{id}` → `get-users-id.bru`, reusing the existing `slug()` (which already lowercases, hyphenates, and strips non-alphanumerics; `{id}` becomes `id`). This is stable regardless of selection and unique because method+path is unique per endpoint.
- *Alternative considered*: keep `handlerName` in the name. Rejected — two handlers with the same method+path-segment but different full paths, or overloaded handler names, could collide; method+path is the true identity.

**D2 — `seq` computed from a deterministic sort, not the selection index.**
Keep `seq` in `meta {}` for Bruno ordering, but compute it from the endpoints' stable sort order (the order the scanner already produces) for the set being written, so it does not jump around arbitrarily. `seq` affects display order only, never file identity.

**D3 — Regeneration mode as a settings enum, applied in `writeCollection`.**
Add `regenerationMode: RegenerationMode = OVERWRITE` (enum `OVERWRITE`, `ADD_ONLY`) to `BrunoGeneratorState`. In `writeCollection`: in `ADD_ONLY`, skip writing a request file when it already exists, and skip `pruneStaleRequestFiles` for request files entirely. `collection.bru` and environment files remain regenerated from settings in both modes (they are derived from settings, not hand-edited request content).
- *Alternative considered*: make Add-only still prune removed endpoints. Rejected — pruning could delete a request the user added by hand, defeating the point of the mode.

**D4 — `computeDiff` takes the mode into account.**
In `ADD_ONLY`, compute `removed` as empty and treat existing expected files as `unchanged`, so the preview matches what will actually happen.

## Risks / Trade-offs

- [One-time churn: file names change on the first regeneration after upgrade] → Expected and acceptable; document it as a single migration. Users commit the rename once.
- [Residual slug collision for two distinct endpoints whose method+path slugify identically] → Extremely unlikely given method+path uniqueness and that slug preserves path structure; if it ever occurs, append a short deterministic disambiguator (e.g. a hash of the full method+path). Note as a follow-up, not needed for the common case.
- [Add-only can leave a stale request when an endpoint's signature changes] → Documented trade-off of the mode; Overwrite remains the default for users who want strict sync.

## Open Questions

None that affect the specs or task breakdown.
