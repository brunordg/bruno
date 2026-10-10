# Tasks

## 1. Stable request file naming

- [x] 1.1 Change `BrunoWriter.requestFilePath` to build the file name from `slug("<httpMethod> <path>")` (folder still the first path segment), dropping the `%02d-` selection-index prefix. Verify with a `BrunoWriterTest` that `GET /users/{id}` yields a fixed name.
- [x] 1.2 Add a `BrunoWriterTest` that generates the same endpoint under two different selections (full set vs. a subset) and asserts the resulting file path is identical in both.
- [x] 1.3 Add a `BrunoWriterTest` that two distinct endpoints in the same top-level folder produce two distinct files (no overwrite / collision).
- [x] 1.4 Compute `seq` for `meta {}` from the deterministic endpoint sort order of the written set rather than the selection index; verify a `BrunoWriterTest` that `seq` is present and stable for a given set.

## 2. Regeneration mode

- [x] 2.1 Add `enum RegenerationMode { OVERWRITE, ADD_ONLY }` and `regenerationMode: RegenerationMode = OVERWRITE` to `BrunoGeneratorState`; add a `BrunoGeneratorStateTest` asserting the default is `OVERWRITE`.
- [x] 2.2 In `writeCollection`, when mode is `ADD_ONLY`: skip writing a request file that already exists, and skip `pruneStaleRequestFiles` for request files. Keep current behavior for `OVERWRITE`. Verify with `BrunoWriterTest`: an existing (edited) request file is left byte-identical after an add-only run.
- [x] 2.3 `BrunoWriterTest`: add-only still creates a request file for a new endpoint that has none.
- [x] 2.4 `BrunoWriterTest`: add-only does not delete a request file whose endpoint is no longer selected.

## 3. Diff preview and settings UI

- [x] 3.1 Update `BrunoWriter.computeDiff` to accept/honor the regeneration mode: in `ADD_ONLY`, `removed` is empty and existing expected files are reported as `unchanged`. Verify with a `BrunoWriterTest` on the diff result.
- [x] 3.2 Expose the regeneration mode in `BrunoGeneratorConfigurable` (a combo box / radio). Manually verify in `runIde` that the choice persists via `bruno-generator.xml`.
- [x] 3.3 Ensure `EndpointSelectionDialog.refreshDiff` passes the configured mode into `computeDiff` so the preview matches the chosen behavior.

## 4. Docs and migration note

- [x] 4.1 Document in `README.md` the stable naming (one-time rename on first regeneration after upgrade) and the new regeneration mode with its two options. Run `bash ./gradlew test --no-daemon` and confirm the suite is green.
