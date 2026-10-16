package com.codeteam.ui

import com.codeteam.model.Endpoint
import com.codeteam.settings.AuthMode
import com.codeteam.settings.FolderStrategy
import com.codeteam.settings.RegenerationMode
import com.codeteam.writer.CollectionDiff

internal const val NO_CONTROLLER_GROUP = "(no controller)"

/**
 * Groups endpoints by their declaring controller. Controllers sharing a simple
 * name across packages get the package appended so the groups stay distinct.
 */
internal fun groupEndpointsByController(endpoints: List<Endpoint>): Map<String, List<Endpoint>> {
    val byQualified = endpoints.groupBy { it.controllerQualifiedName.ifBlank { it.controllerName } }
    val simpleNameCounts = byQualified.keys.groupingBy { simpleName(it) }.eachCount()
    val result = sortedMapOf<String, List<Endpoint>>(String.CASE_INSENSITIVE_ORDER)
    byQualified.forEach { (qualified, group) ->
        val simple = group.first().controllerName.ifBlank { simpleName(qualified) }
        val label = when {
            simple.isBlank() -> NO_CONTROLLER_GROUP
            (simpleNameCounts[simpleName(qualified)] ?: 0) > 1 && qualified.contains('.') ->
                "$simple (${qualified.substringBeforeLast('.')})"
            else -> simple
        }
        result[label] = result[label].orEmpty() + group.sortedWith(compareBy({ it.path }, { it.httpMethod }))
    }
    return result
}

private fun simpleName(qualified: String): String = qualified.substringAfterLast('.')

internal fun matchesFilter(endpoint: Endpoint, query: String): Boolean {
    val needle = query.trim()
    if (needle.isEmpty()) return true
    return listOf(endpoint.httpMethod, endpoint.path, endpoint.handlerName, endpoint.controllerName)
        .any { it.contains(needle, ignoreCase = true) }
}

internal fun confirmLabel(selectedCount: Int): String =
    if (selectedCount == 1) "Generate 1 request" else "Generate $selectedCount requests"

internal fun diffSummary(diff: CollectionDiff): String =
    "+${diff.added.size}  -${diff.removed.size}  =${diff.unchanged.size}"

enum class DiffChange { ADDED, REMOVED }

/** A folder (change == null) or a changed file (leaf) in the diff preview tree. */
internal data class DiffNode(
    val name: String,
    val change: DiffChange? = null,
    val children: MutableList<DiffNode> = mutableListOf()
)

/** Builds a folder tree of the added and removed files; unchanged files only count in the summary. */
internal fun buildDiffTree(diff: CollectionDiff): DiffNode {
    val root = DiffNode("")
    fun insert(path: String, change: DiffChange) {
        val parts = path.replace('\\', '/').split('/').filter { it.isNotEmpty() }
        var node = root
        parts.dropLast(1).forEach { folder ->
            node = node.children.firstOrNull { it.change == null && it.name == folder }
                ?: DiffNode(folder).also { node.children.add(it) }
        }
        node.children.add(DiffNode(parts.last(), change))
    }
    diff.added.sorted().forEach { insert(it, DiffChange.ADDED) }
    diff.removed.sorted().forEach { insert(it, DiffChange.REMOVED) }
    fun sort(node: DiffNode) {
        node.children.sortWith(compareBy({ it.change != null }, { it.name }))
        node.children.forEach(::sort)
    }
    sort(root)
    return root
}

internal val FolderStrategy.label: String
    get() = when (this) {
        FolderStrategy.BY_URL_PATH -> "By URL path"
        FolderStrategy.BY_CONTROLLER -> "By controller"
        FolderStrategy.FLAT -> "Flat"
    }

internal val RegenerationMode.label: String
    get() = when (this) {
        RegenerationMode.OVERWRITE -> "Overwrite"
        RegenerationMode.ADD_ONLY -> "Add-only"
    }

internal val AuthMode.label: String
    get() = when (this) {
        AuthMode.NONE -> "None"
        AuthMode.BEARER -> "Bearer token"
        AuthMode.BASIC -> "Basic"
        AuthMode.API_KEY -> "API key"
    }

internal val FolderStrategy.description: String
    get() = when (this) {
        FolderStrategy.BY_URL_PATH -> "Nested folders mirroring the endpoint path"
        FolderStrategy.BY_CONTROLLER -> "One folder per controller class"
        FolderStrategy.FLAT -> "All requests directly under requests/"
    }

internal val RegenerationMode.description: String
    get() = when (this) {
        RegenerationMode.OVERWRITE -> "Rewrites selected requests and removes stale files"
        RegenerationMode.ADD_ONLY -> "Only creates missing requests; manual edits are preserved"
    }
