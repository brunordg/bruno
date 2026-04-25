package com.codeteam.scanner

import com.codeteam.model.Endpoint
import com.codeteam.model.RequestBodyField
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifierListOwner
import com.intellij.psi.PsiParameter
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiShortNamesCache

class ControllerScanner {

    private val verbsWithBody = setOf("POST", "PUT", "PATCH")

    fun scan(project: Project): List<Endpoint> {
        return ReadAction.compute<List<Endpoint>, RuntimeException> {
            val scope = GlobalSearchScope.projectScope(project)
            val cache = PsiShortNamesCache.getInstance(project)
            val candidateClasses = cache.allClassNames
                .flatMap { cache.getClassesByName(it, scope).toList() }

            candidateClasses
                .filter { it.hasAnnotation("org.springframework.web.bind.annotation.RestController") }
                .flatMap { extractEndpoints(it) }
                .distinctBy { "${it.httpMethod} ${it.path}" }
                .sortedWith(compareBy<Endpoint> { it.path }.thenBy { it.httpMethod })
        }
    }

    private fun extractEndpoints(psiClass: PsiClass): List<Endpoint> {
        val classPath = firstMappingPath(psiClass)
        return psiClass.methods.mapNotNull { method ->
            val mapping = methodMapping(method) ?: return@mapNotNull null
            val fullPath = normalizePath(classPath, mapping.second)
            val requestBodyParameter = method.requestBodyParameter()
            val hasRequestBody = requestBodyParameter != null || mapping.first in verbsWithBody
            Endpoint(
                httpMethod = mapping.first,
                path = fullPath,
                handlerName = method.name,
                hasRequestBody = hasRequestBody,
                requestBodyFields = requestBodyParameter?.extractBodyFields().orEmpty()
            )
        }
    }

    private fun PsiMethod.requestBodyParameter(): PsiParameter? {
        return parameterList.parameters.firstOrNull {
            it.hasAnnotation("org.springframework.web.bind.annotation.RequestBody")
        }
    }

    private fun PsiParameter.extractBodyFields(): List<RequestBodyField> {
        val psiClass = (type as? PsiClassType)?.resolve() ?: return emptyList()
        return psiClass.allFields
            .filterNot { it.hasModifierProperty("static") }
            .mapNotNull { field ->
                val name = field.name.trim()
                if (name.isBlank()) return@mapNotNull null
                RequestBodyField(
                    name = name,
                    type = field.type.presentableText
                )
            }
            .distinctBy { it.name }
            .sortedBy { it.name }
    }

    private fun methodMapping(method: PsiMethod): Pair<String, String>? {
        val mappings = listOf(
            "org.springframework.web.bind.annotation.GetMapping" to "GET",
            "org.springframework.web.bind.annotation.PostMapping" to "POST",
            "org.springframework.web.bind.annotation.PutMapping" to "PUT",
            "org.springframework.web.bind.annotation.DeleteMapping" to "DELETE",
            "org.springframework.web.bind.annotation.PatchMapping" to "PATCH"
        )

        for ((annotationFqn, verb) in mappings) {
            val annotation = method.modifierList.findAnnotation(annotationFqn) ?: continue
            return verb to firstPathValue(annotation)
        }

        val requestMapping =
            method.modifierList.findAnnotation("org.springframework.web.bind.annotation.RequestMapping")
                ?: return null
        val methodAttr = requestMapping.findDeclaredAttributeValue("method")?.text.orEmpty()
        val verb = when {
            methodAttr.contains("GET") -> "GET"
            methodAttr.contains("POST") -> "POST"
            methodAttr.contains("PUT") -> "PUT"
            methodAttr.contains("DELETE") -> "DELETE"
            methodAttr.contains("PATCH") -> "PATCH"
            else -> "GET"
        }
        return verb to firstPathValue(requestMapping)
    }

    private fun firstMappingPath(owner: PsiModifierListOwner): String {
        val annotation = owner.modifierList
            ?.findAnnotation("org.springframework.web.bind.annotation.RequestMapping")
            ?: return ""
        return firstPathValue(annotation)
    }

    private fun firstPathValue(annotation: PsiAnnotation): String {
        val value = annotation.findDeclaredAttributeValue("value")?.text
            ?: annotation.findDeclaredAttributeValue("path")?.text
            ?: ""
        return trimQuotes(stripArraySyntax(value))
    }

    private fun stripArraySyntax(raw: String): String {
        val trimmed = raw.trim()
        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) return trimmed
        val inner = trimmed.removePrefix("{").removeSuffix("}").trim()
        val first = inner.split(",").firstOrNull()?.trim().orEmpty()
        return first
    }

    private fun trimQuotes(text: String): String = text.trim().trim('"')

    private fun normalizePath(classPath: String, methodPath: String): String {
        val left = classPath.trim().trim('/')
        val right = methodPath.trim().trim('/')
        val joined = listOf(left, right).filter { it.isNotBlank() }.joinToString("/")
        return "/$joined".replace("//", "/")
    }

}