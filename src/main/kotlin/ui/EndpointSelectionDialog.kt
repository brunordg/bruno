package com.codeteam.ui

import com.codeteam.model.Endpoint
import com.codeteam.settings.BrunoGeneratorState
import com.codeteam.settings.FolderStrategy
import com.codeteam.writer.BrunoWriter
import com.codeteam.writer.CollectionDiff
import com.intellij.icons.AllIcons
import com.intellij.openapi.observable.properties.AtomicProperty
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.ui.CheckboxTree
import com.intellij.ui.CheckboxTreeBase
import com.intellij.ui.CheckboxTreeListener
import com.intellij.ui.CheckedTreeNode
import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.DoubleClickListener
import com.intellij.ui.JBColor
import com.intellij.ui.JBSplitter
import com.intellij.ui.SearchTextField
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.ActionLink
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.treeStructure.Tree
import com.intellij.ui.scale.JBUIScale
import com.intellij.util.PsiNavigateUtil
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.tree.TreeUtil
import java.awt.BorderLayout
import java.awt.event.MouseEvent
import java.nio.file.Path
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTree
import javax.swing.event.DocumentEvent
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

class EndpointSelectionDialog(
    private val project: Project,
    endpoints: List<Endpoint>,
    private val projectRoot: Path,
    private val settings: BrunoGeneratorState,
    private val outputRoot: Path,
    private val writer: BrunoWriter
) : DialogWrapper(project) {

    /** A controller group in the selection tree; counts come from [selection], so hidden rows still count. */
    private class Group(val label: String, val endpoints: List<Endpoint>)

    private val groups = groupEndpointsByController(endpoints).map { (label, list) -> Group(label, list) }

    // Selection lives outside the tree so filtering never drops hidden endpoints.
    private val selection = LinkedHashMap<Endpoint, Boolean>().apply {
        groups.flatMap { it.endpoints }.forEach { put(it, true) }
    }

    private val root = CheckedTreeNode(null)
    private val tree = CheckboxTree(EndpointCellRenderer(), root, CheckboxTreeBase.CheckPolicy.PROPAGATE_EVERYTHING_POLICY)
    private val searchField = SearchTextField(false)
    private val diffSummaryLabel = JBLabel()
    private val diffRoot = DefaultMutableTreeNode()
    private val diffTree = Tree(DefaultTreeModel(diffRoot))
    private val strategyProperty = AtomicProperty(settings.folderStrategy)
    private val selectedStrategy: FolderStrategy get() = strategyProperty.get()
    private var rebuilding = false

    init {
        title = "Generate Bruno Collection"
        tree.isRootVisible = false
        tree.addCheckboxTreeListener(object : CheckboxTreeListener {
            override fun nodeStateChanged(node: CheckedTreeNode) {
                if (rebuilding) return
                syncSelectionFromTree()
                refresh()
            }
        })
        object : DoubleClickListener() {
            override fun onDoubleClick(event: MouseEvent): Boolean {
                val endpoint = (tree.selectionPath?.lastPathComponent as? CheckedTreeNode)?.userObject as? Endpoint
                    ?: return false
                return navigateToHandler(endpoint)
            }
        }.installOn(tree)

        searchField.textEditor.emptyText.text = "Filter by method, path, handler or controller"
        searchField.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) = rebuildTree()
        })

        diffTree.isRootVisible = false
        diffTree.showsRootHandles = true
        diffTree.cellRenderer = DiffCellRenderer()

        init()
        rebuildTree()
        refresh()
    }

    override fun getPreferredFocusedComponent(): JComponent = searchField.textEditor

    override fun createNorthPanel(): JComponent = panel {
        row {
            cell(searchField).resizableColumn().align(AlignX.FILL)
            cell(ActionLink("Select all") { setAll(true) })
            cell(ActionLink("Clear") { setAll(false) })
        }
        row("Folder strategy:") {
            segmentedButton(FolderStrategy.entries) { text = it.label }.bind(strategyProperty)
            strategyProperty.afterChange(disposable) { refresh() }
        }
    }

    override fun createCenterPanel(): JComponent {
        val diffPanel = JPanel(BorderLayout()).apply {
            diffSummaryLabel.border = JBUI.Borders.empty(6, 8)
            add(diffSummaryLabel, BorderLayout.NORTH)
            add(JBScrollPane(diffTree), BorderLayout.CENTER)
        }
        return JBSplitter(false, 0.5f).apply {
            firstComponent = JBScrollPane(tree)
            secondComponent = diffPanel
            preferredSize = JBUI.size(1000, 480)
        }
    }

    override fun createSouthAdditionalPanel(): JPanel {
        val mode = settings.regenerationMode.label
        return JPanel(BorderLayout()).apply {
            add(JBLabel("Regeneration mode: $mode").apply {
                foreground = JBColor.GRAY
                toolTipText = "Change it in Settings > Tools > Bruno Generator"
            }, BorderLayout.WEST)
        }
    }

    fun selectedFolderStrategy(): FolderStrategy = selectedStrategy

    internal fun applyFilter(query: String) {
        searchField.text = query
    }

    internal fun visibleEndpoints(): List<Endpoint> =
        TreeUtil.treeNodeTraverser(root).mapNotNull { (it as? CheckedTreeNode)?.userObject as? Endpoint }.toList()

    internal fun confirmText(): String = okAction.getValue(javax.swing.Action.NAME) as String

    internal fun isConfirmEnabled(): Boolean = isOKActionEnabled

    internal fun selectStrategy(strategy: FolderStrategy) {
        strategyProperty.set(strategy)
    }

    internal fun diffSummaryText(): String = diffSummaryLabel.text

    internal fun renderedRows(): List<String> = (0 until tree.rowCount).map { row ->
        val path = tree.getPathForRow(row)
        val renderer = tree.cellRenderer.getTreeCellRendererComponent(
            tree, path.lastPathComponent, false, true, tree.model.isLeaf(path.lastPathComponent), row, false
        ) as CheckboxTree.CheckboxTreeCellRenderer
        renderer.textRenderer.getCharSequence(false).toString()
    }

    /** Assembles the dialog body outside a window, for headless visual snapshots in tests. */
    internal fun snapshotPanel(): JComponent = JPanel(BorderLayout()).apply {
        add(createNorthPanel(), BorderLayout.NORTH)
        add(createCenterPanel(), BorderLayout.CENTER)
        add(createSouthPanel(), BorderLayout.SOUTH)
    }

    internal fun setSelected(endpoint: Endpoint, checked: Boolean) {
        selection[endpoint] = checked
        rebuildTree()
        refresh()
    }

    fun selectedEndpoints(): List<Endpoint> = selection.filterValues { it }.keys.toList()

    internal fun setAll(checked: Boolean) {
        selection.keys.forEach { selection[it] = checked }
        rebuildTree()
        refresh()
    }

    private fun syncSelectionFromTree() {
        TreeUtil.treeNodeTraverser(root).forEach { node ->
            val checked = node as? CheckedTreeNode ?: return@forEach
            val endpoint = checked.userObject as? Endpoint ?: return@forEach
            selection[endpoint] = checked.isChecked
        }
        tree.repaint()
    }

    private fun rebuildTree() {
        rebuilding = true
        try {
            root.removeAllChildren()
            val query = searchField.text
            groups.forEach { group ->
                val visible = group.endpoints.filter { matchesFilter(it, query) }
                if (visible.isEmpty()) return@forEach
                val groupNode = CheckedTreeNode(group)
                visible.forEach { endpoint ->
                    groupNode.add(CheckedTreeNode(endpoint).apply { isChecked = selection[endpoint] == true })
                }
                groupNode.isChecked = visible.all { selection[it] == true }
                root.add(groupNode)
            }
            (tree.model as DefaultTreeModel).reload()
            TreeUtil.expandAll(tree)
        } finally {
            rebuilding = false
        }
    }

    private fun refresh() {
        val selected = selectedEndpoints()
        setOKButtonText(confirmLabel(selected.size))
        isOKActionEnabled = selected.isNotEmpty()
        renderDiff(writer.computeDiff(projectRoot, selected, settings, outputRoot, selectedStrategy))
    }

    private fun renderDiff(diff: CollectionDiff) {
        fun count(text: String, size: Int, color: JBColor) =
            if (size == 0) text else "<b><font color='#${hex(color)}'>$text</font></b>"
        diffSummaryLabel.text = "<html>" + count("+${diff.added.size} added", diff.added.size, ADDED_COLOR) +
            "&nbsp;&nbsp;" + count("-${diff.removed.size} removed", diff.removed.size, REMOVED_COLOR) +
            "&nbsp;&nbsp;=${diff.unchanged.size} unchanged</html>"
        diffRoot.removeAllChildren()
        fun attach(parent: DefaultMutableTreeNode, node: DiffNode) {
            node.children.forEach { child ->
                val treeNode = DefaultMutableTreeNode(child)
                parent.add(treeNode)
                attach(treeNode, child)
            }
        }
        attach(diffRoot, buildDiffTree(diff))
        (diffTree.model as DefaultTreeModel).reload()
        TreeUtil.expandAll(diffTree)
    }

    internal fun navigateToHandler(endpoint: Endpoint): Boolean {
        if (endpoint.controllerQualifiedName.isBlank()) return false
        val psiClass = JavaPsiFacade.getInstance(project)
            .findClass(endpoint.controllerQualifiedName, GlobalSearchScope.projectScope(project))
            ?: return false
        val method = psiClass.findMethodsByName(endpoint.handlerName, false).firstOrNull() ?: return false
        if (!method.canNavigate()) return false
        PsiNavigateUtil.navigate(method)
        return true
    }

    private inner class EndpointCellRenderer : CheckboxTree.CheckboxTreeCellRenderer() {
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
            when (val userObject = node.userObject) {
                is Endpoint -> {
                    textRenderer.append(
                        userObject.httpMethod,
                        SimpleTextAttributes(SimpleTextAttributes.STYLE_BOLD, methodColor(userObject.httpMethod))
                    )
                    // Aligns paths into a column regardless of the method's width.
                    textRenderer.appendTextPadding(JBUIScale.scale(METHOD_COLUMN_WIDTH))
                    textRenderer.append(userObject.path)
                    textRenderer.append("  ${userObject.handlerName}", SimpleTextAttributes.GRAYED_ATTRIBUTES)
                }
                is Group -> {
                    textRenderer.icon = AllIcons.Nodes.Class
                    textRenderer.append(userObject.label, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
                    val count = userObject.endpoints.count { selection[it] == true }
                    textRenderer.append("  $count/${userObject.endpoints.size}", SimpleTextAttributes.GRAYED_ATTRIBUTES)
                }
            }
        }
    }

    private class DiffCellRenderer : ColoredTreeCellRenderer() {
        override fun customizeCellRenderer(
            tree: JTree,
            value: Any?,
            selected: Boolean,
            expanded: Boolean,
            leaf: Boolean,
            row: Int,
            hasFocus: Boolean
        ) {
            val node = (value as? DefaultMutableTreeNode)?.userObject as? DiffNode ?: return
            when (node.change) {
                null -> {
                    icon = AllIcons.Nodes.Folder
                    append(node.name)
                }
                DiffChange.ADDED -> {
                    icon = AllIcons.General.Add
                    append(node.name, SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, ADDED_COLOR))
                }
                DiffChange.REMOVED -> {
                    icon = AllIcons.General.Remove
                    append(node.name, SimpleTextAttributes(SimpleTextAttributes.STYLE_STRIKEOUT, REMOVED_COLOR))
                }
            }
        }
    }

    private companion object {
        const val METHOD_COLUMN_WIDTH = 64
        val ADDED_COLOR = JBColor(0x2E7D32, 0x5FB865)
        val REMOVED_COLOR = JBColor(0xC62828, 0xE06C6C)

        fun methodColor(method: String): JBColor = when (method.uppercase()) {
            "GET" -> ADDED_COLOR
            "POST" -> JBColor(0xB26A00, 0xE0A85A)
            "PUT" -> JBColor(0x1565C0, 0x4D9DE0)
            "PATCH" -> JBColor(0x6A1B9A, 0xB48AE0)
            "DELETE" -> REMOVED_COLOR
            else -> JBColor.GRAY
        }

        fun hex(color: JBColor): String = "%06x".format(color.rgb and 0xFFFFFF)
    }
}
