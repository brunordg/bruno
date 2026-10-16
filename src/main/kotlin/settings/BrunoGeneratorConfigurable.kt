package com.codeteam.settings

import com.codeteam.MyMessageBundle
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.TextBrowseFolderListener
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.codeteam.ui.description
import com.codeteam.ui.label
import com.codeteam.writer.composeCollectionName
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBRadioButton
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.table.TableModelEditor
import java.awt.Font
import java.nio.file.Path
import javax.swing.JComponent
import javax.swing.event.DocumentEvent

class BrunoGeneratorConfigurable(private val project: Project) : Configurable {

    private val settings = BrunoGeneratorSettings.getInstance(project)
    private var tableEditor: TableModelEditor<BrunoEnvironment>? = null
    private var defaultEnvironmentCombo: ComboBox<String>? = null
    private val regenerationRadios = mutableMapOf<RegenerationMode, JBRadioButton>()
    private val folderStrategyRadios = mutableMapOf<FolderStrategy, JBRadioButton>()
    private val namePreviewLabel = JBLabel().apply { font = font.deriveFont(Font.BOLD) }
    private var outputDirectoryField: TextFieldWithBrowseButton? = null
    private var collectionNameField: JBTextField? = null
    private var collectionNamePrefixField: JBTextField? = null
    private var collectionNameSuffixField: JBTextField? = null
    private var authModeCombo: ComboBox<AuthMode>? = null

    override fun getDisplayName(): String = MyMessageBundle.message("settings.displayName")

