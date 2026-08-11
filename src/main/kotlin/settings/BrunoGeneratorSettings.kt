package com.codeteam.settings

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil

data class BrunoEnvironment(
    var name: String = "",
    var baseUrl: String = ""
)

class BrunoGeneratorState {
    var environments: MutableList<BrunoEnvironment> = mutableListOf(
        BrunoEnvironment("dev", "http://localhost:8080"),
        BrunoEnvironment("staging", "https://staging.example.com"),
        BrunoEnvironment("prod", "https://api.example.com")
    )
    var defaultEnvironmentName: String = "dev"
}

fun BrunoGeneratorState.resolvedDefaultBaseUrl(): String =
    environments.firstOrNull { it.name == defaultEnvironmentName }?.baseUrl
        ?: environments.firstOrNull()?.baseUrl
        ?: "http://localhost:8080"

@Service(Service.Level.PROJECT)
@State(name = "BrunoGeneratorSettings", storages = [Storage("bruno-generator.xml")])
class BrunoGeneratorSettings : com.intellij.openapi.components.PersistentStateComponent<BrunoGeneratorState> {

    private var myState = BrunoGeneratorState()

    override fun getState(): BrunoGeneratorState = myState

    override fun loadState(state: BrunoGeneratorState) {
        XmlSerializerUtil.copyBean(state, myState)
    }

    companion object {
        fun getInstance(project: Project): BrunoGeneratorSettings = project.service()
    }
}
