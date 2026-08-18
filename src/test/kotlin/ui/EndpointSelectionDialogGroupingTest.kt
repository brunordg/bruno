package ui

import com.codeteam.model.BodyKind
import com.codeteam.model.Endpoint
import com.codeteam.ui.groupEndpointsByTopPath
import org.junit.Assert.assertEquals
import org.junit.Test

class EndpointSelectionDialogGroupingTest {

    private fun endpoint(path: String, handlerName: String) = Endpoint(
        httpMethod = "GET",
        path = path,
        handlerName = handlerName,
        hasRequestBody = false,
        bodyKind = BodyKind.NONE
    )

    @Test
    fun `groups endpoints by their first path segment`() {
        val widgets = endpoint("/widgets", "listWidgets")
        val widgetById = endpoint("/widgets/{id}", "getWidget")
        val orders = endpoint("/orders", "listOrders")

        val groups = groupEndpointsByTopPath(listOf(widgets, widgetById, orders))

        assertEquals(setOf("widgets", "orders"), groups.keys)
        assertEquals(setOf(widgets, widgetById), groups.getValue("widgets").toSet())
        assertEquals(listOf(orders), groups.getValue("orders"))
    }

    @Test
    fun `falls back to root for blank paths`() {
        val rootEndpoint = endpoint("/", "health")

        val groups = groupEndpointsByTopPath(listOf(rootEndpoint))

        assertEquals(setOf("root"), groups.keys)
        assertEquals(listOf(rootEndpoint), groups.getValue("root"))
    }
}
