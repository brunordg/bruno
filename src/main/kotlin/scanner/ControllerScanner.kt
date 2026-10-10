package com.codeteam.scanner

import com.codeteam.model.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import com.intellij.psi.util.InheritanceUtil

class ControllerScanner {

    private val verbsWithBody = setOf("POST", "PUT", "PATCH")

    fun scan(project: Project): List<Endpoint> {
        return ApplicationManager.getApplication().runReadAction<List<Endpoint>> {
            val projectScope = GlobalSearchScope.projectScope(project)
            val libraryScope = GlobalSearchScope.allScope(project)
            val restControllerClass = JavaPsiFacade.getInstance(project)
                .findClass(REST_CONTROLLER_FQN, libraryScope)
                ?: return@runReadAction emptyList()

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
            val requestBodyFields = requestBodyParameter?.extractBodyFields().orEmpty()
            val multipartParts = method.multipartParts()
            val hasRequestBody = requestBodyParameter != null || multipartParts.isNotEmpty() || mapping.first in verbsWithBody
            val bodyKind = when {
                multipartParts.isNotEmpty() -> BodyKind.MULTIPART
                hasRequestBody -> BodyKind.JSON
                else -> BodyKind.NONE
            }
            val parameters = method.parameterList.parameters
            val operation = method.modifierList.findAnnotation(OPERATION_FQN)
            Endpoint(
                httpMethod = mapping.first,
                path = fullPath,
                handlerName = method.name,
                hasRequestBody = hasRequestBody,
                summary = operation?.attributeText("summary"),
                description = operation?.attributeText("description"),
                bodyKind = bodyKind,
                requestBodyFields = requestBodyFields,
                multipartParts = multipartParts,
                pathVariables = parameters.extractParams(PATH_VARIABLE_FQN, ParamKind.PATH),
                queryParams = parameters.extractParams(REQUEST_PARAM_FQN, ParamKind.QUERY),
                headerParams = parameters.extractParams(REQUEST_HEADER_FQN, ParamKind.HEADER),
                cookieParams = parameters.extractParams(COOKIE_VALUE_FQN, ParamKind.COOKIE)
            )
        }
    }

    private fun PsiMethod.multipartParts(): List<MultipartPart> =
        parameterList.parameters.mapNotNull { it.toMultipartPart() }

    private fun PsiParameter.toMultipartPart(): MultipartPart? {
        val requestPart = modifierList?.findAnnotation(REQUEST_PART_FQN)
        val isFile = isMultipartFileType(type) || isMultipartFileType(collectionElementType(type))
        if (requestPart == null && !isFile) return null

        val declaredName = requestPart?.findDeclaredAttributeValue("value")?.text
            ?.let { trimQuotes(it) }?.takeIf { it.isNotBlank() }
            ?: requestPart?.findDeclaredAttributeValue("name")?.text
                ?.let { trimQuotes(it) }?.takeIf { it.isNotBlank() }
        val explicitRequired = requestPart?.findDeclaredAttributeValue("required")?.text?.toBooleanStrictOrNull()

        return MultipartPart(
            name = declaredName ?: name.orEmpty(),
            isFile = isFile,
            type = type.presentableText,
            required = explicitRequired ?: true
        )
    }

    private fun isMultipartFileType(type: PsiType?): Boolean {
        val resolved = (type as? PsiClassType)?.resolve() ?: return false
        return resolved.qualifiedName == MULTIPART_FILE_FQN
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
                defaultValue = defaultValueText,
                description = parameter.modifierList?.findAnnotation(PARAMETER_FQN)?.attributeText("description")
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
        return extractFields(psiClass, depth = 0, ancestry = emptySet())
    }

    private fun extractFields(psiClass: PsiClass, depth: Int, ancestry: Set<String>): List<RequestBodyField> {
        return psiClass.allFields
            .filterNot { it.hasModifierProperty("static") }
            .filterNot { it.isJacksonExcluded() }
            .mapNotNull { field -> field.toRequestBodyField(depth, ancestry) }
            .distinctBy { it.name }
            .sortedBy { it.name }
    }

    // Jackson can declare a property's name or ignore it on any of its accessors
    // (the field, the getter, a constructor parameter, or a record component), so
    // collect every owner that could carry a Jackson annotation for this field.
    private fun PsiField.jacksonAccessors(): List<PsiModifierListOwner> {
        val owners = mutableListOf<PsiModifierListOwner>(this)
        val owner = containingClass ?: return owners
        val capitalized = name.replaceFirstChar { it.uppercaseChar() }
        owner.findMethodsByName("get$capitalized", true).forEach { owners.add(it) }
        owner.findMethodsByName("is$capitalized", true).forEach { owners.add(it) }
        owner.findMethodsByName(name, true).forEach { owners.add(it) } // record accessor
        owner.constructors.forEach { ctor ->
            ctor.parameterList.parameters.firstOrNull { it.name == name }?.let { owners.add(it) }
        }
        if (owner.isRecord) {
            owner.recordComponents.firstOrNull { it.name == name }?.let { owners.add(it) }
        }
        return owners
    }

