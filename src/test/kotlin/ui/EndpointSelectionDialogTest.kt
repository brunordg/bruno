package ui

import com.codeteam.model.Endpoint
import com.codeteam.settings.BrunoGeneratorState
import com.codeteam.settings.FolderStrategy
import com.codeteam.settings.RegenerationMode
import com.codeteam.ui.EndpointSelectionDialog
import com.codeteam.writer.BrunoWriter
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.TextRange
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.nio.file.Files

class EndpointSelectionDialogTest : BasePlatformTestCase() {

    private fun endpoint(method: String, path: String, handler: String, controller: String) = Endpoint(
        httpMethod = method,
        path = path,
        handlerName = handler,
        hasRequestBody = false,
        controllerName = controller,
        controllerQualifiedName = "com.shop.$controller"
    )

    private val endpoints = listOf(
        endpoint("GET", "/api/orders/{id}", "findById", "OrderController"),
        endpoint("POST", "/api/orders", "create", "OrderController"),
        endpoint("DELETE", "/api/orders/{id}", "cancel", "OrderController"),
        endpoint("GET", "/api/products", "search", "ProductController"),
        endpoint("PUT", "/api/products/{id}", "update", "ProductController")
    )

    private fun dialog(mode: RegenerationMode = RegenerationMode.OVERWRITE): EndpointSelectionDialog {
        val root = Files.createTempDirectory("bruno-dialog-test")
        val settings = BrunoGeneratorState().apply { regenerationMode = mode }
        val dialog = EndpointSelectionDialog(project, endpoints, root, settings, root, BrunoWriter())
        Disposer.register(testRootDisposable, dialog.disposable)
        return dialog
    }

    fun `test starts with every endpoint selected and a counting confirm button`() {
        val dialog = dialog()

        assertEquals(endpoints.toSet(), dialog.selectedEndpoints().toSet())
        assertEquals("Generate 5 requests", dialog.confirmText())
        assertTrue(dialog.isConfirmEnabled())
        // 5 request files plus the default environment file.
        assertTrue(dialog.diffSummaryText().contains("+6 added"))
    }

    fun `test rows are grouped by controller with method, path, handler and counter`() {
        val dialog = dialog()
        dialog.applyFilter("cancel")
        dialog.applyFilter("")

        assertEquals(
            listOf(
                "OrderController  3/3",
                "POST/api/orders  create",
                "DELETE/api/orders/{id}  cancel",
                "GET/api/orders/{id}  findById",
                "ProductController  2/2",
                "GET/api/products  search",
                "PUT/api/products/{id}  update"
            ),
            dialog.renderedRows()
        )
    }

    fun `test clearing the selection disables the confirm button`() {
        val dialog = dialog()

        dialog.setAll(false)
        dialog.applyFilter("")

        assertTrue(dialog.selectedEndpoints().isEmpty())
        assertFalse(dialog.isConfirmEnabled())
        assertEquals("Generate 0 requests", dialog.confirmText())
        assertEquals("OrderController  0/3", dialog.renderedRows().first())
        assertTrue(dialog.diffSummaryText().contains("+1 added"))
    }

    fun `test filtering hides rows but keeps hidden endpoints selected`() {
        val dialog = dialog()

        dialog.applyFilter("products")

        assertEquals(listOf("search", "update"), dialog.visibleEndpoints().map { it.handlerName })
        assertEquals(5, dialog.selectedEndpoints().size)

        dialog.applyFilter("")
        assertEquals(5, dialog.visibleEndpoints().size)
    }

    fun `test select all after clear restores every endpoint`() {
        val dialog = dialog()
        dialog.setAll(false)

        dialog.setAll(true)

        assertEquals(5, dialog.selectedEndpoints().size)
        assertEquals("Generate 5 requests", dialog.confirmText())
    }

    fun `test double click navigation opens the handler method in the editor`() {
        myFixture.addFileToProject(
            "OrderController.java",
            """
            public class OrderController {
                public Object findById(Long id) { return null; }

                public Object create(Object body) { return null; }
            }
            """.trimIndent()
        )
        val dialog = dialog()
        // Fixture files live in the default package, so the qualified name is the simple one.
        val create = endpoints.first { it.handlerName == "create" }.copy(controllerQualifiedName = "OrderController")

        assertTrue(dialog.navigateToHandler(create))

        val editor = FileEditorManager.getInstance(project).selectedTextEditor!!
        assertEquals("OrderController.java", FileDocumentManager.getInstance().getFile(editor.document)!!.name)
        val caretLine = editor.document.getLineNumber(editor.caretModel.offset)
        assertTrue(editor.document.getText(TextRange(editor.document.getLineStartOffset(caretLine), editor.document.getLineEndOffset(caretLine))).contains("create("))
    }

    fun `test navigation is a no-op when the controller cannot be resolved`() {
        val dialog = dialog()

        assertFalse(dialog.navigateToHandler(endpoints.first { it.handlerName == "search" }))
    }

    fun `test switching folder strategy is returned to the caller`() {
        val dialog = dialog()

        dialog.selectStrategy(FolderStrategy.FLAT)

        assertEquals(FolderStrategy.FLAT, dialog.selectedFolderStrategy())
    }
}
