import com.codeteam.GenerateBrunoCollectionAction
import com.codeteam.writer.CollectionDiff
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.nio.file.Path

class GenerateBrunoCollectionActionTest : BasePlatformTestCase() {

    fun `test success notification summarizes the diff and offers follow-up actions`() {
        val diff = CollectionDiff(
            added = listOf("a.bru", "b.bru", "c.bru"),
            removed = listOf("d.bru"),
            unchanged = listOf("e.bru")
        )

        val notification = GenerateBrunoCollectionAction()
            .successNotification(project, 8, Path.of("/tmp/shop-api"), diff)

        assertEquals("Bruno collection generated", notification.title)
        assertEquals("8 requests in shop-api/ (+3, -1)", notification.content)
        assertEquals(listOf("Open folder", "Settings"), notification.actions.map { it.templateText })
    }

    fun `test success notification uses singular for one request`() {
        val notification = GenerateBrunoCollectionAction()
            .successNotification(project, 1, Path.of("/tmp/shop-api"), CollectionDiff(listOf("a.bru"), emptyList(), emptyList()))

        assertEquals("1 request in shop-api/ (+1, -0)", notification.content)
    }
}
