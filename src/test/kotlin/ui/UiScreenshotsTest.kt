package ui

import com.codeteam.model.Endpoint
import com.codeteam.settings.AuthMode
import com.codeteam.settings.BrunoEnvironment
import com.codeteam.settings.BrunoGeneratorConfigurable
import com.codeteam.settings.BrunoGeneratorSettings
import com.codeteam.settings.BrunoGeneratorState
import com.codeteam.ui.EndpointSelectionDialog
import com.codeteam.writer.BrunoWriter
import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.awt.Component
import java.awt.Container
import java.awt.Dimension
import java.awt.GraphicsEnvironment
import java.awt.image.BufferedImage
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO
import javax.swing.JComponent
import javax.swing.JFrame
import javax.swing.SwingUtilities

/**
 * Renders the plugin's main screens to PNG for the README. Skipped unless
 * `-Dbruno.screenshots=<dir>` is set, and needs a display (`java.awt.headless=false`)
 * so the tree cell renderers lay out like in the IDE.
 */
class UiScreenshotsTest : BasePlatformTestCase() {

    private val outDir: Path? = System.getProperty("bruno.screenshots")?.let(Path::of)

    private fun endpoint(method: String, path: String, handler: String, controller: String) = Endpoint(
        httpMethod = method,
        path = path,
        handlerName = handler,
        hasRequestBody = method in setOf("POST", "PUT", "PATCH"),
        controllerName = controller,
        controllerQualifiedName = "com.shop.api.$controller"
    )

    private val endpoints = listOf(
        endpoint("GET", "/api/customers", "list", "CustomerController"),
        endpoint("POST", "/api/customers", "create", "CustomerController"),
        endpoint("GET", "/api/customers/{id}", "findById", "CustomerController"),
        endpoint("GET", "/api/orders", "search", "OrderController"),
        endpoint("POST", "/api/orders", "create", "OrderController"),
        endpoint("GET", "/api/orders/{id}", "findById", "OrderController"),
        endpoint("PATCH", "/api/orders/{id}/status", "updateStatus", "OrderController"),
        endpoint("DELETE", "/api/orders/{id}", "cancel", "OrderController"),
        endpoint("GET", "/api/products", "search", "ProductController"),
        endpoint("PUT", "/api/products/{id}", "update", "ProductController"),
        endpoint("POST", "/api/products/{id}/image", "uploadImage", "ProductController")
    )

    private fun settings() = BrunoGeneratorState().apply {
        environments = mutableListOf(
            BrunoEnvironment("dev", "http://localhost:8080"),
            BrunoEnvironment("staging", "https://staging.shop.com"),
            BrunoEnvironment("prod", "https://api.shop.com")
        )
        collectionName = "shop-api"
        collectionNamePrefix = ""
        authMode = AuthMode.BEARER
    }

    fun `test capture endpoint selection dialog`() {
        val dir = outDir ?: return
        val root = Files.createTempDirectory("shop-api")
        val dialog = EndpointSelectionDialog(project, endpoints, root, settings(), root, BrunoWriter())
        Disposer.register(testRootDisposable, dialog.disposable)
        capture(dialog.snapshotPanel(), Dimension(1040, 640), dir.resolve("dialog-select-endpoints.png"))
    }

    fun `test capture dialog with filter and removal`() {
        val dir = outDir ?: return
        val root = Files.createTempDirectory("shop-api")
        val settings = settings()
        // An earlier generation on disk, so unchecking an endpoint shows up as a removal.
        BrunoWriter().writeCollection(root, endpoints.dropLast(1), settings, root)
        val dialog = EndpointSelectionDialog(project, endpoints, root, settings, root, BrunoWriter())
        Disposer.register(testRootDisposable, dialog.disposable)
        dialog.setSelected(endpoints.first { it.handlerName == "cancel" }, false)
        dialog.applyFilter("orders")
        capture(dialog.snapshotPanel(), Dimension(1040, 640), dir.resolve("dialog-filter-and-diff.png"))
    }

    fun `test capture settings page`() {
        val dir = outDir ?: return
        val state = BrunoGeneratorSettings.getInstance(project).state
        settings().let {
            state.environments = it.environments
            state.collectionName = it.collectionName
            state.collectionNamePrefix = it.collectionNamePrefix
            state.authMode = it.authMode
        }
        val configurable = BrunoGeneratorConfigurable(project)
        try {
            capture(configurable.createComponent(), Dimension(820, 960), dir.resolve("settings.png"))
        } finally {
            configurable.disposeUIResources()
        }
    }

    private fun capture(component: JComponent, size: Dimension, target: Path) {
        assertFalse("screenshots need a display", GraphicsEnvironment.isHeadless())
        val frame = JFrame().apply {
            contentPane.add(component)
            component.preferredSize = size
            // The first capture in a JVM can predate the IDE look-and-feel; re-apply it.
            SwingUtilities.updateComponentTreeUI(this)
            pack()
        }
        try {
            component.setSize(size)
            layoutRecursively(component)
            val scale = 2
            val image = BufferedImage(size.width * scale, size.height * scale, BufferedImage.TYPE_INT_RGB)
            val g = image.createGraphics()
            g.scale(scale.toDouble(), scale.toDouble())
            component.paint(g)
            g.dispose()
            Files.createDirectories(target.parent)
            ImageIO.write(image, "png", target.toFile())
        } finally {
            frame.dispose()
        }
    }

    private fun layoutRecursively(component: Component) {
        component.doLayout()
        (component as? Container)?.components?.forEach { layoutRecursively(it) }
    }
}
