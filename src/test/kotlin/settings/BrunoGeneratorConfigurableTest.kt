package settings

import com.codeteam.settings.BrunoEnvironment
import com.codeteam.settings.BrunoGeneratorConfigurable
import com.codeteam.settings.BrunoGeneratorSettings
import com.codeteam.settings.RegenerationMode
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class BrunoGeneratorConfigurableTest : BasePlatformTestCase() {

    private lateinit var configurable: BrunoGeneratorConfigurable

    override fun setUp() {
        super.setUp()
        configurable = BrunoGeneratorConfigurable(project)
        configurable.createComponent()
    }

    override fun tearDown() {
        try {
            configurable.disposeUIResources()
        } finally {
            super.tearDown()
        }
    }

    fun `test default environment choices follow unsaved table edits`() {
        assertEquals(listOf("dev"), configurable.defaultEnvironmentChoices())

        configurable.environmentsModel()!!.addRow(BrunoEnvironment("staging", "https://staging.example.com"))

        assertEquals(listOf("dev", "staging"), configurable.defaultEnvironmentChoices())
        assertTrue(configurable.isModified)
    }

    fun `test collection name preview updates while typing`() {
        configurable.setCollectionNameParts("my-", "shop-api", "-dev")

        assertEquals("my-shop-api-dev", configurable.namePreview())
    }

    fun `test regeneration radio persists on apply and resets`() {
        val state = BrunoGeneratorSettings.getInstance(project).state
        assertFalse(configurable.isModified)

        configurable.selectRegenerationMode(RegenerationMode.ADD_ONLY)
        assertTrue(configurable.isModified)
        configurable.apply()

        assertEquals(RegenerationMode.ADD_ONLY, state.regenerationMode)
        assertFalse(configurable.isModified)

        state.regenerationMode = RegenerationMode.OVERWRITE
        configurable.reset()
        assertFalse(configurable.isModified)
    }
}
