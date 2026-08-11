package scanner

import com.codeteam.scanner.ControllerScanner
import com.intellij.openapi.application.PathManager
import com.intellij.testFramework.PsiTestUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import jakarta.validation.constraints.NotNull
import org.springframework.web.bind.annotation.RestController

class ControllerScannerTest : BasePlatformTestCase() {

    override fun setUp() {
        super.setUp()
        PsiTestUtil.addLibrary(module, PathManager.getJarPathForClass(RestController::class.java)!!)
        PsiTestUtil.addLibrary(module, PathManager.getJarPathForClass(NotNull::class.java)!!)
    }

    fun `test scans a rest controller for params, path variables, headers, cookies and a validated body`() {
        myFixture.addFileToProject(
            "WidgetController.java",
            """
            import org.springframework.web.bind.annotation.*;

            @RestController
            @RequestMapping("/api/widgets")
            public class WidgetController {

                @GetMapping("/{id}")
                public Object get(
                    @PathVariable Long id,
                    @RequestParam(required = false, defaultValue = "10") int limit,
                    @RequestHeader("X-Trace-Id") String traceId,
                    @CookieValue(value = "session", required = false) String session
                ) {
                    return null;
                }

                @PostMapping
                public Object create(@RequestBody CreateWidgetRequest body) {
                    return null;
                }
            }
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "CreateWidgetRequest.java",
            """
            import jakarta.validation.constraints.*;

            public class CreateWidgetRequest {
                @NotBlank
                public String name;
                @Email
                public String contactEmail;
                @Min(1)
                @Max(100)
                public int quantity;
                public Status status;
            }
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "Status.java",
            """
            public enum Status { ACTIVE, INACTIVE }
            """.trimIndent()
        )

        val endpoints = ControllerScanner().scan(project)

        assertEquals(2, endpoints.size)

        val getEndpoint = endpoints.first { it.httpMethod == "GET" }
        assertEquals("/api/widgets/{id}", getEndpoint.path)

        assertEquals(1, getEndpoint.pathVariables.size)
        assertEquals("id", getEndpoint.pathVariables[0].name)
        assertTrue(getEndpoint.pathVariables[0].required)

        assertEquals(1, getEndpoint.queryParams.size)
        assertEquals("limit", getEndpoint.queryParams[0].name)
        assertFalse(getEndpoint.queryParams[0].required)
        assertEquals("10", getEndpoint.queryParams[0].defaultValue)

        assertEquals(1, getEndpoint.headerParams.size)
        assertEquals("X-Trace-Id", getEndpoint.headerParams[0].name)

        assertEquals(1, getEndpoint.cookieParams.size)
        assertEquals("session", getEndpoint.cookieParams[0].name)
        assertFalse(getEndpoint.cookieParams[0].required)

        val postEndpoint = endpoints.first { it.httpMethod == "POST" }
        assertEquals("/api/widgets", postEndpoint.path)
        assertEquals(4, postEndpoint.requestBodyFields.size)

        val contactEmailField = postEndpoint.requestBodyFields.first { it.name == "contactEmail" }
        assertTrue(contactEmailField.constraints.email)

        val quantityField = postEndpoint.requestBodyFields.first { it.name == "quantity" }
        assertEquals(1L, quantityField.constraints.min)
        assertEquals(100L, quantityField.constraints.max)

        val nameField = postEndpoint.requestBodyFields.first { it.name == "name" }
        assertTrue(nameField.constraints.required)

        val statusField = postEndpoint.requestBodyFields.first { it.name == "status" }
        assertTrue(statusField.isEnum)
        assertEquals(listOf("ACTIVE", "INACTIVE"), statusField.enumConstants)
    }
}
