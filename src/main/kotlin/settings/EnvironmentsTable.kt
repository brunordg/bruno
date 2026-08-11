package com.codeteam.settings

import com.intellij.util.ui.CollectionItemEditor
import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.table.TableModelEditor

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

private class BrunoEnvironmentItemEditor : CollectionItemEditor<BrunoEnvironment> {
    override fun getItemClass(): Class<out BrunoEnvironment> = BrunoEnvironment::class.java
    override fun clone(item: BrunoEnvironment, forInPlaceEditing: Boolean): BrunoEnvironment = item.copy()
    override fun isEmpty(item: BrunoEnvironment): Boolean = item.name.isBlank() && item.baseUrl.isBlank()
}

fun createEnvironmentsTableEditor(): TableModelEditor<BrunoEnvironment> {
    return TableModelEditor(
        arrayOf(NameColumn, BaseUrlColumn),
        BrunoEnvironmentItemEditor(),
        "No environments configured"
    )
}
