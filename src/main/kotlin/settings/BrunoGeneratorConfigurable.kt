package com.codeteam.settings

import com.codeteam.MyMessageBundle
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.table.TableModelEditor
import javax.swing.JComponent

class BrunoGeneratorConfigurable(private val project: Project) : Configurable {

    private val settings = BrunoGeneratorSettings.getInstance(project)
    private var tableEditor: TableModelEditor<BrunoEnvironment>? = null
    private var defaultEnvironmentField: JBTextField? = null

    override fun getDisplayName(): String = MyMessageBundle.message("settings.displayName")

    override fun createComponent(): JComponent {
        val editor = createEnvironmentsTableEditor()
        tableEditor = editor
        val newField = JBTextField()
        defaultEnvironmentField = newField

        val panel = panel {
            row {
                cell(editor.createComponent()).align(Align.FILL)
            }.resizableRow()
            row("Default environment name:") {
                cell(newField).align(Align.FILL)
            }
        }
        reset()
        return panel
    }

    override fun isModified(): Boolean {
        val currentField = defaultEnvironmentField ?: return false
        return tableEditor?.isModified() == true ||
            currentField.text.trim() != settings.state.defaultEnvironmentName
    }

    override fun apply() {
        val editor = tableEditor ?: return
        val currentField = defaultEnvironmentField ?: return
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
        settings.state.environments = values.map { it.copy() }.toMutableList()
        settings.state.defaultEnvironmentName = defaultName
    }

    override fun reset() {
        tableEditor?.reset(settings.state.environments.map { it.copy() })
        defaultEnvironmentField?.text = settings.state.defaultEnvironmentName
    }
}
