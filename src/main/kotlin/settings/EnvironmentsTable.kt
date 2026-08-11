package com.codeteam.settings

import com.intellij.execution.util.ListTableWithButtons
import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.ListTableModel

class EnvironmentsTable : ListTableWithButtons<BrunoEnvironment>() {

    fun currentValues(): List<BrunoEnvironment> = elements

    override fun createListModel(): ListTableModel<BrunoEnvironment> =
        ListTableModel(NameColumn, BaseUrlColumn)

    override fun createElement(): BrunoEnvironment = BrunoEnvironment()

    override fun isEmpty(element: BrunoEnvironment): Boolean =
        element.name.isBlank() && element.baseUrl.isBlank()

    override fun cloneElement(variable: BrunoEnvironment): BrunoEnvironment = variable.copy()

    override fun canDeleteElement(selection: BrunoEnvironment): Boolean = true

    private object NameColumn : ColumnInfo<BrunoEnvironment, String>("Name") {
        override fun valueOf(item: BrunoEnvironment): String = item.name
        override fun setValue(item: BrunoEnvironment, value: String) {
            item.name = value
        }
        override fun isCellEditable(item: BrunoEnvironment): Boolean = true
    }

    private object BaseUrlColumn : ColumnInfo<BrunoEnvironment, String>("Base URL") {
        override fun valueOf(item: BrunoEnvironment): String = item.baseUrl
        override fun setValue(item: BrunoEnvironment, value: String) {
            item.baseUrl = value
        }
        override fun isCellEditable(item: BrunoEnvironment): Boolean = true
    }
}
