package writer

import com.codeteam.model.*
import com.codeteam.settings.AuthMode
import com.codeteam.settings.BrunoEnvironment
import com.codeteam.settings.BrunoGeneratorState
import com.codeteam.settings.RegenerationMode
import com.codeteam.writer.BrunoWriter
import org.junit.Assert.*
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

        val requestFile = brunoRoot.resolve("requests/api/post-api-widgets-id.bru")
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
        assertTrue(Files.exists(brunoRoot.resolve("requests/api/get-api-a.bru")))
        assertTrue(Files.exists(brunoRoot.resolve("requests/api/get-api-b.bru")))
        assertTrue(Files.exists(brunoRoot.resolve("environments/staging.bru")))

        settings.environments.removeIf { it.name == "staging" }
        writer.writeCollection(projectRoot, listOf(endpointA), settings)

        assertTrue(Files.exists(brunoRoot.resolve("requests/api/get-api-a.bru")))
        assertFalse(Files.exists(brunoRoot.resolve("requests/api/get-api-b.bru")))
        assertFalse(Files.exists(brunoRoot.resolve("environments/staging.bru")))
        assertTrue(Files.exists(brunoRoot.resolve("environments/dev.bru")))
    }

    @Test
    fun `writes the collection under a custom output directory with configured collection name`() {
        val projectRoot = Files.createTempDirectory("bruno-writer-project")
        val outputRoot = Files.createTempDirectory("bruno-writer-custom-output")
        val settings = BrunoGeneratorState().apply {
            environments = mutableListOf(BrunoEnvironment("dev", "http://localhost:8080"))
            defaultEnvironmentName = "dev"
            collectionName = "my-api-collection"
            collectionNamePrefix = "custom-"
        }
        val endpoint = Endpoint(httpMethod = "GET", path = "/api/a", handlerName = "a", hasRequestBody = false)

        val brunoRoot = writer.writeCollection(projectRoot, listOf(endpoint), settings, outputRoot)

        assertFalse(Files.exists(projectRoot.resolve("custom-my-api-collection")))
        assertTrue(Files.exists(outputRoot.resolve("custom-my-api-collection")))
        assertTrue(brunoRoot.startsWith(outputRoot))
        assertTrue(Files.exists(brunoRoot.resolve("bruno.json")))
        assertTrue(Files.exists(brunoRoot.resolve("requests/api/get-api-a.bru")))
        assertTrue(
            Files.readString(brunoRoot.resolve("bruno.json")).contains("custom-my-api-collection")
        )
    }

    @Test
    fun `uses project name with bruno prefix when collection name is empty`() {
        val projectRoot = Files.createTempDirectory("my-project")
        val settings = BrunoGeneratorState().apply {
            environments = mutableListOf(BrunoEnvironment("dev", "http://localhost:8080"))
            defaultEnvironmentName = "dev"
        }
        val endpoint = Endpoint(httpMethod = "GET", path = "/api/a", handlerName = "a", hasRequestBody = false)

        val brunoRoot = writer.writeCollection(projectRoot, listOf(endpoint), settings)

        assertTrue(brunoRoot.fileName.toString().contains("bruno-my-project"))
        assertTrue(Files.exists(brunoRoot.resolve("bruno.json")))
    }

    @Test
    fun `applies custom prefix and suffix to project name when collection name is empty`() {
        val projectRoot = Files.createTempDirectory("my-api")
        val settings = BrunoGeneratorState().apply {
            environments = mutableListOf(BrunoEnvironment("dev", "http://localhost:8080"))
            defaultEnvironmentName = "dev"
            collectionNamePrefix = "test-"
            collectionNameSuffix = "-v1"
        }
        val endpoint = Endpoint(httpMethod = "GET", path = "/api/a", handlerName = "a", hasRequestBody = false)

        val brunoRoot = writer.writeCollection(projectRoot, listOf(endpoint), settings)

        val folderName = brunoRoot.fileName.toString()
        assertTrue(folderName.startsWith("test-my-api"))
        assertTrue(folderName.endsWith("-v1"))
        assertTrue(Files.exists(brunoRoot.resolve("bruno.json")))
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
        val requestText = Files.readString(brunoRoot.resolve("requests/api/post-api-orders.bru"))

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
        val requestText = Files.readString(brunoRoot.resolve("requests/api/post-api-uploads.bru"))

        assertTrue(requestText.contains("body: multipart-form"))
        assertTrue(requestText.contains("body:multipart-form {"))
        assertTrue(requestText.contains("file: @file()"))
        assertTrue(requestText.contains("~caption:"))
        assertFalse(requestText.contains("Content-Type: application/json"))
    }

    @Test
    fun `uses operation summary as the request name and schema example as the field value`() {
        val projectRoot = Files.createTempDirectory("bruno-swagger-test")
        val settings = BrunoGeneratorState()

        val endpoint = Endpoint(
            httpMethod = "POST",
            path = "/api/items",
            handlerName = "createItem",
            hasRequestBody = true,
            summary = "Create an item",
            description = "Creates a catalog item",
            bodyKind = BodyKind.JSON,
            requestBodyFields = listOf(
                // example must win over the @Min constraint
                RequestBodyField(
                    name = "age", type = "int",
                    constraints = ValidationConstraints(min = 18),
                    example = "42",
                    description = "Item age"
                )
            )
        )

        val brunoRoot = writer.writeCollection(projectRoot, listOf(endpoint), settings)
        val requestText = Files.walk(brunoRoot.resolve("requests")).use { stream ->
            Files.readString(stream.filter { it.toString().endsWith(".bru") }.findFirst().get())
        }

        assertTrue(requestText.contains("name: Create an item"))
        assertTrue(requestText.contains("\"age\": 42"))
        assertFalse(requestText.contains("\"age\": 18"))
        assertTrue(requestText.contains("docs {"))
        assertTrue(requestText.contains("Creates a catalog item"))
        assertTrue(requestText.contains("field 'age': Item age"))
    }

    @Test
    fun `falls back to method and path when no operation summary is present`() {
        val projectRoot = Files.createTempDirectory("bruno-swagger-fallback-test")
        val settings = BrunoGeneratorState()
        val endpoint = Endpoint(httpMethod = "GET", path = "/api/items", handlerName = "list", hasRequestBody = false)

        val brunoRoot = writer.writeCollection(projectRoot, listOf(endpoint), settings)
        val requestText = Files.walk(brunoRoot.resolve("requests")).use { stream ->
            Files.readString(stream.filter { it.toString().endsWith(".bru") }.findFirst().get())
        }

        assertTrue(requestText.contains("name: GET /api/items"))
        assertFalse(requestText.contains("docs {"))
    }

    @Test
    fun `bearer auth emits a collection auth block, inherit on requests and a token env variable`() {
        val projectRoot = Files.createTempDirectory("bruno-auth-bearer")
        val settings = BrunoGeneratorState().apply {
            environments = mutableListOf(BrunoEnvironment("dev", "http://localhost:8080"))
            defaultEnvironmentName = "dev"
            authMode = AuthMode.BEARER
        }
        val endpoint = Endpoint(httpMethod = "GET", path = "/api/a", handlerName = "a", hasRequestBody = false)

        val brunoRoot = writer.writeCollection(projectRoot, listOf(endpoint), settings)

        val collectionText = Files.readString(brunoRoot.resolve("collection.bru"))
        assertTrue(collectionText.contains("auth:bearer {"))
        assertTrue(collectionText.contains("token: {{token}}"))

        val requestText = Files.readString(brunoRoot.resolve("requests/api/get-api-a.bru"))
        assertTrue(requestText.contains("auth: inherit"))
        assertFalse(requestText.contains("auth: none"))

        val envText = Files.readString(brunoRoot.resolve("environments/dev.bru"))
        assertTrue(envText.contains("token:"))
    }

    @Test
    fun `basic auth emits username and password variables`() {
        val projectRoot = Files.createTempDirectory("bruno-auth-basic")
        val settings = BrunoGeneratorState().apply { authMode = AuthMode.BASIC }
        val endpoint = Endpoint(httpMethod = "GET", path = "/api/a", handlerName = "a", hasRequestBody = false)

        val brunoRoot = writer.writeCollection(projectRoot, listOf(endpoint), settings)
        val collectionText = Files.readString(brunoRoot.resolve("collection.bru"))

        assertTrue(collectionText.contains("auth:basic {"))
        assertTrue(collectionText.contains("username: {{username}}"))
        assertTrue(collectionText.contains("password: {{password}}"))
        assertTrue(Files.readString(brunoRoot.resolve("environments/dev.bru")).contains("username:"))
    }

    @Test
    fun `api key auth emits an apikey block`() {
        val projectRoot = Files.createTempDirectory("bruno-auth-apikey")
        val settings = BrunoGeneratorState().apply { authMode = AuthMode.API_KEY }
        val endpoint = Endpoint(httpMethod = "GET", path = "/api/a", handlerName = "a", hasRequestBody = false)

        val brunoRoot = writer.writeCollection(projectRoot, listOf(endpoint), settings)
        val collectionText = Files.readString(brunoRoot.resolve("collection.bru"))

        assertTrue(collectionText.contains("auth:apikey {"))
        assertTrue(collectionText.contains("value: {{apiKey}}"))
        assertTrue(Files.readString(brunoRoot.resolve("environments/dev.bru")).contains("apiKey:"))
    }

    @Test
    fun `default none auth keeps requests on auth none and emits no auth block or credential vars`() {
        val projectRoot = Files.createTempDirectory("bruno-auth-none")
        val settings = BrunoGeneratorState()
        val endpoint = Endpoint(httpMethod = "GET", path = "/api/a", handlerName = "a", hasRequestBody = false)

        val brunoRoot = writer.writeCollection(projectRoot, listOf(endpoint), settings)

        val collectionText = Files.readString(brunoRoot.resolve("collection.bru"))
        assertFalse(collectionText.contains("auth"))

        val requestText = Files.readString(brunoRoot.resolve("requests/api/get-api-a.bru"))
        assertTrue(requestText.contains("auth: none"))

        val envText = Files.readString(brunoRoot.resolve("environments/dev.bru"))
        assertEquals("vars {\n  baseUrl: http://localhost:8080\n}\n", envText)
    }

    @Test
    fun `renders a resolved jackson property name as the json key`() {
        val projectRoot = Files.createTempDirectory("bruno-writer-jackson-test")
        val settings = BrunoGeneratorState()

        // The scanner resolves @JsonProperty into the field name; the writer must
        // render that resolved name verbatim as the JSON key and placeholder.
        val endpoint = Endpoint(
            httpMethod = "POST",
            path = "/api/users",
            handlerName = "createUser",
            hasRequestBody = true,
            bodyKind = BodyKind.JSON,
            requestBodyFields = listOf(
                RequestBodyField(name = "user_name", type = "String")
            )
        )

        val brunoRoot = writer.writeCollection(projectRoot, listOf(endpoint), settings)
        val requestText = Files.walk(brunoRoot.resolve("requests")).use { stream ->
            Files.readString(stream.filter { it.toString().endsWith(".bru") }.findFirst().get())
        }

        assertTrue(requestText.contains("\"user_name\":"))
        assertFalse(requestText.contains("userName"))
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

        assertEquals(setOf("requests/api/get-api-c.bru"), diff.added.toSet())
        assertEquals(setOf("requests/api/get-api-b.bru", "environments/staging.bru"), diff.removed.toSet())
        assertEquals(setOf("requests/api/get-api-a.bru", "environments/dev.bru"), diff.unchanged.toSet())

        assertTrue(Files.exists(brunoRoot.resolve("requests/api/get-api-a.bru")))
        assertTrue(Files.exists(brunoRoot.resolve("requests/api/get-api-b.bru")))
        assertTrue(Files.exists(brunoRoot.resolve("environments/staging.bru")))
        assertFalse(Files.exists(brunoRoot.resolve("requests/api/get-api-c.bru")))
    }

    @Test
    fun `request file name is stable across different selections`() {
        val settings = BrunoGeneratorState()
        val endpointA = Endpoint(httpMethod = "GET", path = "/api/a", handlerName = "a", hasRequestBody = false)
        val endpointB = Endpoint(httpMethod = "GET", path = "/api/b", handlerName = "b", hasRequestBody = false)

        val fullRoot = Files.createTempDirectory("bruno-stable-full")
        val fullBruno = writer.writeCollection(fullRoot, listOf(endpointA, endpointB), settings)

        val subsetRoot = Files.createTempDirectory("bruno-stable-subset")
        val subsetBruno = writer.writeCollection(subsetRoot, listOf(endpointB), settings)

        // Endpoint B keeps the same relative path whether or not A is also selected.
        assertTrue(Files.exists(fullBruno.resolve("requests/api/get-api-b.bru")))
        assertTrue(Files.exists(subsetBruno.resolve("requests/api/get-api-b.bru")))
    }

    @Test
    fun `distinct endpoints in the same folder do not collide`() {
        val projectRoot = Files.createTempDirectory("bruno-collision")
        val settings = BrunoGeneratorState()
        val get = Endpoint(httpMethod = "GET", path = "/api/widgets", handlerName = "list", hasRequestBody = false)
        val post = Endpoint(httpMethod = "POST", path = "/api/widgets", handlerName = "list", hasRequestBody = false)

        val brunoRoot = writer.writeCollection(projectRoot, listOf(get, post), settings)

        assertTrue(Files.exists(brunoRoot.resolve("requests/api/get-api-widgets.bru")))
        assertTrue(Files.exists(brunoRoot.resolve("requests/api/post-api-widgets.bru")))
    }

    @Test
    fun `add-only preserves an existing edited request file`() {
        val projectRoot = Files.createTempDirectory("bruno-addonly-preserve")
        val settings = BrunoGeneratorState().apply { regenerationMode = RegenerationMode.ADD_ONLY }
        val endpoint = Endpoint(httpMethod = "GET", path = "/api/a", handlerName = "a", hasRequestBody = false)

        val brunoRoot = writer.writeCollection(projectRoot, listOf(endpoint), settings)
        val requestFile = brunoRoot.resolve("requests/api/get-api-a.bru")
        Files.writeString(requestFile, "hand edited content")

        writer.writeCollection(projectRoot, listOf(endpoint), settings)

        assertEquals("hand edited content", Files.readString(requestFile))
    }

    @Test
    fun `add-only still creates a request file for a new endpoint`() {
        val projectRoot = Files.createTempDirectory("bruno-addonly-create")
        val settings = BrunoGeneratorState().apply { regenerationMode = RegenerationMode.ADD_ONLY }
        val endpointA = Endpoint(httpMethod = "GET", path = "/api/a", handlerName = "a", hasRequestBody = false)
        val endpointB = Endpoint(httpMethod = "GET", path = "/api/b", handlerName = "b", hasRequestBody = false)

        writer.writeCollection(projectRoot, listOf(endpointA), settings)
        val brunoRoot = writer.writeCollection(projectRoot, listOf(endpointA, endpointB), settings)

        assertTrue(Files.exists(brunoRoot.resolve("requests/api/get-api-b.bru")))
    }

    @Test
    fun `add-only does not delete a request file for a deselected endpoint`() {
        val projectRoot = Files.createTempDirectory("bruno-addonly-nodelete")
        val settings = BrunoGeneratorState().apply { regenerationMode = RegenerationMode.ADD_ONLY }
        val endpointA = Endpoint(httpMethod = "GET", path = "/api/a", handlerName = "a", hasRequestBody = false)
        val endpointB = Endpoint(httpMethod = "GET", path = "/api/b", handlerName = "b", hasRequestBody = false)

        val brunoRoot = writer.writeCollection(projectRoot, listOf(endpointA, endpointB), settings)
        writer.writeCollection(projectRoot, listOf(endpointA), settings)

        assertTrue(Files.exists(brunoRoot.resolve("requests/api/get-api-b.bru")))
    }

    @Test
    fun `add-only diff reports no removals and keeps existing files as unchanged`() {
        val projectRoot = Files.createTempDirectory("bruno-addonly-diff")
        val settings = BrunoGeneratorState()
        val endpointA = Endpoint(httpMethod = "GET", path = "/api/a", handlerName = "a", hasRequestBody = false)
        val endpointB = Endpoint(httpMethod = "GET", path = "/api/b", handlerName = "b", hasRequestBody = false)

        writer.writeCollection(projectRoot, listOf(endpointA, endpointB), settings)

        settings.regenerationMode = RegenerationMode.ADD_ONLY
        val endpointC = Endpoint(httpMethod = "GET", path = "/api/c", handlerName = "c", hasRequestBody = false)
        val diff = writer.computeDiff(projectRoot, listOf(endpointA, endpointC), settings)

        assertEquals(setOf("requests/api/get-api-c.bru"), diff.added.toSet())
        assertTrue(diff.removed.isEmpty())
        assertTrue(diff.unchanged.contains("requests/api/get-api-a.bru"))
        assertTrue(diff.unchanged.contains("requests/api/get-api-b.bru"))
    }
}
