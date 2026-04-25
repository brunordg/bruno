package com.codeteam.model

data class RequestBodyField(
    val name: String,
    val type: String
)

data class Endpoint(
    val httpMethod: String,
    val path: String,
    val handlerName: String,
    val hasRequestBody: Boolean,
    val requestBodyFields: List<RequestBodyField> = emptyList()
) {

}