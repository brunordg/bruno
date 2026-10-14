# Design

## Context

See proposal.md — Why. Today `BrunoWriter.requestFilePath(requestsDir, endpoint)` builds `requestsDir/<first-path-segment>/<slug("<method> <path>")>.bru`, and `writeCollection`/`computeDiff` read every option (regeneration mode, auth) from the `BrunoGeneratorState`. The `EndpointSelectionDialog` already shows a live diff via `refreshDiff()` calling `computeDiff`, and `GenerateBrunoCollectionAction` orchestrates scan → dialog → write. The scanner's `extractEndpoints(psiClass)` has the declaring controller class but does not carry its name onto `Endpoint`.

## Goals / Non-Goals

Goals:
- Three folder strategies, a configurable default, and a per-generation override chosen in the dialog with a live preview that does not leak into saved state.

Non-Goals:
- Moving regeneration mode / auth into the dialog (stay in Settings).
- A fully nested selection tree mirroring the URL hierarchy (diff preview already reflects the strategy; tree grouping stays best-effort).
- Changing the file name per strategy — it stays `slug("<method> <path>")` everywhere.

## Decisions

**D1 — `folderStrategy` is an explicit parameter of `computeDiff`/`writeCollection`, not read from state.**
The dialog's live preview must reflect the user's current combobox choice without mutating `BrunoGeneratorState` (a cancel must leave the saved default untouched). So `computeDiff` and `writeCollection` take `folderStrategy: FolderStrategy` as an explicit argument. `BrunoGeneratorState.folderStrategy` is only the persisted default that seeds the dialog.
- *Alternative considered*: keep reading from `settings` and have the dialog mutate a settings copy. Rejected — mutation risk and a clone is clumsier than one parameter.

**D2 — Folder derivation is the only strategy-dependent part of the path.**
`requestFilePath` becomes `requestFilePath(requestsDir, endpoint, strategy)`; the file name stays `slug("<method> <path>")`. Folder per strategy:
- `BY_URL_PATH`: join `endpoint.path` segments, each run through `slug` (which drops `{}`), under `requestsDir`.
- `BY_CONTROLLER`: `slug(endpoint.controllerName)`; fall back to `"root"` when blank.
- `FLAT`: `requestsDir` itself (no subfolder).

**D3 — `Endpoint` carries the controller simple name.**
Add `controllerName: String = ""` to `Endpoint`; `extractEndpoints` passes `psiClass.name`. Only `BY_CONTROLLER` reads it, so the default is harmless for the other strategies and keeps existing `Endpoint(...)` construction in tests valid.

**D4 — Dialog seeds from the default, overrides per-run.**
`EndpointSelectionDialog` adds a combobox (initialized to `settings.folderStrategy`) in a north region; its `ItemListener` calls `refreshDiff()`, and `refreshDiff` passes `selectedFolderStrategy()` into `computeDiff`. `GenerateBrunoCollectionAction` reads `dialog.selectedFolderStrategy()` and passes it to `writeCollection`. The saved default is not rewritten by the override.

**D5 — `BY_URL_PATH` is the default.**
It directly fixes the "everything in one folder" problem and matches Bruno's native nested layout.

## Risks / Trade-offs

- [Switching strategy reorganizes files → one-time git churn] → Expected and documented, same as the stable-naming change; Add-only mode softens overwrite but the path still changes.
- [Deep URLs produce deep folders under BY_URL_PATH] → Accepted; it mirrors the API shape, which is the point.
- [Two controllers whose names slugify identically collide under BY_CONTROLLER] → Extremely unlikely; if it ever happens, requests from both simply share a folder and still have distinct file names (method+path), so nothing is overwritten.
- [Redundant file name under BY_URL_PATH (`api/widgets/get-api-widgets.bru`)] → Deliberate trade-off chosen for a single consistent naming rule and to preserve the stability guarantee without strategy-specific naming logic.

## Open Questions

None that affect the specs or task breakdown.
