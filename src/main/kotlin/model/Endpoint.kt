package com.codeteam.model

enum class ParamKind { QUERY, PATH, HEADER, COOKIE }

data class RequestParameter(
    val name: String,
    val kind: ParamKind,
    val type: String,
    val required: Boolean,
    val defaultValue: String? = null
)

data class ValidationConstraints(
    val required: Boolean = false,
    val minSize: Int? = null,
    val maxSize: Int? = null,
    val min: Long? = null,
    val max: Long? = null,
    val email: Boolean = false,
    val pattern: String? = null
)

data class RequestBodyField(
    val name: String,
    val type: String,
    val isEnum: Boolean = false,
    val enumConstants: List<String> = emptyList(),
    val constraints: ValidationConstraints = ValidationConstraints(),
    val isCollection: Boolean = false,
    val nestedFields: List<RequestBodyField> = emptyList()
)

enum class BodyKind { NONE, JSON, MULTIPART }

data class MultipartPart(
    val name: String,
    val isFile: Boolean,
    val type: String,
    val required: Boolean = true
)

data class Endpoint(
    val httpMethod: String,
    val path: String,
    val handlerName: String,
    val hasRequestBody: Boolean,
    val bodyKind: BodyKind = BodyKind.NONE,
    val requestBodyFields: List<RequestBodyField> = emptyList(),
    val multipartParts: List<MultipartPart> = emptyList(),
    val pathVariables: List<RequestParameter> = emptyList(),
    val queryParams: List<RequestParameter> = emptyList(),
    val headerParams: List<RequestParameter> = emptyList(),
    val cookieParams: List<RequestParameter> = emptyList()
)
