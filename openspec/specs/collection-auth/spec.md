# collection-auth Specification

## Purpose
Define how the plugin configures authentication for a generated Bruno collection from settings — emitting a collection-level auth block, making requests inherit it, and provisioning per-environment credential variables — so users declare auth once instead of editing every request.

## Requirements

### Requirement: Authentication mode is configurable

The system SHALL provide an authentication mode setting with the values None, Bearer, Basic, and API Key, defaulting to None. None SHALL preserve the current output exactly.

#### Scenario: Default mode is None

- **WHEN** the plugin is used without configuring authentication
- **THEN** the authentication mode is None and the generated output is unchanged from current behavior (requests use `auth: none`, no auth block in the collection)

### Requirement: Collection declares the selected auth scheme

When the authentication mode is not None, the generated `collection.bru` SHALL include a collection-level auth block for the selected scheme, referencing Bruno variables rather than literal secrets:
- Bearer → a bearer auth block using a `{{token}}` variable
- Basic → a basic auth block using `{{username}}` and `{{password}}` variables
- API Key → an api-key auth block using a `{{apiKey}}` variable

#### Scenario: Bearer mode emits a bearer block

- **WHEN** the mode is Bearer
- **THEN** `collection.bru` contains a bearer auth block whose token references `{{token}}`

#### Scenario: Basic mode emits a basic block

- **WHEN** the mode is Basic
- **THEN** `collection.bru` contains a basic auth block referencing `{{username}}` and `{{password}}`

#### Scenario: API Key mode emits an api-key block

- **WHEN** the mode is API Key
- **THEN** `collection.bru` contains an api-key auth block referencing `{{apiKey}}`

### Requirement: Requests inherit collection auth

When the authentication mode is not None, each generated request SHALL declare `auth: inherit` so it uses the collection-level auth. When the mode is None, each request SHALL declare `auth: none`.

#### Scenario: Requests inherit when a mode is set

- **WHEN** the mode is Bearer, Basic, or API Key
- **THEN** every generated request declares `auth: inherit`

#### Scenario: Requests opt out when mode is None

- **WHEN** the mode is None
- **THEN** every generated request declares `auth: none`

### Requirement: Credential variables are provisioned per environment

When the authentication mode is not None, each generated environment file SHALL include the credential variable keys for the active mode (Bearer → `token`; Basic → `username`, `password`; API Key → `apiKey`) alongside the existing `baseUrl`, with empty values for the user to fill in. Secrets SHALL NOT be written as literal values by the generator.

#### Scenario: Environment gains credential keys for the mode

- **WHEN** the mode is Bearer and environments `dev` and `prod` are configured
- **THEN** each environment file contains a `token` variable (empty) in addition to `baseUrl`

#### Scenario: None mode leaves environments unchanged

- **WHEN** the mode is None
- **THEN** environment files contain only `baseUrl` (unchanged from current behavior)
