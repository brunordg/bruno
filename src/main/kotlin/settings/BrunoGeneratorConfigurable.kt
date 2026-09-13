package com.codeteam.settings

import com.codeteam.MyMessageBundle
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextBrowseFolderListener
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.table.TableModelEditor
import javax.swing.JComponent

class BrunoGeneratorConfigurable(private val project: Project) : Configurable {

    private val settings = BrunoGeneratorSettings.getInstance(project)
    private var tableEditor: TableModelEditor<BrunoEnvironment>? = null
    private var defaultEnvironmentField: JBTextField? = null
    private var outputDirectoryField: TextFieldWithBrowseButton? = null
    private var collectionNameField: JBTextField? = null
    private var collectionNamePrefixField: JBTextField? = null
    private var collectionNameSuffixField: JBTextField? = null

    override fun getDisplayName(): String = MyMessageBundle.message("settings.displayName")

    override fun createComponent(): JComponent {
        val editor = createEnvironmentsTableEditor()
        tableEditor = editor
        val newField = JBTextField()
        defaultEnvironmentField = newField

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

        val panel = panel {
            row {
                cell(editor.createComponent()).align(Align.FILL)
            }.resizableRow()
            row("Default environment name:") {
                cell(newField).align(Align.FILL)
            }
            row("Output directory (optional):") {
                cell(newOutputDirectoryField).align(Align.FILL)
            }.rowComment("Leave empty to generate the collection in the project root")
            row("Collection name:") {
                cell(newCollectionNameField).align(Align.FILL)
            }.rowComment("Default name for the collection folder")
            row("Collection name prefix (optional):") {
                cell(newCollectionNamePrefixField).align(Align.FILL)
            }.rowComment("Prepended to the collection name")
            row("Collection name suffix (optional):") {
                cell(newCollectionNameSuffixField).align(Align.FILL)
            }.rowComment("Appended to the collection name")
        }
        reset()
        return panel
    }

    override fun isModified(): Boolean {
        val currentField = defaultEnvironmentField ?: return false
        val currentOutputDirectoryField = outputDirectoryField ?: return false
        val currentCollectionNameField = collectionNameField ?: return false
        val currentCollectionNamePrefixField = collectionNamePrefixField ?: return false
        val currentCollectionNameSuffixField = collectionNameSuffixField ?: return false
        return tableEditor?.isModified() == true ||
            currentField.text.trim() != settings.state.defaultEnvironmentName ||
            currentOutputDirectoryField.text.trim() != settings.state.outputDirectory ||
            currentCollectionNameField.text.trim() != settings.state.collectionName ||
            currentCollectionNamePrefixField.text.trim() != settings.state.collectionNamePrefix ||
            currentCollectionNameSuffixField.text.trim() != settings.state.collectionNameSuffix
    }

    override fun apply() {
        val editor = tableEditor ?: return
        val currentField = defaultEnvironmentField ?: return
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
        val defaultName = currentField.text.trim()
        if (values.none { it.name == defaultName }) {
            throw ConfigurationException("Default environment \"$defaultName\" does not match any configured environment name.")
        }
        val collectionName = currentCollectionNameField.text.trim()
        if (collectionName.isBlank()) {
            throw ConfigurationException("Collection name cannot be empty.")
        }
        settings.state.environments = values.map { it.copy() }.toMutableList()
        settings.state.defaultEnvironmentName = defaultName
        settings.state.outputDirectory = (outputDirectoryField ?: return).text.trim()
        settings.state.collectionName = collectionName
        settings.state.collectionNamePrefix = (collectionNamePrefixField ?: return).text.trim()
        settings.state.collectionNameSuffix = (collectionNameSuffixField ?: return).text.trim()
    }

    override fun reset() {
        tableEditor?.reset(settings.state.environments.map { it.copy() })
        defaultEnvironmentField?.text = settings.state.defaultEnvironmentName
        outputDirectoryField?.text = settings.state.outputDirectory
        collectionNameField?.text = settings.state.collectionName
        collectionNamePrefixField?.text = settings.state.collectionNamePrefix
        collectionNameSuffixField?.text = settings.state.collectionNameSuffix
    }
}
