package com.codeteam.scanner

import com.codeteam.model.Endpoint
import com.codeteam.model.ParamKind
import com.codeteam.model.RequestBodyField
import com.codeteam.model.RequestParameter
import com.codeteam.model.ValidationConstraints
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiEnumConstant
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifierListOwner
import com.intellij.psi.PsiParameter
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.AnnotatedElementsSearch

class ControllerScanner {

    private val verbsWithBody = setOf("POST", "PUT", "PATCH")

    fun scan(project: Project): List<Endpoint> {
        return ReadAction.compute<List<Endpoint>, RuntimeException> {
            val projectScope = GlobalSearchScope.projectScope(project)
            val libraryScope = GlobalSearchScope.allScope(project)
            val restControllerClass = JavaPsiFacade.getInstance(project)
                .findClass(REST_CONTROLLER_FQN, libraryScope)
                ?: return@compute emptyList()

            AnnotatedElementsSearch.searchPsiClasses(restControllerClass, projectScope)
                .findAll()
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
            val parameters = method.parameterList.parameters
            Endpoint(
                httpMethod = mapping.first,
                path = fullPath,
                handlerName = method.name,
                hasRequestBody = hasRequestBody,
                requestBodyFields = requestBodyParameter?.extractBodyFields().orEmpty(),
                pathVariables = parameters.extractParams(PATH_VARIABLE_FQN, ParamKind.PATH),
                queryParams = parameters.extractParams(REQUEST_PARAM_FQN, ParamKind.QUERY),
                headerParams = parameters.extractParams(REQUEST_HEADER_FQN, ParamKind.HEADER),
                cookieParams = parameters.extractParams(COOKIE_VALUE_FQN, ParamKind.COOKIE)
            )
        }
    }

    private fun Array<PsiParameter>.extractParams(annotationFqn: String, kind: ParamKind): List<RequestParameter> {
        return mapNotNull { parameter ->
            val annotation = parameter.modifierList?.findAnnotation(annotationFqn) ?: return@mapNotNull null
            val declaredName = annotation.findDeclaredAttributeValue("value")?.text
                ?: annotation.findDeclaredAttributeValue("name")?.text
            val name = declaredName?.let { trimQuotes(it) }?.takeIf { it.isNotBlank() } ?: parameter.name
            val defaultValueText = annotation.findDeclaredAttributeValue("defaultValue")?.text
                ?.let { trimQuotes(it) }
                ?.takeIf { it.isNotBlank() }
            val explicitRequired = annotation.findDeclaredAttributeValue("required")?.text?.toBooleanStrictOrNull()
            val required = explicitRequired ?: (defaultValueText == null)
            RequestParameter(
                name = name,
                kind = kind,
                type = parameter.type.presentableText,
                required = required,
                defaultValue = defaultValueText
            )
        }
    }

    private fun PsiMethod.requestBodyParameter(): PsiParameter? {
        return parameterList.parameters.firstOrNull {
            it.hasAnnotation(REQUEST_BODY_FQN)
        }
    }

    private fun PsiParameter.extractBodyFields(): List<RequestBodyField> {
        val psiClass = (type as? PsiClassType)?.resolve() ?: return emptyList()
        return psiClass.allFields
            .filterNot { it.hasModifierProperty("static") }
            .mapNotNull { field -> field.toRequestBodyField() }
            .distinctBy { it.name }
            .sortedBy { it.name }
    }

    private fun PsiField.toRequestBodyField(): RequestBodyField? {
        val fieldName = name.trim()
        if (fieldName.isBlank()) return null
        val fieldClass = (type as? PsiClassType)?.resolve()
        val isEnum = fieldClass?.isEnum == true
        val enumConstants = if (isEnum) {
            fieldClass.fields.filterIsInstance<PsiEnumConstant>().map { it.name }
        } else {
            emptyList()
        }
        return RequestBodyField(
            name = fieldName,
            type = type.presentableText,
            isEnum = isEnum,
            enumConstants = enumConstants,
            constraints = extractConstraints()
        )
    }