    override fun createComponent(): JComponent {
        val editor = createEnvironmentsTableEditor()
        tableEditor = editor
        val newDefaultEnvironmentCombo = ComboBox<String>()
        defaultEnvironmentCombo = newDefaultEnvironmentCombo
        // Keep the default-environment choices in step with unsaved table edits.
        editor.model.addTableModelListener { refreshEnvironmentChoices() }

        val newOutputDirectoryField = TextFieldWithBrowseButton()
        newOutputDirectoryField.addBrowseFolderListener(
            TextBrowseFolderListener(
                FileChooserDescriptorFactory.createSingleFolderDescriptor(),
                project
            )
        )
        outputDirectoryField = newOutputDirectoryField

        val newCollectionNameField = JBTextField()
        collectionNameField = newCollectionNameField
        val newCollectionNamePrefixField = JBTextField()
        collectionNamePrefixField = newCollectionNamePrefixField
        val newCollectionNameSuffixField = JBTextField()
        collectionNameSuffixField = newCollectionNameSuffixField
        newCollectionNamePrefixField.emptyText.text = "prefix"
        newCollectionNameField.emptyText.text = "project name"
        newCollectionNameSuffixField.emptyText.text = "suffix"
        val previewListener = object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) = refreshNamePreview()
        }
        listOf(newCollectionNameField, newCollectionNamePrefixField, newCollectionNameSuffixField)
            .forEach { it.document.addDocumentListener(previewListener) }

        val newAuthModeCombo = ComboBox(AuthMode.entries.toTypedArray())
        newAuthModeCombo.renderer = SimpleListCellRenderer.create("") { it.label }
        authModeCombo = newAuthModeCombo

        val panel = panel {
            group("Environments") {
                row {
                    cell(editor.createComponent()).align(Align.FILL)
                }.resizableRow()
                row("Default environment:") {
                    cell(newDefaultEnvironmentCombo).align(AlignX.FILL)
                }
            }
            group("Output") {
                row("Directory:") {
                    cell(newOutputDirectoryField).align(AlignX.FILL)
                }.rowComment("Optional. Leave empty to generate the collection in the project root")
                row("Collection name:") {
                    cell(newCollectionNamePrefixField).columns(6)
                    cell(newCollectionNameField).resizableColumn().align(AlignX.FILL)
                    cell(newCollectionNameSuffixField).columns(6)
                }.rowComment("Prefix, name and suffix. An empty name uses the project name")
                row("Folder name:") {
                    cell(namePreviewLabel)
                }
            }
            group("Generation") {
                buttonsGroup("Regeneration mode:") {
                    RegenerationMode.entries.forEach { mode ->
                        row {
                            val radio = radioButton(mode.label).comment(mode.description).component
                            regenerationRadios[mode] = radio
                        }
                    }
                }
                buttonsGroup("Default folder strategy:") {
                    FolderStrategy.entries.forEach { strategy ->
                        row {
                            val radio = radioButton(strategy.label).comment(strategy.description).component
                            folderStrategyRadios[strategy] = radio
                        }
                    }
                }
                row("Authentication:") {
                    cell(newAuthModeCombo)
                }.rowComment("Collection-level auth inherited by every request. Fill token / username / password / apiKey per environment")
            }
        }
        reset()
        return panel
    }

    internal fun environmentsModel() = tableEditor?.model

    internal fun defaultEnvironmentChoices(): List<String> =
        defaultEnvironmentCombo?.let { combo -> (0 until combo.itemCount).map { combo.getItemAt(it) } }.orEmpty()

    internal fun namePreview(): String = namePreviewLabel.text

    internal fun setCollectionNameParts(prefix: String, name: String, suffix: String) {
        collectionNamePrefixField?.text = prefix
        collectionNameField?.text = name
        collectionNameSuffixField?.text = suffix
    }

    internal fun selectRegenerationMode(mode: RegenerationMode) {
        regenerationRadios[mode]?.isSelected = true
    }

    private fun refreshEnvironmentChoices() {
        val combo = defaultEnvironmentCombo ?: return
        val editor = tableEditor ?: return
        val current = combo.selectedItem as? String
        val names = editor.model.items.map { it.name.trim() }.filter { it.isNotEmpty() }.distinct()
        combo.removeAllItems()
        names.forEach(combo::addItem)
        combo.selectedItem = current?.takeIf { it in names } ?: names.firstOrNull()
    }

    private fun refreshNamePreview() {
        val baseName = collectionNameField?.text?.trim().orEmpty().ifBlank {
            project.basePath?.let { Path.of(it).fileName?.toString() }.orEmpty().ifBlank { "collection" }
        }
        namePreviewLabel.text = composeCollectionName(
            baseName,
            collectionNamePrefixField?.text.orEmpty(),
            collectionNameSuffixField?.text.orEmpty()
        )
    }

    override fun isModified(): Boolean {
        val currentDefault = defaultEnvironmentCombo ?: return false
        val currentOutputDirectoryField = outputDirectoryField ?: return false
        val currentCollectionNameField = collectionNameField ?: return false
        val currentCollectionNamePrefixField = collectionNamePrefixField ?: return false
        val currentCollectionNameSuffixField = collectionNameSuffixField ?: return false
        return tableEditor?.isModified() == true ||
            selectedDefaultEnvironment() != settings.state.defaultEnvironmentName ||
            currentOutputDirectoryField.text.trim() != settings.state.outputDirectory ||
            currentCollectionNameField.text.trim() != settings.state.collectionName ||
            currentCollectionNamePrefixField.text.trim() != settings.state.collectionNamePrefix ||
            currentCollectionNameSuffixField.text.trim() != settings.state.collectionNameSuffix ||
            selectedRegenerationMode() != settings.state.regenerationMode ||
            selectedAuthMode() != settings.state.authMode ||
            selectedFolderStrategy() != settings.state.folderStrategy
    }

    private fun selectedDefaultEnvironment(): String =
        (defaultEnvironmentCombo?.selectedItem as? String).orEmpty().trim()

    private fun selectedRegenerationMode(): RegenerationMode =
        regenerationRadios.entries.firstOrNull { it.value.isSelected }?.key ?: RegenerationMode.OVERWRITE

    private fun selectedAuthMode(): AuthMode =
        authModeCombo?.selectedItem as? AuthMode ?: AuthMode.NONE

    private fun selectedFolderStrategy(): FolderStrategy =
        folderStrategyRadios.entries.firstOrNull { it.value.isSelected }?.key ?: FolderStrategy.BY_URL_PATH

    override fun apply() {
        val editor = tableEditor ?: return
        val currentCollectionNameField = collectionNameField ?: return
        val values = editor.apply()
            .filter { it.name.isNotBlank() && it.baseUrl.isNotBlank() }
        if (values.isEmpty()) {
            throw ConfigurationException("Add at least one environment with a name and base URL.")
        }
        val distinctNames = values.map { it.name }.toSet()
        if (distinctNames.size != values.size) {
            throw ConfigurationException("Environment names must be unique.")
        }
        val defaultName = selectedDefaultEnvironment()
        if (values.none { it.name == defaultName }) {
            throw ConfigurationException("Default environment \"$defaultName\" does not match any configured environment name.")
        }
        settings.state.environments = values.map { it.copy() }.toMutableList()
        settings.state.defaultEnvironmentName = defaultName
        settings.state.outputDirectory = (outputDirectoryField ?: return).text.trim()
        settings.state.collectionName = currentCollectionNameField.text.trim()
        settings.state.collectionNamePrefix = (collectionNamePrefixField ?: return).text.trim()
        settings.state.collectionNameSuffix = (collectionNameSuffixField ?: return).text.trim()
        settings.state.regenerationMode = selectedRegenerationMode()
        settings.state.authMode = selectedAuthMode()
        settings.state.folderStrategy = selectedFolderStrategy()
    }

    override fun reset() {
        tableEditor?.reset(settings.state.environments.map { it.copy() })
        outputDirectoryField?.text = settings.state.outputDirectory
        collectionNameField?.text = settings.state.collectionName
        collectionNamePrefixField?.text = settings.state.collectionNamePrefix
        collectionNameSuffixField?.text = settings.state.collectionNameSuffix
        regenerationRadios[settings.state.regenerationMode]?.isSelected = true
        authModeCombo?.selectedItem = settings.state.authMode
        folderStrategyRadios[settings.state.folderStrategy]?.isSelected = true
        refreshEnvironmentChoices()
        defaultEnvironmentCombo?.selectedItem = settings.state.defaultEnvironmentName
        refreshNamePreview()
    }
}
