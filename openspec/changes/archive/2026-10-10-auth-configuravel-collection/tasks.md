# Tasks

## 1. Settings

- [x] 1.1 Add `enum AuthMode { NONE, BEARER, BASIC, API_KEY }` and `authMode: AuthMode = NONE` to `BrunoGeneratorState`. Add a `BrunoGeneratorStateTest` asserting the default is `NONE`.
- [x] 1.2 Expose the auth mode in `BrunoGeneratorConfigurable` (combo/radio) and persist it via `bruno-generator.xml`. Manually verify persistence in `runIde`.

## 2. Confirm Bruno auth grammar

- [x] 2.1 Confirm the exact `.bru` auth block syntax for bearer, basic, and api-key (field names, `mode`, placement) against current Bruno docs or a round-trip in the Bruno app. Record the confirmed snippets to use as the writer's output (verification: the Bruno app opens the generated collection with the auth configured, no parse error).

## 3. Writer emits auth

- [x] 3.1 When `authMode != NONE`, append the collection-level auth block to `collection.bru` for the selected scheme, referencing `{{token}}` / `{{username}}`+`{{password}}` / `{{apiKey}}`. Verify with `BrunoWriterTest` per mode.
- [x] 3.2 In `requestContent`, emit `auth: inherit` when `authMode != NONE`, else `auth: none`. Verify with `BrunoWriterTest` that requests inherit under each non-None mode and stay `none` under None.
- [x] 3.3 In `writeEnvironments`, add the active mode's credential keys (empty values) alongside `baseUrl`; emit only `baseUrl` when mode is None. Verify with `BrunoWriterTest` for Bearer (adds `token`) and None (unchanged).
- [x] 3.4 Regression `BrunoWriterTest`: with default mode None, `collection.bru`, requests, and environment files are byte-identical to pre-change output.

## 4. Docs

- [x] 4.1 Document the authentication setting in `README.md` (modes, variables to fill per environment, the known limitation that per-endpoint exceptions and Add-only-mode interaction are out of scope). Run `bash ./gradlew test --no-daemon` and confirm green.
