# request-body-generation Specification

## Purpose
Define how the plugin derives the fields, property names, and placeholder values of a request's JSON body from the DTO bound to a Spring `@RequestBody` parameter, so that the generated body matches the API's real JSON contract.

## Requirements

### Requirement: JSON property names honor @JsonProperty

The generated request body SHALL use the effective JSON property name for each field. When a field, its getter, or the matching constructor parameter is annotated with Jackson `@JsonProperty` declaring a non-blank `value`, the system SHALL use that declared name as the JSON key and in the corresponding placeholder value. When no such annotation is present, the system SHALL use the Java/Kotlin field name.

#### Scenario: Field annotated with @JsonProperty

- **WHEN** a request body DTO has a field `userName` annotated `@JsonProperty("user_name")`
- **THEN** the generated JSON body contains the key `"user_name"` and not `"userName"`

#### Scenario: Annotation on the getter or constructor parameter

- **WHEN** the `@JsonProperty("user_name")` annotation is declared on the field's getter or on the constructor parameter (e.g. a Java `record` component or a Kotlin `val` constructor property) rather than on the field itself
- **THEN** the generated JSON body still uses the key `"user_name"`

#### Scenario: No Jackson annotation present

- **WHEN** a field `userName` has no `@JsonProperty` annotation
- **THEN** the generated JSON body uses the key `"userName"` (unchanged from current behavior)

### Requirement: Ignored fields are omitted from the body

The system SHALL omit from the generated request body any field that Jackson excludes from deserialization: fields annotated with `@JsonIgnore`, and fields whose `@JsonProperty` declares `access = JsonProperty.Access.READ_ONLY`.

#### Scenario: Field annotated with @JsonIgnore

- **WHEN** a request body DTO has a field annotated `@JsonIgnore`
- **THEN** that field does not appear in the generated JSON body

#### Scenario: Read-only field

- **WHEN** a request body DTO has a field annotated `@JsonProperty(access = JsonProperty.Access.READ_ONLY)`
- **THEN** that field does not appear in the generated JSON body

#### Scenario: All fields ignored

- **WHEN** every field of a request body DTO is excluded by `@JsonIgnore` or read-only access
- **THEN** the generated body falls back to the existing empty-body placeholder behavior, with no stray keys from ignored fields

### Requirement: Jackson resolution applies to nested fields

The resolution of property names and the omission of ignored fields SHALL apply at every nesting level of the request body, consistent with how the plugin already expands nested objects and collections.

#### Scenario: Nested object with renamed and ignored fields

- **WHEN** a request body contains a nested object whose fields include a `@JsonProperty("zip_code")` rename and a `@JsonIgnore` field
- **THEN** the nested object in the generated JSON uses `"zip_code"` and omits the ignored field
