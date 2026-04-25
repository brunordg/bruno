package com.codeteam.writer

import com.codeteam.model.Endpoint
import com.codeteam.model.RequestBodyField
import fleet.codepoints.isDoubleWidthCharacter
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.use

class BrunoWriter {
    private val defaultBaseUrl = "http://localhost:8080"

    fun writeCollection(projectRoot: Path, endpoints: List<Endpoint>) {
        val brunoRoot = projectRoot.resolve("bruno")
        val requestsDir = brunoRoot.resolve("requests")

        brunoRoot.createDirectories()
        requestsDir.createDirectories()

        brunoRoot.resolve("bruno.json").writeText(brunoJson(projectRoot))
        brunoRoot.resolve("collection.bru").writeText(collectionVariables())

        endpoints.forEachIndexed { index, endpoint ->
            val folderName = endpoint.path.trim('/').split('/').firstOrNull().orEmpty().ifBlank { "root" }
            val endpointDir = requestsDir.resolve(folderName)
            endpointDir.createDirectories()
            val fileName = "%02d-%s.bru".format(index + 1, slug(endpoint.handlerName))
            endpointDir.resolve(fileName).writeText(requestContent(endpoint, index + 1))
        }

        pruneStaleBruFiles(requestsDir, endpoints)
    }

    private fun brunoJson(projectRoot: Path): String {
        val collectionName = projectRoot.fileName?.toString().orEmpty().ifBlank { "collection" }
        return """
            {
              "version": "1",
              "name": "$collectionName",
              "type": "collection"
            }
        """.trimIndent() + "\n"
    }

    private fun collectionVariables(): String {
        return """
            vars:pre-request {
              baseUrl: $defaultBaseUrl
            }
        """.trimIndent() + "\n"
    }

    private fun requestContent(endpoint: Endpoint, seq: Int): String {
        val verbLower = endpoint.httpMethod.lowercase()
        val name = "${endpoint.httpMethod} ${endpoint.path}"
        val content = buildString {
            appendLine("meta {")
            appendLine("  name: $name")
            appendLine("  type: http")
            appendLine("  seq: $seq")
            appendLine("}")
            appendLine()

            appendLine("$verbLower {")
            appendLine("  url: {{baseUrl}}${endpoint.path}")
            if (endpoint.hasRequestBody) {
                appendLine("  body: json")
            }
            appendLine("  auth: none")
            appendLine("}")
            appendLine()

            appendLine("headers {")
            appendLine("  Accept: application/json")
            if (endpoint.hasRequestBody) {
                appendLine("  Content-Type: application/json")
            }
            appendLine("}")

            if (endpoint.hasRequestBody) {
                appendLine()
                appendLine("body:json {")
                appendLine(jsonObject(endpoint))
                appendLine("}")
            }
        }
        return content + "\n"
    }

    private fun jsonObject(endpoint: Endpoint): String {
        val fields = endpoint.requestBodyFields.filter { it.name.isNotBlank() }
        if (fields.isEmpty()) {
            val fallback = if (usesDynamicFakes(endpoint.httpMethod)) "{{\$randomUUID}}" else "{{usuarioId}}"
            return "  {\n    \"id\": \"$fallback\"\n  }"
        }

        val entries = fields.joinToString(",\n") { field ->
            val value = if (usesDynamicFakes(endpoint.httpMethod)) dynamicFakeForField(field) else "{{${field.name}}}"
            "    \"${field.name}\": \"$value\""
        }
        return "  {\n$entries\n  }"
    }

    private fun usesDynamicFakes(httpMethod: String): Boolean {
        return httpMethod.uppercase() in setOf("POST", "PUT", "PATCH")
    }

    private fun dynamicFakeForField(field: RequestBodyField): String {
        val normalized = field.name.lowercase()
        val type = field.type.lowercase()
        val isStringType = type.contains("string")
        val isDate = type.contains("date") ||
                type.contains("time") ||
                type.contains("timestamp") ||
                type.contains("localdatetime") ||
                type.contains("offsetdatetime")
        val isNumericType = type.contains("int") ||
            type.contains("long") ||
            type.contains("double") ||
            type.contains("float") ||
            type.contains("bigdecimal") ||
            type.contains("number")

        val isDouble = type.contains("bigdecimal") ||
                type.contains("double") ||
                type.contains("float")


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
            else -> "{{${field.name}}}"
        }
    }

    private fun slug(raw: String): String {
        return raw
            .replace(Regex("([a-z])([A-Z]+)"), "$1-$2")
            .lowercase()
            .replace(Regex("[^a-z0-9-]+"), "-")
            .trim('-')
            .ifBlank { "request" }
    }

    private fun pruneStaleBruFiles(requestsDir: Path, endpoints: List<Endpoint>) {
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
                    if (normalized !in expected && !file.toString().contains("environments")) {
                        Files.deleteIfExists(file)
                    }
                }
        }
    }



}