    private fun PsiField.jacksonProperty(): PsiAnnotation? =
        jacksonAccessors().firstNotNullOfOrNull { it.modifierList?.findAnnotation(JSON_PROPERTY_FQN) }

    // Finds an annotation (e.g. @Schema) wherever it may sit for this property:
    // the field, its getter, a constructor parameter, or a record component.
    private fun PsiField.findPropertyAnnotation(fqn: String): PsiAnnotation? =
        jacksonAccessors().firstNotNullOfOrNull { it.modifierList?.findAnnotation(fqn) }

    private fun PsiAnnotation.attributeText(name: String): String? =
        findDeclaredAttributeValue(name)?.text?.let { trimQuotes(it) }?.takeIf { it.isNotBlank() }

    private fun PsiField.resolvedJsonName(): String {
        val declared = jacksonProperty()
            ?.findDeclaredAttributeValue("value")?.text
            ?.let { trimQuotes(it) }
            ?.takeIf { it.isNotBlank() }
        return declared ?: name.trim()
    }

    private fun PsiField.isJacksonExcluded(): Boolean {
        val accessors = jacksonAccessors()
        if (accessors.any { it.hasAnnotation(JSON_IGNORE_FQN) }) return true
        val access = accessors.firstNotNullOfOrNull {
            it.modifierList?.findAnnotation(JSON_PROPERTY_FQN)?.findDeclaredAttributeValue("access")?.text
        }
        return access?.contains("READ_ONLY") == true
    }

    private fun PsiField.toRequestBodyField(depth: Int, ancestry: Set<String>): RequestBodyField? {
        val fieldName = resolvedJsonName()
        if (fieldName.isBlank()) return null

        val elementType = collectionElementType(type)
        val isCollection = elementType != null
        val effectiveType = elementType ?: type

        val fieldClass = (effectiveType as? PsiClassType)?.resolve()
        val isEnum = fieldClass?.isEnum == true
        val enumConstants = if (isEnum) {
            fieldClass.fields.filterIsInstance<PsiEnumConstant>().map { it.name }
        } else {
            emptyList()
        }

        val nestedFields = if (fieldClass != null && !isEnum && depth < MAX_NESTING_DEPTH &&
            isRecursableType(fieldClass) && fieldClass.qualifiedName !in ancestry
        ) {
            extractFields(fieldClass, depth + 1, ancestry + fieldClass.qualifiedName.orEmpty())
        } else {
            emptyList()
        }

        val schema = findPropertyAnnotation(SCHEMA_FQN)
        return RequestBodyField(
            name = fieldName,
            type = type.presentableText,
            isEnum = isEnum,
            enumConstants = enumConstants,
            constraints = extractConstraints(),
            isCollection = isCollection,
            nestedFields = nestedFields,
            example = schema?.attributeText("example"),
            description = schema?.attributeText("description")
        )
    }

    private fun collectionElementType(type: PsiType): PsiType? {
        if (type is PsiArrayType) return type.componentType
        val classType = type as? PsiClassType ?: return null
        val psiClass = classType.resolve() ?: return null
        if (!InheritanceUtil.isInheritor(psiClass, "java.util.Collection")) return null
        return classType.parameters.firstOrNull()
    }

    private fun isRecursableType(psiClass: PsiClass): Boolean {
        if (psiClass.isEnum || psiClass.isInterface || psiClass.isAnnotationType) return false
        val qualifiedName = psiClass.qualifiedName ?: return false
        return JDK_PACKAGE_PREFIXES.none { qualifiedName.startsWith(it) }
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
        private const val REQUEST_PART_FQN = "org.springframework.web.bind.annotation.RequestPart"
        private const val MULTIPART_FILE_FQN = "org.springframework.web.multipart.MultipartFile"
        private const val JSON_PROPERTY_FQN = "com.fasterxml.jackson.annotation.JsonProperty"
        private const val JSON_IGNORE_FQN = "com.fasterxml.jackson.annotation.JsonIgnore"
        private const val OPERATION_FQN = "io.swagger.v3.oas.annotations.Operation"
        private const val SCHEMA_FQN = "io.swagger.v3.oas.annotations.media.Schema"
        private const val PARAMETER_FQN = "io.swagger.v3.oas.annotations.Parameter"
        private const val MAX_NESTING_DEPTH = 5
        private val JDK_PACKAGE_PREFIXES = listOf(
            "java.", "javax.", "jakarta.", "kotlin.", "org.springframework."
        )

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
