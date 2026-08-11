package settings

import com.codeteam.settings.BrunoEnvironment
import com.codeteam.settings.BrunoGeneratorState
import com.codeteam.settings.resolvedDefaultBaseUrl
import org.junit.Assert.assertEquals
import org.junit.Test

class BrunoGeneratorStateTest {

    @Test
    fun `resolvedDefaultBaseUrl picks the environment matching defaultEnvironmentName`() {
        val state = BrunoGeneratorState().apply {
            environments = mutableListOf(
                BrunoEnvironment("dev", "http://localhost:8080"),
                BrunoEnvironment("prod", "https://api.example.com")
            )
            defaultEnvironmentName = "prod"
        }
        assertEquals("https://api.example.com", state.resolvedDefaultBaseUrl())
    }

    @Test
    fun `resolvedDefaultBaseUrl falls back to the first environment when the default name is missing`() {
        val state = BrunoGeneratorState().apply {
            environments = mutableListOf(BrunoEnvironment("dev", "http://localhost:8080"))
            defaultEnvironmentName = "does-not-exist"
        }
        assertEquals("http://localhost:8080", state.resolvedDefaultBaseUrl())
    }

    @Test
    fun `resolvedDefaultBaseUrl falls back to a hardcoded default when no environments are configured`() {
        val state = BrunoGeneratorState().apply { environments = mutableListOf() }
        assertEquals("http://localhost:8080", state.resolvedDefaultBaseUrl())
    }
}
