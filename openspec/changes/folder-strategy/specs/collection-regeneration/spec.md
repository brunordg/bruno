# Spec Delta

## MODIFIED Requirements

### Requirement: Request file names are stable and selection-independent

The system SHALL derive each request's file name deterministically from the endpoint's HTTP method and path. The file name SHALL NOT depend on which endpoints are selected, nor on the number or ordering of selected endpoints. The folder that contains the file SHALL be determined by the active folder organization strategy. Within a given strategy, regenerating the same endpoint SHALL always target the same file path.

#### Scenario: Same endpoint, different selection

- **WHEN** an endpoint is generated as part of a full selection, and later generated again as part of a smaller selection, under the same strategy
- **THEN** its request file has the same path in both runs

#### Scenario: Distinct endpoints do not collide

- **WHEN** two different endpoints map (by method and path) to request files in the same folder
- **THEN** they are written to two different files, and neither overwrites the other

#### Scenario: Ordering metadata remains present

- **WHEN** a request file is generated
- **THEN** its `meta {}` block still contains a `seq` value for ordering, computed deterministically rather than from the selection index

## ADDED Requirements

### Requirement: Folder organization strategy

The system SHALL support three folder organization strategies that determine the directory a request file is placed in under `requests/`, while the file name itself remains `<method>-<path>` derived (unchanged across strategies):

- **By URL path**: the request is placed in a nested folder mirroring the endpoint's path segments; path-variable segments contribute a folder named after the variable with its braces removed (e.g. `{id}` becomes `id`).
- **By controller**: the request is placed in a single folder named after the controller class that declares the endpoint.
- **Flat**: the request is placed directly under `requests/` with no subfolder.

#### Scenario: By URL path groups a resource together

- **WHEN** the strategy is By URL path and the endpoints `GET /api/widgets` and `POST /api/widgets` are generated
- **THEN** both request files are placed in the same `requests/api/widgets/` folder

#### Scenario: By URL path nests a path variable

- **WHEN** the strategy is By URL path and the endpoint `GET /api/widgets/{id}` is generated
- **THEN** its request file is placed under `requests/api/widgets/id/`

#### Scenario: By controller groups by declaring class

- **WHEN** the strategy is By controller and two endpoints declared by the same controller class are generated
- **THEN** both request files are placed in the same folder named after that controller

#### Scenario: Flat places everything at the root

- **WHEN** the strategy is Flat and any endpoint is generated
- **THEN** its request file is placed directly under `requests/` with no subfolder

### Requirement: Folder strategy has a configurable default

The system SHALL expose the folder organization strategy as a setting with the three values By URL path, By controller, and Flat, defaulting to By URL path.

#### Scenario: Default strategy

- **WHEN** the plugin is used without changing the folder strategy setting
- **THEN** the strategy is By URL path

### Requirement: Folder strategy is chosen at generation time with live preview

The generation dialog SHALL present a folder strategy selector, initialized from the configured default. The user MAY choose a different strategy for the current generation; that per-generation choice SHALL determine the folders written for that run and SHALL NOT change the saved default. The pre-generation diff preview SHALL reflect the currently selected strategy, updating when the selection changes.

#### Scenario: Preview updates when the strategy changes

- **WHEN** the user changes the selected strategy in the generation dialog
- **THEN** the diff preview updates to show the file paths that the newly selected strategy would produce

#### Scenario: Per-generation choice does not change the default

- **WHEN** the user overrides the strategy in the generation dialog and generates
- **THEN** the collection is written using the chosen strategy and the saved default strategy is unchanged

#### Scenario: Generation uses the selected strategy

- **WHEN** the user selects By controller in the dialog and generates
- **THEN** the written request files are organized into per-controller folders
