package com.codeteam.writer

import com.codeteam.model.BodyKind
import com.codeteam.model.Endpoint
import com.codeteam.model.RequestBodyField
import com.codeteam.model.RequestParameter
import com.codeteam.settings.AuthMode
import com.codeteam.settings.BrunoEnvironment
import com.codeteam.settings.BrunoGeneratorState
import com.codeteam.settings.FolderStrategy
import com.codeteam.settings.RegenerationMode
import com.codeteam.settings.resolvedDefaultBaseUrl
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

data class CollectionDiff(
    val added: List<String>,
    val removed: List<String>,
    val unchanged: List<String>
)

class BrunoWriter {

    fun computeDiff(
        projectRoot: Path,
        endpoints: List<Endpoint>,
        settings: BrunoGeneratorState,
        outputRoot: Path = projectRoot,
        folderStrategy: FolderStrategy = settings.folderStrategy
    ): CollectionDiff {
        val brunoRoot = outputRoot.resolve(resolveCollectionName(projectRoot, settings))
        val requestsDir = brunoRoot.resolve("requests")
        val environmentsDir = brunoRoot.resolve("environments")

        val expectedRequests = endpoints.map { requestFilePath(requestsDir, it, folderStrategy) }
        val expectedEnvironments = settings.environments.map { environmentsDir.resolve("${slug(it.name)}.bru") }
        val expected = (expectedRequests + expectedEnvironments).map { it.normalize() }.toSet()

        val existing = (existingBruFiles(requestsDir) + existingBruFiles(environmentsDir))
            .map { it.normalize() }.toSet()

        fun rel(path: Path) = brunoRoot.relativize(path).toString()

        // Add-only never deletes, so nothing is reported as removed and every existing
        // file stays as-is; only genuinely missing files are added.
        if (settings.regenerationMode == RegenerationMode.ADD_ONLY) {
            return CollectionDiff(
                added = (expected - existing).map(::rel).sorted(),
                removed = emptyList(),
                unchanged = existing.map(::rel).sorted()
            )
        }

        return CollectionDiff(
            added = (expected - existing).map(::rel).sorted(),
            removed = (existing - expected).map(::rel).sorted(),
            unchanged = (expected intersect existing).map(::rel).sorted()
        )
    }

