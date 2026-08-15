package writer

import com.codeteam.model.Endpoint
import com.codeteam.model.ParamKind
import com.codeteam.model.RequestBodyField
import com.codeteam.model.RequestParameter
import com.codeteam.model.ValidationConstraints
import com.codeteam.settings.BrunoEnvironment
import com.codeteam.settings.BrunoGeneratorState
import com.codeteam.writer.BrunoWriter
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class BrunoWriterCollectionTest {

    private val writer = BrunoWriter()

    @Test
    fun `writes bruno json, environments, and a request file with params, headers and a validated body`() {
        val projectRoot = Files.createTempDirectory("bruno-writer-test")

        val settings = BrunoGeneratorState().apply {
            environments = mutableListOf(
                BrunoEnvironment("dev", "http://localhost:8080"),
                BrunoEnvironment("staging", "https://staging.example.com")
            )
            defaultEnvironmentName = "staging"
        }

        val endpoint = Endpoint(
            httpMethod = "POST",
            path = "/api/widgets/{id}",
            handlerName = "createWidget",
            hasRequestBody = true,
            requestBodyFields = listOf(
                RequestBodyField(name = "quantity", type = "int", constraints = ValidationConstraints(min = 1, max = 9)),
                RequestBodyField(name = "contactEmail", type = "String", constraints = ValidationConstraints(email = true)),
                RequestBodyField(
                    name = "status", type = "Status", isEnum = true,
                    enumConstants = listOf("ACTIVE", "INACTIVE")
                ),
                RequestBodyField(name = "code", type = "String", constraints = ValidationConstraints(pattern = "[A-Z]{3}"))
            ),
            pathVariables = listOf(RequestParameter(name = "id", kind = ParamKind.PATH, type = "Long", required = true)),
            queryParams = listOf(
                RequestParameter(name = "verbose", kind = ParamKind.QUERY, type = "boolean", required = false, defaultValue = "false")
            ),
            headerParams = listOf(RequestParameter(name = "X-Trace-Id", kind = ParamKind.HEADER, type = "String", required = true)),
            cookieParams = listOf(RequestParameter(name = "session", kind = ParamKind.COOKIE, type = "String", required = false))
        )

        val brunoRoot = writer.writeCollection(projectRoot, listOf(endpoint), settings)

        assertTrue(Files.exists(brunoRoot.resolve("bruno.json")))

        val collectionText = Files.readString(brunoRoot.resolve("collection.bru"))
        assertTrue(collectionText.contains("baseUrl: https://staging.example.com"))

        assertTrue(Files.readString(brunoRoot.resolve("environments/dev.bru")).contains("baseUrl: http://localhost:8080"))
        assertTrue(Files.readString(brunoRoot.resolve("environments/staging.bru")).contains("baseUrl: https://staging.example.com"))

        val requestFile = brunoRoot.resolve("requests/api/01-create-widget.bru")
        assertTrue(Files.exists(requestFile))
        val requestText = Files.readString(requestFile)

        assertTrue(requestText.contains("url: {{baseUrl}}/api/widgets/:id"))
        assertTrue(requestText.contains("params:path {"))
        assertTrue(requestText.contains("params:query {"))
        assertTrue(requestText.contains("~verbose: false"))
        assertTrue(requestText.contains("X-Trace-Id:"))
        assertTrue(requestText.contains("Cookie: session="))
        assertTrue(requestText.contains("\"quantity\": 5"))
        assertTrue(requestText.contains("\"contactEmail\": \"{{\$randomEmail}}\""))
        assertTrue(requestText.contains("\"status\": \"ACTIVE\""))
        assertTrue(requestText.contains("docs {"))
        assertTrue(requestText.contains("code' must match pattern: [A-Z]{3}"))
    }

    @Test
    fun `regenerating prunes stale request files and removed environments`() {
        val projectRoot = Files.createTempDirectory("bruno-writer-prune-test")
        val settings = BrunoGeneratorState().apply {
            environments = mutableListOf(
                BrunoEnvironment("dev", "http://localhost:8080"),
                BrunoEnvironment("staging", "https://staging.example.com")
            )
            defaultEnvironmentName = "dev"
        }
        val endpointA = Endpoint(httpMethod = "GET", path = "/api/a", handlerName = "a", hasRequestBody = false)
        val endpointB = Endpoint(httpMethod = "GET", path = "/api/b", handlerName = "b", hasRequestBody = false)

        val brunoRoot = writer.writeCollection(projectRoot, listOf(endpointA, endpointB), settings)
        assertTrue(Files.exists(brunoRoot.resolve("requests/api/01-a.bru")))
        assertTrue(Files.exists(brunoRoot.resolve("requests/api/02-b.bru")))
        assertTrue(Files.exists(brunoRoot.resolve("environments/staging.bru")))

        settings.environments.removeIf { it.name == "staging" }
        writer.writeCollection(projectRoot, listOf(endpointA), settings)

        assertTrue(Files.exists(brunoRoot.resolve("requests/api/01-a.bru")))
        assertFalse(Files.exists(brunoRoot.resolve("requests/api/02-b.bru")))
        assertFalse(Files.exists(brunoRoot.resolve("environments/staging.bru")))
        assertTrue(Files.exists(brunoRoot.resolve("environments/dev.bru")))
    }

    @Test
    fun `writes the collection under a custom output directory while keeping the project name`() {
        val projectRoot = Files.createTempDirectory("bruno-writer-project")
        val outputRoot = Files.createTempDirectory("bruno-writer-custom-output")
        val settings = BrunoGeneratorState().apply {
            environments = mutableListOf(BrunoEnvironment("dev", "http://localhost:8080"))
            defaultEnvironmentName = "dev"
        }
        val endpoint = Endpoint(httpMethod = "GET", path = "/api/a", handlerName = "a", hasRequestBody = false)

        val brunoRoot = writer.writeCollection(projectRoot, listOf(endpoint), settings, outputRoot)

        val projectName = projectRoot.fileName.toString()
        assertFalse(Files.exists(projectRoot.resolve(projectName)))
        assertTrue(Files.exists(outputRoot.resolve(projectName)))
        assertTrue(brunoRoot.startsWith(outputRoot))
        assertTrue(Files.exists(brunoRoot.resolve("bruno.json")))
        assertTrue(Files.exists(brunoRoot.resolve("requests/api/01-a.bru")))
        assertTrue(
            Files.readString(brunoRoot.resolve("bruno.json")).contains(projectName)
        )
    }
}