    private fun PsiField.extractConstraints(): ValidationConstraints {
        val required = hasAnyAnnotation(NOT_NULL_ANNOTATIONS) ||
            hasAnyAnnotation(NOT_BLANK_ANNOTATIONS) ||
            hasAnyAnnotation(NOT_EMPTY_ANNOTATIONS)
        val sizeAnnotation = findAnyAnnotation(SIZE_ANNOTATIONS)
        val minSize = sizeAnnotation?.findDeclaredAttributeValue("min")?.text?.toIntOrNull()
        val maxSize = sizeAnnotation?.findDeclaredAttributeValue("max")?.text?.toIntOrNull()
        val min = findAnyAnnotation(MIN_ANNOTATIONS)?.findDeclaredAttributeValue("value")?.text?.toLongOrNull()
        val max = findAnyAnnotation(MAX_ANNOTATIONS)?.findDeclaredAttributeValue("value")?.text?.toLongOrNull()
        val email = hasAnyAnnotation(EMAIL_ANNOTATIONS)
        val pattern = findAnyAnnotation(PATTERN_ANNOTATIONS)
            ?.findDeclaredAttributeValue("regexp")?.text?.let { trimQuotes(it) }
        return ValidationConstraints(
            required = required,
            minSize = minSize,
            maxSize = maxSize,
            min = min,
            max = max,
            email = email,
            pattern = pattern
        )
    }

    private fun PsiModifierListOwner.hasAnyAnnotation(fqns: List<String>): Boolean =
        fqns.any { hasAnnotation(it) }

    private fun PsiModifierListOwner.findAnyAnnotation(fqns: List<String>): PsiAnnotation? =
        fqns.firstNotNullOfOrNull { modifierList?.findAnnotation(it) }

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

    internal fun stripArraySyntax(raw: String): String {
        val trimmed = raw.trim()
        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) return trimmed
        val inner = trimmed.removePrefix("{").removeSuffix("}").trim()
        val first = inner.split(",").firstOrNull()?.trim().orEmpty()
        return first
    }

    internal fun trimQuotes(text: String): String = text.trim().trim('"')

    internal fun normalizePath(classPath: String, methodPath: String): String {
        val left = classPath.trim().trim('/')
        val right = methodPath.trim().trim('/')
        val joined = listOf(left, right).filter { it.isNotBlank() }.joinToString("/")
        return "/$joined".replace("//", "/")
    }

    companion object {
        private const val REST_CONTROLLER_FQN = "org.springframework.web.bind.annotation.RestController"
        private const val REQUEST_BODY_FQN = "org.springframework.web.bind.annotation.RequestBody"
        private const val PATH_VARIABLE_FQN = "org.springframework.web.bind.annotation.PathVariable"
        private const val REQUEST_PARAM_FQN = "org.springframework.web.bind.annotation.RequestParam"
        private const val REQUEST_HEADER_FQN = "org.springframework.web.bind.annotation.RequestHeader"
        private const val COOKIE_VALUE_FQN = "org.springframework.web.bind.annotation.CookieValue"

        private val NOT_NULL_ANNOTATIONS = listOf(
            "jakarta.validation.constraints.NotNull",
            "javax.validation.constraints.NotNull"
        )
        private val NOT_BLANK_ANNOTATIONS = listOf(
            "jakarta.validation.constraints.NotBlank",
            "javax.validation.constraints.NotBlank"
        )
        private val NOT_EMPTY_ANNOTATIONS = listOf(
            "jakarta.validation.constraints.NotEmpty",
            "javax.validation.constraints.NotEmpty"
        )
        private val SIZE_ANNOTATIONS = listOf(
            "jakarta.validation.constraints.Size",
            "javax.validation.constraints.Size"
        )
        private val MIN_ANNOTATIONS = listOf(
            "jakarta.validation.constraints.Min",
            "javax.validation.constraints.Min"
        )
        private val MAX_ANNOTATIONS = listOf(
            "jakarta.validation.constraints.Max",
            "javax.validation.constraints.Max"
        )
        private val EMAIL_ANNOTATIONS = listOf(
            "jakarta.validation.constraints.Email",
            "javax.validation.constraints.Email"
        )
        private val PATTERN_ANNOTATIONS = listOf(
            "jakarta.validation.constraints.Pattern",
            "javax.validation.constraints.Pattern"
        )
    }
}