    private fun existingBruFiles(dir: Path): Set<Path> {
        if (!Files.exists(dir)) return emptySet()
        Files.walk(dir).use { stream ->
            return stream.filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(".bru") }
                .toList()
                .toSet()
        }
    }

    fun writeCollection(
        projectRoot: Path,
        endpoints: List<Endpoint>,
        settings: BrunoGeneratorState,
        outputRoot: Path = projectRoot,
        folderStrategy: FolderStrategy = settings.folderStrategy
    ): Path {
        val collectionName = resolveCollectionName(projectRoot, settings)
        val brunoRoot = outputRoot.resolve(collectionName)
        val requestsDir = brunoRoot.resolve("requests")
        val environmentsDir = brunoRoot.resolve("environments")

        val addOnly = settings.regenerationMode == RegenerationMode.ADD_ONLY

        brunoRoot.createDirectories()
        requestsDir.createDirectories()

        brunoRoot.resolve("bruno.json").writeText(brunoJson(collectionName))
        brunoRoot.resolve("collection.bru").writeText(collectionFileContent(settings))

        writeEnvironments(environmentsDir, settings.environments, settings.authMode)

        // Deterministic ordering: stable file names come from method+path, and the
        // meta `seq` comes from this sort order, never from the selection index.
        val ordered = endpoints.sortedWith(compareBy<Endpoint> { it.path }.thenBy { it.httpMethod })
        ordered.forEachIndexed { index, endpoint ->
            val requestFile = requestFilePath(requestsDir, endpoint, folderStrategy)
            // Add-only preserves existing (possibly hand-edited) request files.
            if (addOnly && Files.exists(requestFile)) return@forEachIndexed
            requestFile.parent.createDirectories()
            requestFile.writeText(requestContent(endpoint, index + 1, settings.authMode))
        }

        // Add-only never deletes, so pruning is skipped entirely in that mode.
        if (!addOnly) {
            pruneStaleRequestFiles(requestsDir, ordered, folderStrategy)
            pruneStaleEnvironmentFiles(environmentsDir, settings.environments)
        }

        return brunoRoot
    }

    private fun resolveCollectionName(projectRoot: Path, settings: BrunoGeneratorState): String {
        val baseName = settings.collectionName.ifBlank {
            projectRoot.fileName?.toString().orEmpty().ifBlank { "collection" }
        }
        return composeCollectionName(baseName, settings.collectionNamePrefix, settings.collectionNameSuffix)
    }

    private fun brunoJson(collectionName: String): String {
        return """
            {
              "version": "1",
              "name": "$collectionName",
              "type": "collection"
            }
        """.trimIndent() + "\n"
    }

    private fun collectionFileContent(settings: BrunoGeneratorState): String {
        val vars = """
            vars:pre-request {
              baseUrl: ${settings.resolvedDefaultBaseUrl()}
            }
        """.trimIndent() + "\n"
        val auth = authBlock(settings.authMode)
        val withAuth = if (auth.isEmpty()) vars else vars + "\n" + auth
        return withAuth + "\n" + ExampleValues.DATE_SCRIPT
    }

    private fun authBlock(mode: AuthMode): String = when (mode) {
        AuthMode.NONE -> ""
        AuthMode.BEARER -> """
            auth {
              mode: bearer
            }
            auth:bearer {
              token: {{token}}
            }
        """.trimIndent() + "\n"
        AuthMode.BASIC -> """
            auth {
              mode: basic
            }
            auth:basic {
              username: {{username}}
              password: {{password}}
            }
        """.trimIndent() + "\n"
        AuthMode.API_KEY -> """
            auth {
              mode: apikey
            }
            auth:apikey {
              key: X-API-Key
              value: {{apiKey}}
              placement: header
            }
        """.trimIndent() + "\n"
    }

    private fun credentialKeys(mode: AuthMode): List<String> = when (mode) {
        AuthMode.NONE -> emptyList()
        AuthMode.BEARER -> listOf("token")
        AuthMode.BASIC -> listOf("username", "password")
        AuthMode.API_KEY -> listOf("apiKey")
    }

    private fun writeEnvironments(
        environmentsDir: Path,
        environments: List<BrunoEnvironment>,
        authMode: AuthMode
    ) {
        environmentsDir.createDirectories()
        val credentialLines = credentialKeys(authMode).map { "  $it:" }
        environments.forEach { environment ->
            val lines = listOf("  baseUrl: ${environment.baseUrl}") + credentialLines
            environmentsDir.resolve("${slug(environment.name)}.bru").writeText(
                "vars {\n" + lines.joinToString("\n") + "\n}\n"
            )
        }
    }

    private fun requestContent(endpoint: Endpoint, seq: Int, authMode: AuthMode): String {
        val verbLower = endpoint.httpMethod.lowercase()
        val name = endpoint.summary?.let(::collapseWhitespace) ?: "${endpoint.httpMethod} ${endpoint.path}"
        val urlPath = toBrunoUrlPath(endpoint.path)
        val queryString = buildQueryString(endpoint.queryParams)

        val content = buildString {
            appendLine("meta {")
            appendLine("  name: $name")
            appendLine("  type: http")
            appendLine("  seq: $seq")
            appendLine("}")
            appendLine()

            appendLine("$verbLower {")
            appendLine("  url: {{baseUrl}}$urlPath$queryString")
            when (endpoint.bodyKind) {
                BodyKind.JSON -> appendLine("  body: json")
                BodyKind.MULTIPART -> appendLine("  body: multipart-form")
                BodyKind.NONE -> {}
            }
            appendLine("  auth: ${if (authMode == AuthMode.NONE) "none" else "inherit"}")
            appendLine("}")

            if (endpoint.pathVariables.isNotEmpty()) {
                appendLine()
                appendLine("params:path {")
                endpoint.pathVariables.forEach { appendLine("  ${paramLine(it)}") }
                appendLine("}")
            }

            if (endpoint.queryParams.isNotEmpty()) {
                appendLine()
                appendLine("params:query {")
                endpoint.queryParams.forEach { appendLine("  ${paramLine(it)}") }
                appendLine("}")
            }

            appendLine()
            appendLine("headers {")
            appendLine("  Accept: application/json")
            if (endpoint.bodyKind == BodyKind.JSON) {
                appendLine("  Content-Type: application/json")
            }
            endpoint.headerParams.forEach { appendLine("  ${paramLine(it)}") }
            if (endpoint.cookieParams.isNotEmpty()) {
                appendLine("  ${cookieHeaderLine(endpoint.cookieParams)}")
            }
            appendLine("}")

            when (endpoint.bodyKind) {
                BodyKind.JSON -> {
                    appendLine()
                    appendLine("body:json {")
                    appendLine(jsonObject(endpoint))
                    appendLine("}")
                }
                BodyKind.MULTIPART -> {
                    appendLine()
                    appendLine("body:multipart-form {")
                    appendLine(multipartFormBlock(endpoint))
                    appendLine("}")
                }
                BodyKind.NONE -> {}
            }

            val docLines = buildList {
                endpoint.description?.let { add(collapseWhitespace(it)) }
                (endpoint.pathVariables + endpoint.queryParams + endpoint.headerParams + endpoint.cookieParams)
                    .forEach { param ->
                        param.description?.let { add("param '${param.name}': ${collapseWhitespace(it)}") }
                    }
                endpoint.requestBodyFields.flattenWithPath().forEach { (path, field) ->
                    field.description?.let { add("field '$path': ${collapseWhitespace(it)}") }
                }
                endpoint.requestBodyFields.flattenWithPath().forEach { (path, field) ->
                    field.constraints.pattern?.let { add("field '$path' must match pattern: $it") }
                }
            }
            if (docLines.isNotEmpty()) {
                appendLine()
                appendLine("docs {")
                docLines.forEach { appendLine("  $it") }
                appendLine("}")
            }
        }
        return content + "\n"
    }

    private fun collapseWhitespace(text: String): String =
        text.replace(Regex("\\s*\\n\\s*"), " ").trim()

    private fun paramLine(param: RequestParameter): String {
        val key = if (param.required) param.name else "~${param.name}"
        val value = param.defaultValue ?: fakeValue(param.name, param.type)
        return "$key: $value"
    }

    private fun cookieHeaderLine(cookies: List<RequestParameter>): String {
        val pairs = cookies.joinToString("; ") { "${it.name}=${it.defaultValue ?: fakeValue(it.name, it.type)}" }
        return "Cookie: $pairs"
    }

    private fun buildQueryString(queryParams: List<RequestParameter>): String {
        val enabled = queryParams.filter { it.required }
        if (enabled.isEmpty()) return ""
        return "?" + enabled.joinToString("&") { "${it.name}=${it.defaultValue ?: fakeValue(it.name, it.type)}" }
    }

    internal fun toBrunoUrlPath(path: String): String =
        path.replace(Regex("""\{(\w+)(:[^}]*)?}"""), ":$1")

    private fun jsonObject(endpoint: Endpoint): String {
        val fields = endpoint.requestBodyFields.filter { it.name.isNotBlank() }
        if (fields.isEmpty()) {
            val fallback = if (usesDynamicFakes(endpoint.httpMethod)) "{{\$randomUUID}}" else "{{usuarioId}}"
            return "  {\n    \"id\": \"$fallback\"\n  }"
        }
        return "  " + renderObject(fields, usesDynamicFakes(endpoint.httpMethod), level = 2)
    }

    // level = indent depth (2-space units) of this object's own entries; its braces sit at level-1,
    // with the opening brace emitted without a leading indent since callers place it right after "key": .
    private fun renderObject(fields: List<RequestBodyField>, dynamic: Boolean, level: Int): String {
        val closeIndent = "  ".repeat(level - 1)
        val entryIndent = "  ".repeat(level)
        val entries = fields.joinToString(",\n") { field ->
            "$entryIndent\"${field.name}\": ${renderFieldValue(field, dynamic, level + 1)}"
        }
        return "{\n$entries\n$closeIndent}"
    }

    private fun renderFieldValue(field: RequestBodyField, dynamic: Boolean, level: Int): String {
        return when {
            field.isCollection -> renderArrayValue(field, dynamic, level)
            field.nestedFields.isNotEmpty() -> renderObject(field.nestedFields, dynamic, level)
            else -> renderScalarValue(field, dynamic)
        }
    }

    // level = indent depth of the array's single representative element; the brackets sit at level-1.
    private fun renderArrayValue(field: RequestBodyField, dynamic: Boolean, level: Int): String {
        val closeIndent = "  ".repeat(level - 1)
        val elementIndent = "  ".repeat(level)
        val element = if (field.nestedFields.isNotEmpty()) {
            renderObject(field.nestedFields, dynamic, level + 1)
        } else {
            renderScalarValue(field, dynamic)
        }
        return "[\n$elementIndent$element\n$closeIndent]"
    }

    private fun renderScalarValue(field: RequestBodyField, dynamic: Boolean): String {
        // Precedence: an explicit @Schema(example) wins over constraint-derived and
        // name/type heuristic values.
        val value = when {
            field.example != null -> field.example
            dynamic -> dynamicFakeForField(field)
            else -> "{{${field.name}}}"
        }
        return if (quoteJsonValue(field)) "\"$value\"" else value
    }

    private fun List<RequestBodyField>.flattenWithPath(prefix: String = ""): List<Pair<String, RequestBodyField>> =
        flatMap { field ->
            val path = if (prefix.isBlank()) field.name else "$prefix.${field.name}"
            listOf(path to field) + field.nestedFields.flattenWithPath(path)
        }

    private fun multipartFormBlock(endpoint: Endpoint): String {
        if (endpoint.multipartParts.isEmpty()) return "  file: @file()"
        return endpoint.multipartParts.joinToString("\n") { part ->
            val key = if (part.required) part.name else "~${part.name}"
            val value = if (part.isFile) "@file()" else fakeValue(part.name, part.type)
            "  $key: $value"
        }
    }

    private fun quoteJsonValue(field: RequestBodyField): Boolean {
        if (field.isEnum) return true
        val type = field.type.lowercase()
        return !(isNumericTypeName(type) || type == "boolean" || type == "bool")
    }

    private fun usesDynamicFakes(httpMethod: String): Boolean {
        return httpMethod.uppercase() in setOf("POST", "PUT", "PATCH")
    }

    private fun dynamicFakeForField(field: RequestBodyField): String {
        if (field.isEnum && field.enumConstants.isNotEmpty()) {
            return field.enumConstants.first()
        }
        val constraints = field.constraints
        val type = field.type.lowercase()
        if (constraints.email) {
            return "{{\$randomEmail}}"
        }
        if (isNumericTypeName(type) && (constraints.min != null || constraints.max != null)) {
            val literal = boundedNumericLiteral(constraints.min, constraints.max)
            return if (isDoubleTypeName(type)) "$literal.0" else literal.toString()
        }
        return ExampleValues.valueFor(field.name, field.type, constraints)
    }

    private fun boundedNumericLiteral(min: Long?, max: Long?): Long = when {
        min != null && max != null -> (min + max) / 2
        min != null -> min
        max != null -> max
        else -> 0L
    }

    private fun isNumericTypeName(type: String): Boolean = type.lowercase().let {
        it.contains("int") || it.contains("long") || it.contains("double") ||
            it.contains("float") || it.contains("bigdecimal") || it.contains("number") ||
            it.contains("short") || it.contains("byte")
    }

    private fun isDoubleTypeName(type: String): Boolean = type.lowercase().let {
        it.contains("bigdecimal") || it.contains("double") || it.contains("float")
    }

    internal fun fakeValue(name: String, type: String): String = ExampleValues.valueFor(name, type)

    internal fun slug(raw: String): String {
        return raw
            .replace(Regex("([a-z])([A-Z]+)"), "$1-$2")
            .lowercase()
            .replace(Regex("[^a-z0-9-]+"), "-")
            .trim('-')
            .ifBlank { "request" }
    }

    // Stable, selection-independent file name derived from the endpoint's method and
    // path. method+path is unique per endpoint, so distinct endpoints never collide.
    // Only the containing folder depends on the chosen strategy.
    private fun requestFilePath(requestsDir: Path, endpoint: Endpoint, strategy: FolderStrategy): Path {
        val fileName = "${slug("${endpoint.httpMethod} ${endpoint.path}")}.bru"
        return requestFolder(requestsDir, endpoint, strategy).resolve(fileName)
    }

    private fun requestFolder(requestsDir: Path, endpoint: Endpoint, strategy: FolderStrategy): Path =
        when (strategy) {
            FolderStrategy.FLAT -> requestsDir
            FolderStrategy.BY_CONTROLLER ->
                requestsDir.resolve(slug(endpoint.controllerName).ifBlank { "root" })
            FolderStrategy.BY_URL_PATH -> {
                val segments = endpoint.path.trim('/').split('/')
                    .map { slug(it) }
                    .filter { it.isNotBlank() }
                segments.fold(requestsDir) { dir, segment -> dir.resolve(segment) }
            }
        }

    private fun pruneStaleRequestFiles(
        requestsDir: Path,
        endpoints: List<Endpoint>,
        strategy: FolderStrategy
    ) {
        if (!Files.exists(requestsDir)) return

        val expected = endpoints.map { endpoint ->
            requestFilePath(requestsDir, endpoint, strategy).normalize().toString()
        }.toSet()

        Files.walk(requestsDir).use { stream ->
            stream.filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(".bru") }
                .forEach { file ->
                    val normalized = file.normalize().toString()
                    if (normalized !in expected) {
                        Files.deleteIfExists(file)
                    }
                }
        }
    }

    private fun pruneStaleEnvironmentFiles(environmentsDir: Path, environments: List<BrunoEnvironment>) {
        if (!Files.exists(environmentsDir)) return

        val expected = environments.map { "${slug(it.name)}.bru" }.toSet()

        Files.walk(environmentsDir).use { stream ->
            stream.filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(".bru") }
                .forEach { file ->
                    if (file.fileName.toString() !in expected) {
                        Files.deleteIfExists(file)
                    }
                }
        }
    }
}

internal fun composeCollectionName(baseName: String, prefix: String, suffix: String): String =
    "${prefix.trim()}$baseName${suffix.trim()}"
