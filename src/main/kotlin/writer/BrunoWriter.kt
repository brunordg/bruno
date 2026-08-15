package com.codeteam.writer

import com.codeteam.model.Endpoint
import com.codeteam.model.RequestBodyField
import com.codeteam.model.RequestParameter
import com.codeteam.settings.BrunoEnvironment
import com.codeteam.settings.BrunoGeneratorState
import com.codeteam.settings.resolvedDefaultBaseUrl
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.use

class BrunoWriter {

    fun writeCollection(
        projectRoot: Path,
        endpoints: List<Endpoint>,
        settings: BrunoGeneratorState,
        outputRoot: Path = projectRoot
    ): Path {
        val collectionName = collectionName(projectRoot)
        val brunoRoot = outputRoot.resolve(collectionName)
        val requestsDir = brunoRoot.resolve("requests")
        val environmentsDir = brunoRoot.resolve("environments")

        brunoRoot.createDirectories()
        requestsDir.createDirectories()

        brunoRoot.resolve("bruno.json").writeText(brunoJson(collectionName))
        brunoRoot.resolve("collection.bru").writeText(collectionVariables(settings.resolvedDefaultBaseUrl()))

        writeEnvironments(environmentsDir, settings.environments)

        endpoints.forEachIndexed { index, endpoint ->
            val folderName = endpoint.path.trim('/').split('/').firstOrNull().orEmpty().ifBlank { "root" }
            val endpointDir = requestsDir.resolve(folderName)
            endpointDir.createDirectories()
            val fileName = "%02d-%s.bru".format(index + 1, slug(endpoint.handlerName))
            endpointDir.resolve(fileName).writeText(requestContent(endpoint, index + 1))
        }

        pruneStaleRequestFiles(requestsDir, endpoints)
        pruneStaleEnvironmentFiles(environmentsDir, settings.environments)

        return brunoRoot
    }

    private fun collectionName(projectRoot: Path): String =
        projectRoot.fileName?.toString().orEmpty().ifBlank { "collection" }

    private fun brunoJson(collectionName: String): String {
        return """
            {
              "version": "1",
              "name": "$collectionName",
              "type": "collection"
            }
        """.trimIndent() + "\n"
    }

    private fun collectionVariables(defaultBaseUrl: String): String {
        return """
            vars:pre-request {
              baseUrl: $defaultBaseUrl
            }
        """.trimIndent() + "\n"
    }

    private fun writeEnvironments(environmentsDir: Path, environments: List<BrunoEnvironment>) {
        environmentsDir.createDirectories()
        environments.forEach { environment ->
            environmentsDir.resolve("${slug(environment.name)}.bru").writeText(
                "vars {\n  baseUrl: ${environment.baseUrl}\n}\n"
            )
        }
    }

    private fun requestContent(endpoint: Endpoint, seq: Int): String {
        val verbLower = endpoint.httpMethod.lowercase()
        val name = "${endpoint.httpMethod} ${endpoint.path}"
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
            if (endpoint.hasRequestBody) {
                appendLine("  body: json")
            }
            appendLine("  auth: none")
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
            if (endpoint.hasRequestBody) {
                appendLine("  Content-Type: application/json")
            }
            endpoint.headerParams.forEach { appendLine("  ${paramLine(it)}") }
            if (endpoint.cookieParams.isNotEmpty()) {
                appendLine("  ${cookieHeaderLine(endpoint.cookieParams)}")
            }
            appendLine("}")

            if (endpoint.hasRequestBody) {
                appendLine()
                appendLine("body:json {")
                appendLine(jsonObject(endpoint))
                appendLine("}")
            }

            val patternNotes = endpoint.requestBodyFields.mapNotNull { field ->
                field.constraints.pattern?.let { "field '${field.name}' must match pattern: $it" }
            }
            if (patternNotes.isNotEmpty()) {
                appendLine()
                appendLine("docs {")
                patternNotes.forEach { appendLine("  $it") }
                appendLine("}")
            }
        }
        return content + "\n"
    }

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

        val dynamic = usesDynamicFakes(endpoint.httpMethod)
        val entries = fields.joinToString(",\n") { field ->
            val value = if (dynamic) dynamicFakeForField(field) else "{{${field.name}}}"
            val valueText = if (quoteJsonValue(field)) "\"$value\"" else value
            "    \"${field.name}\": $valueText"
        }
        return "  {\n$entries\n  }"
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
        return fakeValue(field.name, field.type)
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

    internal fun fakeValue(name: String, type: String): String {
        val normalized = name.lowercase()
        val lowerType = type.lowercase()
        val isStringType = lowerType.contains("string")
        val isDate = lowerType.contains("date") ||
            lowerType.contains("time") ||
            lowerType.contains("timestamp") ||
            lowerType.contains("localdatetime") ||
            lowerType.contains("offsetdatetime")
        val isNumericType = isNumericTypeName(lowerType)
        val isDouble = isDoubleTypeName(lowerType)

        return when {
            normalized == "id" || normalized.endsWith("id") -> {
                when {
                    isNumericType -> "{{\$randomInt}}"
                    isStringType -> "{{\$randomUUID}}"
                    else -> "{{\$randomUUID}}"
                }
            }
            "email" in normalized -> "{{\$randomEmail}}"
            "first" in normalized && "name" in normalized -> "{{\$randomFirstName}}"
            "last" in normalized && "name" in normalized -> "{{\$randomLastName}}"
            "phone" in normalized || "mobile" in normalized || "cel" in normalized || "celular" in normalized || "telefone" in normalized -> "{{\$randomPhoneNumber}}"
            "zip" in normalized || "postal" in normalized || "cep" in normalized -> "{{\$randomInt}}"
            "date" in normalized || "time" in normalized || "createdAt" in normalized || "updatedAt" in normalized -> "{{\$isoTimestamp}}"
            "amount" in normalized || "price" in normalized || "total" in normalized -> "{{\$randomInt}}"
            "street" in normalized || "rua" in normalized -> "{{\$randomStreetAddress}}"
            "city" in normalized || "cidade" in normalized -> "{{\$randomCity}}"
            "country" in normalized || "pais" in normalized -> "{{\$randomCountry}}"
            "description" in normalized || "descricao" in normalized -> "{{\$randomProduct}}"
            isDouble -> "{{\$randomPrice}}"
            isDate -> "{{\$isoTimestamp}}"
            isStringType -> "{{\$randomLoremWord}}"
            isNumericType -> "{{\$randomInt}}"
            else -> "{{$name}}"
        }
    }

    internal fun slug(raw: String): String {
        return raw
            .replace(Regex("([a-z])([A-Z]+)"), "$1-$2")
            .lowercase()
            .replace(Regex("[^a-z0-9-]+"), "-")
            .trim('-')
            .ifBlank { "request" }
    }

    private fun pruneStaleRequestFiles(requestsDir: Path, endpoints: List<Endpoint>) {
        if (!Files.exists(requestsDir)) return

        val expected = endpoints.mapIndexed { index, endpoint ->
            val folderName = endpoint.path.trim('/').split('/').firstOrNull().orEmpty().ifBlank { "root" }
            requestsDir.resolve(folderName).resolve("%02d-%s.bru".format(index + 1, slug(endpoint.handlerName)))
                .normalize()
                .toString()
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
