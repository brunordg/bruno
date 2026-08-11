package writer

import com.codeteam.writer.BrunoWriter
import org.junit.Assert.assertEquals
import org.junit.Test

class BrunoWriterTest {

    private val writer = BrunoWriter()

    @Test
    fun `slug converts camelCase handler names to kebab-case`() {
        assertEquals("get-widget-by-id", writer.slug("getWidgetById"))
    }

    @Test
    fun `slug falls back to request when the name has no usable characters`() {
        assertEquals("request", writer.slug("   "))
    }

    @Test
    fun `toBrunoUrlPath converts curly-brace path variables to colon syntax`() {
        assertEquals("/api/widgets/:id", writer.toBrunoUrlPath("/api/widgets/{id}"))
    }

    @Test
    fun `toBrunoUrlPath strips Spring inline regex constraints from path variables`() {
        assertEquals("/api/widgets/:id", writer.toBrunoUrlPath("/api/widgets/{id:[0-9]+}"))
    }

    @Test
    fun `toBrunoUrlPath leaves paths without variables unchanged`() {
        assertEquals("/api/widgets", writer.toBrunoUrlPath("/api/widgets"))
    }

    @Test
    fun `fakeValue for an id-like numeric field uses randomInt`() {
        assertEquals("{{\$randomInt}}", writer.fakeValue("id", "Long"))
    }

    @Test
    fun `fakeValue for an id-like string field uses randomUUID`() {
        assertEquals("{{\$randomUUID}}", writer.fakeValue("userId", "String"))
    }

    @Test
    fun `fakeValue for an email-like field uses randomEmail`() {
        assertEquals("{{\$randomEmail}}", writer.fakeValue("contactEmail", "String"))
    }

    @Test
    fun `fakeValue for a plain string field uses randomLoremWord`() {
        assertEquals("{{\$randomLoremWord}}", writer.fakeValue("notes", "String"))
    }

    @Test
    fun `fakeValue falls back to a variable reference for unrecognized boolean fields`() {
        assertEquals("{{flag}}", writer.fakeValue("flag", "boolean"))
    }
}
