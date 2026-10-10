# collection-regeneration Specification

## Purpose
Define how the plugin names request files and reconciles an existing Bruno collection across repeated regenerations — which files it creates, overwrites, preserves, and removes — so that regeneration stays idempotent without destroying hand edits.

## Requirements

### Requirement: Request file names are stable and selection-independent

The system SHALL derive each request's file name deterministically from the endpoint's HTTP method and path. The file name SHALL NOT depend on which endpoints are selected, nor on the number or ordering of selected endpoints. Regenerating the same endpoint SHALL always target the same file path.

#### Scenario: Same endpoint, different selection

- **WHEN** an endpoint is generated as part of a full selection, and later generated again as part of a smaller selection
- **THEN** its request file has the same path in both runs

#### Scenario: Distinct endpoints do not collide

- **WHEN** two different endpoints map (by method and path) to request files in the same folder
- **THEN** they are written to two different files, and neither overwrites the other

#### Scenario: Ordering metadata remains present

- **WHEN** a request file is generated
- **THEN** its `meta {}` block still contains a `seq` value for ordering, computed deterministically rather than from the selection index

### Requirement: Regeneration mode controls overwrite and removal

The system SHALL provide a configurable regeneration mode with two values, defaulting to Overwrite.

In **Overwrite** mode, regeneration SHALL rewrite every request file for the selected endpoints and SHALL remove request files that no longer correspond to a selected endpoint (current behavior).

In **Add-only** mode, regeneration SHALL create request files that do not yet exist, SHALL NOT overwrite a request file that already exists, and SHALL NOT remove any request file.

#### Scenario: Overwrite mode rewrites and prunes

- **WHEN** the mode is Overwrite and an endpoint is removed from the code or deselected
- **THEN** its request file is deleted and existing request files are rewritten from the current code

#### Scenario: Add-only preserves an edited request

- **WHEN** the mode is Add-only and a request file that already exists was edited by hand
- **THEN** regeneration leaves that file untouched

#### Scenario: Add-only still creates new requests

- **WHEN** the mode is Add-only and a newly added endpoint has no request file yet
- **THEN** regeneration creates the new request file

#### Scenario: Add-only does not delete manual files

- **WHEN** the mode is Add-only and a request file exists for an endpoint that is no longer selected, or was added manually by the user
- **THEN** regeneration does not delete it

### Requirement: Diff preview reflects the regeneration mode

The pre-generation diff preview SHALL reflect the active regeneration mode: in Add-only mode it SHALL NOT list existing files as removed, consistent with the files that regeneration will actually write and keep.

#### Scenario: Add-only diff shows no removals

- **WHEN** the mode is Add-only and the diff preview is shown
- **THEN** the "removed" section is empty and existing files appear as unchanged
