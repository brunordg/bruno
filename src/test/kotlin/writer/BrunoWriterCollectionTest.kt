package writer

import com.codeteam.model.BodyKind
import com.codeteam.model.Endpoint
import com.codeteam.model.MultipartPart
import com.codeteam.model.ParamKind
import com.codeteam.model.RequestBodyField
import com.codeteam.model.RequestParameter
import com.codeteam.model.ValidationConstraints
import com.codeteam.settings.BrunoEnvironment
import com.codeteam.settings.BrunoGeneratorState
import com.codeteam.writer.BrunoWriter
import org.junit.Assert.assertEquals
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
            bodyKind = BodyKind.JSON,
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

    @Test
    fun `renders nested objects and arrays as real JSON structures instead of placeholders`() {
        val projectRoot = Files.createTempDirectory("bruno-writer-nested-test")
        val settings = BrunoGeneratorState()

        val endpoint = Endpoint(
            httpMethod = "POST",
            path = "/api/orders",
            handlerName = "createOrder",
            hasRequestBody = true,
            bodyKind = BodyKind.JSON,
            requestBodyFields = listOf(
                RequestBodyField(
                    name = "shippingAddress",
                    type = "Address",
                    nestedFields = listOf(
                        RequestBodyField(name = "city", type = "String"),
                        RequestBodyField(name = "street", type = "String")
                    )
                ),
                RequestBodyField(
                    name = "items",
                    type = "List<OrderItem>",
                    isCollection = true,
                    nestedFields = listOf(
                        RequestBodyField(name = "qty", type = "int"),
                        RequestBodyField(name = "sku", type = "String")
                    )
                ),
                RequestBodyField(name = "tags", type = "List<String>", isCollection = true)
            )
        )

        val brunoRoot = writer.writeCollection(projectRoot, listOf(endpoint), settings)
        val requestText = Files.readString(brunoRoot.resolve("requests/api/01-create-order.bru"))

        assertTrue(requestText.contains("\"shippingAddress\": {"))
        assertTrue(requestText.contains("\"city\": "))
        assertTrue(requestText.contains("\"street\": "))
        assertTrue(requestText.contains("\"items\": ["))
        assertTrue(requestText.contains("\"qty\": "))
        assertTrue(requestText.contains("\"sku\": "))
        assertTrue(requestText.contains("\"tags\": ["))
        assertFalse(requestText.contains("\"shippingAddress\": \"{{shippingAddress}}\""))
    }

    @Test
    fun `renders a multipart-form body block for multipart endpoints`() {
        val projectRoot = Files.createTempDirectory("bruno-writer-multipart-test")
        val settings = BrunoGeneratorState()

        val endpoint = Endpoint(
            httpMethod = "POST",
            path = "/api/uploads",
            handlerName = "upload",
            hasRequestBody = true,
            bodyKind = BodyKind.MULTIPART,
            multipartParts = listOf(
                MultipartPart(name = "file", isFile = true, type = "MultipartFile"),
                MultipartPart(name = "caption", isFile = false, type = "String", required = false)
            )
        )

        val brunoRoot = writer.writeCollection(projectRoot, listOf(endpoint), settings)
        val requestText = Files.readString(brunoRoot.resolve("requests/api/01-upload.bru"))

        assertTrue(requestText.contains("body: multipart-form"))
        assertTrue(requestText.contains("body:multipart-form {"))
        assertTrue(requestText.contains("file: @file()"))
        assertTrue(requestText.contains("~caption:"))
        assertFalse(requestText.contains("Content-Type: application/json"))
    }

    @Test
    fun `computeDiff reports added, removed and unchanged files without writing or deleting anything`() {
        val projectRoot = Files.createTempDirectory("bruno-writer-diff-test")
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

        settings.environments.removeIf { it.name == "staging" }
        val endpointC = Endpoint(httpMethod = "GET", path = "/api/c", handlerName = "c", hasRequestBody = false)

        val diff = writer.computeDiff(projectRoot, listOf(endpointA, endpointC), settings)

        assertEquals(setOf("requests/api/02-c.bru"), diff.added.toSet())
        assertEquals(setOf("requests/api/02-b.bru", "environments/staging.bru"), diff.removed.toSet())
        assertEquals(setOf("requests/api/01-a.bru", "environments/dev.bru"), diff.unchanged.toSet())

        assertTrue(Files.exists(brunoRoot.resolve("requests/api/01-a.bru")))
        assertTrue(Files.exists(brunoRoot.resolve("requests/api/02-b.bru")))
        assertTrue(Files.exists(brunoRoot.resolve("environments/staging.bru")))
        assertFalse(Files.exists(brunoRoot.resolve("requests/api/02-c.bru")))
    }
}
