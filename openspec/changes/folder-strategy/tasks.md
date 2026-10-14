# Tasks

## 1. Settings and model

- [x] 1.1 Add `enum FolderStrategy { BY_URL_PATH, BY_CONTROLLER, FLAT }` and `folderStrategy: FolderStrategy = BY_URL_PATH` to `BrunoGeneratorState`. Add a `BrunoGeneratorStateTest` asserting the default is `BY_URL_PATH`.
- [x] 1.2 Add `controllerName: String = ""` to `Endpoint`; in `ControllerScanner.extractEndpoints`, pass `psiClass.name.orEmpty()`. Verify in `ControllerScannerTest` that a scanned endpoint carries its controller's simple name.

## 2. Writer: strategy-aware folders

- [x] 2.1 Change `requestFilePath` to `requestFilePath(requestsDir, endpoint, strategy)`: folder by strategy (BY_URL_PATH = path segments each `slug`-ged; BY_CONTROLLER = `slug(controllerName)` or `root` when blank; FLAT = no subfolder), file name unchanged (`slug("<method> <path>")`). Keep it private.
- [x] 2.2 Thread `folderStrategy` as an explicit parameter through `writeCollection(...)` and `computeDiff(...)` and their internal uses of `requestFilePath`/`pruneStaleRequestFiles`. Do not read the strategy from `settings` inside the writer.
- [x] 2.3 `BrunoWriterCollectionTest`: BY_URL_PATH places `GET /api/widgets` and `POST /api/widgets` both under `requests/api/widgets/`, and `GET /api/widgets/{id}` under `requests/api/widgets/id/`.
- [x] 2.4 `BrunoWriterCollectionTest`: BY_CONTROLLER places endpoints from the same `controllerName` in one folder named after it; FLAT places files directly under `requests/`.
- [x] 2.5 `BrunoWriterCollectionTest`: same endpoint under a full selection and a subset (same strategy) yields the identical file path (stability preserved per strategy).

## 3. Dialog and action

- [x] 3.1 Add a folder-strategy `ComboBox<FolderStrategy>` to `EndpointSelectionDialog` (north region), initialized from `settings.folderStrategy`; expose `selectedFolderStrategy()`. Wire an item listener to call `refreshDiff()`.
- [x] 3.2 Update `refreshDiff` to pass `selectedFolderStrategy()` into `computeDiff` so the preview reflects the selected strategy and updates on change.
- [x] 3.3 In `GenerateBrunoCollectionAction`, pass `dialog.selectedFolderStrategy()` into `writeCollection`; confirm the saved `settings.folderStrategy` default is not overwritten by the per-run choice.

## 4. Settings UI

- [x] 4.1 Add a folder-strategy combo to `BrunoGeneratorConfigurable` (default selector), persisted via `bruno-generator.xml`, following the existing regeneration/auth combo pattern. Manually verify persistence in `runIde`.

## 5. Docs and verification

- [x] 5.1 Document the folder strategies and the generation-time selector in `README.md` (EN/PT), noting the one-time rename on first regeneration after upgrade.
- [x] 5.2 Run `bash ./gradlew clean test --no-build-cache` and confirm the full suite (including prior regression tests) is green.
