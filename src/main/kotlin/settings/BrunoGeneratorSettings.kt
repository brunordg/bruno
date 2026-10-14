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

enum class RegenerationMode {
    /** Rewrite every selected request and remove stale request files (default). */
    OVERWRITE,

    /** Only create missing request files; never overwrite or delete, preserving hand edits. */
    ADD_ONLY
}

enum class AuthMode {
    /** No collection auth; requests keep `auth: none` (default). */
    NONE,
    BEARER,
    BASIC,
    API_KEY
}

enum class FolderStrategy {
    /** Nested folders mirroring the endpoint's URL path (default). */
    BY_URL_PATH,

    /** One folder per declaring controller class. */
    BY_CONTROLLER,

    /** All request files directly under requests/, no subfolder. */
    FLAT
}

class BrunoGeneratorState {
    var environments: MutableList<BrunoEnvironment> = mutableListOf(
        BrunoEnvironment("dev", "http://localhost:8080")
    )
    var defaultEnvironmentName: String = "dev"
    var outputDirectory: String = ""
    var collectionName: String = ""
    var collectionNamePrefix: String = "bruno-"
    var collectionNameSuffix: String = ""
    var regenerationMode: RegenerationMode = RegenerationMode.OVERWRITE
    var authMode: AuthMode = AuthMode.NONE
    var folderStrategy: FolderStrategy = FolderStrategy.BY_URL_PATH
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
