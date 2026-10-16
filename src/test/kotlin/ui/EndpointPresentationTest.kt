package ui

import com.codeteam.model.Endpoint
import com.codeteam.ui.DiffChange
import com.codeteam.ui.NO_CONTROLLER_GROUP
import com.codeteam.ui.buildDiffTree
import com.codeteam.ui.confirmLabel
import com.codeteam.ui.diffSummary
import com.codeteam.ui.groupEndpointsByController
import com.codeteam.ui.matchesFilter
import com.codeteam.writer.CollectionDiff
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EndpointPresentationTest {

    private fun endpoint(
        method: String,
        path: String,
        handler: String,
        controller: String = "",
        qualified: String = ""
    ) = Endpoint(
        httpMethod = method,
        path = path,
        handlerName = handler,
        hasRequestBody = false,
        controllerName = controller,
        controllerQualifiedName = qualified
    )

    @Test
    fun `groups endpoints under a common prefix by controller`() {
        val listOrders = endpoint("GET", "/api/orders", "list", "OrderController", "com.shop.OrderController")
        val createOrder = endpoint("POST", "/api/orders", "create", "OrderController", "com.shop.OrderController")
        val products = endpoint("GET", "/api/products", "search", "ProductController", "com.shop.ProductController")

        val groups = groupEndpointsByController(listOf(products, createOrder, listOrders))

        assertEquals(listOf("OrderController", "ProductController"), groups.keys.toList())
        assertEquals(listOf(listOrders, createOrder), groups.getValue("OrderController"))
    }

    @Test
    fun `disambiguates controllers sharing a simple name with their package`() {
        val v1 = endpoint("GET", "/v1/users", "list", "UserController", "com.shop.v1.UserController")
        val v2 = endpoint("GET", "/v2/users", "list", "UserController", "com.shop.v2.UserController")

        val groups = groupEndpointsByController(listOf(v1, v2))

        assertEquals(
            setOf("UserController (com.shop.v1)", "UserController (com.shop.v2)"),
            groups.keys
        )
    }

    @Test
    fun `endpoints without controller fall into a fallback group`() {
        val orphan = endpoint("GET", "/health", "status")

        assertEquals(setOf(NO_CONTROLLER_GROUP), groupEndpointsByController(listOf(orphan)).keys)
    }

    @Test
    fun `filter matches method, path, handler and controller case-insensitively`() {
        val e = endpoint("DELETE", "/api/orders/{id}", "cancel", "OrderController")

        assertTrue(matchesFilter(e, "orders"))
        assertTrue(matchesFilter(e, "delete"))
        assertTrue(matchesFilter(e, "CANCEL"))
        assertTrue(matchesFilter(e, "ordercontroller"))
        assertTrue(matchesFilter(e, "  "))
        assertFalse(matchesFilter(e, "products"))
    }

    @Test
    fun `confirm label states the request count`() {
        assertEquals("Generate 8 requests", confirmLabel(8))
        assertEquals("Generate 1 request", confirmLabel(1))
        assertEquals("Generate 0 requests", confirmLabel(0))
    }

    @Test
    fun `diff tree nests added and removed files under their folders`() {
        val diff = CollectionDiff(
            added = listOf("requests/api/products/id/image/post-api-products-id-image.bru", "requests/health/get-health.bru"),
            removed = listOf("requests/api/orders/id/delete-api-orders-id.bru"),
            unchanged = List(7) { "requests/x$it.bru" }
        )

        assertEquals("+2  -1  =7", diffSummary(diff))

        val root = buildDiffTree(diff)
        val requests = root.children.single()
        assertEquals("requests", requests.name)
        assertNull(requests.change)
        assertEquals(listOf("api", "health"), requests.children.map { it.name })

        val orders = requests.children[0].children.first { it.name == "orders" }
        val removed = orders.children.single().children.single()
        assertEquals("delete-api-orders-id.bru", removed.name)
        assertEquals(DiffChange.REMOVED, removed.change)

        val health = requests.children[1].children.single()
        assertEquals(DiffChange.ADDED, health.change)
    }

    @Test
    fun `diff tree is empty when nothing changes`() {
        val root = buildDiffTree(CollectionDiff(emptyList(), emptyList(), listOf("a.bru")))
        assertTrue(root.children.isEmpty())
    }
}
