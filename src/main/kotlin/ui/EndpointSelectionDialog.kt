package com.codeteam.ui

import com.codeteam.model.Endpoint
import com.codeteam.settings.BrunoGeneratorState
import com.codeteam.writer.BrunoWriter
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.CheckboxTree
import com.intellij.ui.CheckboxTreeBase
import com.intellij.ui.CheckboxTreeListener
import com.intellij.ui.CheckedTreeNode
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.tree.TreeUtil
import java.awt.BorderLayout
import java.awt.Dimension
import java.nio.file.Path
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JSplitPane
import javax.swing.JTree

internal fun groupEndpointsByTopPath(endpoints: List<Endpoint>): Map<String, List<Endpoint>> =
    endpoints.groupBy { it.path.trim('/').split('/').firstOrNull().orEmpty().ifBlank { "root" } }

class EndpointSelectionDialog(
    project: Project,
    endpoints: List<Endpoint>,
    private val projectRoot: Path,
    private val settings: BrunoGeneratorState,
    private val outputRoot: Path,
    private val writer: BrunoWriter
) : DialogWrapper(project) {

    private val root = CheckedTreeNode(null)
    private val tree: CheckboxTree
    private val diffPanel = JPanel(BorderLayout())

    init {
        title = "Generate Bruno Collection"
        groupEndpointsByTopPath(endpoints).toSortedMap().forEach { (group, groupEndpoints) ->
            val groupNode = CheckedTreeNode(group)
            groupEndpoints.forEach { endpoint -> groupNode.add(CheckedTreeNode(endpoint)) }
            root.add(groupNode)
        }
        tree = CheckboxTree(EndpointCellRenderer(), root, CheckboxTreeBase.CheckPolicy.PROPAGATE_EVERYTHING_POLICY)
        TreeUtil.expandAll(tree)
        tree.addCheckboxTreeListener(object : CheckboxTreeListener {
            override fun nodeStateChanged(node: CheckedTreeNode) = refreshDiff()
        })
        init()
        refreshDiff()
    }

    override fun createCenterPanel(): JComponent {
        val treeScroll = JBScrollPane(tree)
        val diffScroll = JBScrollPane(diffPanel)
        val split = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treeScroll, diffScroll)
        split.resizeWeight = 0.5
        split.preferredSize = Dimension(1000, 480)
        return split
    }

    fun selectedEndpoints(): List<Endpoint> {
        val result = mutableListOf<Endpoint>()
        fun walk(node: CheckedTreeNode) {
            val userObject = node.userObject
            if (userObject is Endpoint && node.isChecked) {
                result.add(userObject)
            }
            for (i in 0 until node.childCount) {
                walk(node.getChildAt(i) as CheckedTreeNode)
            }
        }
        walk(root)
        return result
    }

    private fun refreshDiff() {
        val diff = writer.computeDiff(projectRoot, selectedEndpoints(), settings, outputRoot)
        diffPanel.removeAll()
        diffPanel.add(
            panel {
                group("Added (${diff.added.size})") {
                    diff.added.forEach { row { label(it) } }
                }
                group("Removed (${diff.removed.size})") {
                    diff.removed.forEach { row { label(it) } }
                }
                group("Unchanged (${diff.unchanged.size})") {
                    row { label("${diff.unchanged.size} file(s) stay as-is") }
                }
            },
            BorderLayout.CENTER
        )
        diffPanel.revalidate()
        diffPanel.repaint()
    }

    private class EndpointCellRenderer : CheckboxTree.CheckboxTreeCellRenderer() {
        override fun customizeRenderer(
            tree: JTree,
            value: Any,
            selected: Boolean,
            expanded: Boolean,
            leaf: Boolean,
            row: Int,
            hasFocus: Boolean
        ) {
            val node = value as? CheckedTreeNode ?: return
            val text = when (val userObject = node.userObject) {
                is Endpoint -> "${userObject.httpMethod} ${userObject.path} — ${userObject.handlerName}"
                is String -> userObject
                else -> ""
            }
            textRenderer.append(text)
        }
    }
}
