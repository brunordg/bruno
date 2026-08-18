package scanner

import com.codeteam.model.BodyKind
import com.codeteam.scanner.ControllerScanner
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.projectRoots.impl.JavaAwareProjectJdkTableImpl
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.testFramework.PsiTestUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import jakarta.validation.constraints.NotNull
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

class ControllerScannerTest : BasePlatformTestCase() {

    @Suppress("DEPRECATION")
    override fun setUp() {
        super.setUp()
        // Scoped to just the java.base module (not a full SDK): the platform's
        // internal JDK also exposes jdk.javadoc's bundled doclet resources, whose
        // JS indexing crashes in this sandboxed test environment.
        val jdkHome = JavaAwareProjectJdkTableImpl.getInstanceEx().internalJdk.homePath
        ModuleRootModificationUtil.addModuleLibrary(
            module,
            "java.base",
            listOf("jrt://$jdkHome!/java.base"),
            emptyList()
        )
        PsiTestUtil.addLibrary(module, PathManager.getJarPathForClass(RestController::class.java)!!)
        PsiTestUtil.addLibrary(module, PathManager.getJarPathForClass(NotNull::class.java)!!)
        PsiTestUtil.addLibrary(module, PathManager.getJarPathForClass(MultipartFile::class.java)!!)
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

    fun `test extracts nested object fields and lists of objects and scalars from a request body`() {
        myFixture.addFileToProject(
            "OrderController.java",
            """
            import org.springframework.web.bind.annotation.*;

            @RestController
            @RequestMapping("/api/orders")
            public class OrderController {
                @PostMapping
                public Object create(@RequestBody CreateOrderRequest body) {
                    return null;
                }
            }
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "CreateOrderRequest.java",
            """
            import java.util.List;

            public class CreateOrderRequest {
                public Address shippingAddress;
                public List<OrderItem> items;
                public List<String> tags;
            }
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "Address.java",
            """
            public class Address {
                public String street;
                public String city;
            }
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "OrderItem.java",
            """
            public class OrderItem {
                public String sku;
                public int qty;
            }
            """.trimIndent()
        )

        val endpoints = ControllerScanner().scan(project)
        val postEndpoint = endpoints.first { it.httpMethod == "POST" }

        val addressField = postEndpoint.requestBodyFields.first { it.name == "shippingAddress" }
        assertFalse(addressField.isCollection)
        assertEquals(setOf("street", "city"), addressField.nestedFields.map { it.name }.toSet())

        val itemsField = postEndpoint.requestBodyFields.first { it.name == "items" }
        assertTrue(itemsField.isCollection)
        assertEquals(setOf("sku", "qty"), itemsField.nestedFields.map { it.name }.toSet())

        val tagsField = postEndpoint.requestBodyFields.first { it.name == "tags" }
        assertTrue(tagsField.isCollection)
        assertTrue(tagsField.nestedFields.isEmpty())
    }

    fun `test stops recursing into a self-referencing request body field instead of overflowing`() {
        myFixture.addFileToProject(
            "NodeController.java",
            """
            import org.springframework.web.bind.annotation.*;

            @RestController
            @RequestMapping("/api/nodes")
            public class NodeController {
                @PostMapping
                public Object create(@RequestBody Node body) {
                    return null;
                }
            }
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "Node.java",
            """
            public class Node {
                public String label;
                public Node parent;
            }
            """.trimIndent()
        )

        val endpoints = ControllerScanner().scan(project)
        val postEndpoint = endpoints.first { it.httpMethod == "POST" }

        val parentField = postEndpoint.requestBodyFields.first { it.name == "parent" }
        assertFalse(parentField.nestedFields.isEmpty())

        val grandparentField = parentField.nestedFields.first { it.name == "parent" }
        assertTrue(grandparentField.nestedFields.isEmpty())
    }

    fun `test detects multipart request parts and file uploads`() {
        myFixture.addFileToProject(
            "UploadController.java",
            """
            import org.springframework.web.bind.annotation.*;
            import org.springframework.web.multipart.MultipartFile;

            @RestController
            @RequestMapping("/api/uploads")
            public class UploadController {
                @PostMapping
                public Object upload(
                    @RequestPart("file") MultipartFile file,
                    @RequestPart(value = "caption", required = false) String caption
                ) {
                    return null;
                }
            }
            """.trimIndent()
        )

        val endpoints = ControllerScanner().scan(project)
        val postEndpoint = endpoints.first { it.httpMethod == "POST" }

        assertEquals(BodyKind.MULTIPART, postEndpoint.bodyKind)
        assertEquals(2, postEndpoint.multipartParts.size)

        val filePart = postEndpoint.multipartParts.first { it.name == "file" }
        assertTrue(filePart.isFile)
        assertTrue(filePart.required)

        val captionPart = postEndpoint.multipartParts.first { it.name == "caption" }
        assertFalse(captionPart.isFile)
        assertFalse(captionPart.required)
    }
}
