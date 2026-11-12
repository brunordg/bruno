package golden

import com.codeteam.scanner.ControllerScanner
import com.codeteam.settings.BrunoEnvironment
import com.codeteam.settings.BrunoGeneratorState
import com.fasterxml.jackson.annotation.JsonProperty
import com.codeteam.writer.BrunoWriter
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.projectRoots.impl.JavaAwareProjectJdkTableImpl
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.testFramework.PsiTestUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import io.swagger.v3.oas.annotations.Operation
import jakarta.validation.constraints.NotNull
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.nio.file.Path

/**
 * End-to-end black box: Spring source in -> Bruno collection on disk out, compared
 * file by file with `src/test/resources/golden/widgets`.
 *
 * When the output changes on purpose, review `build/golden-actual/bruno-widgets` (written on
 * every run) and copy it over the golden folder.
 */
class ScannerToWriterGoldenTest : BasePlatformTestCase() {

    private val goldenDir = Path.of("src/test/resources/golden/widgets")
    private val actualDir = Path.of("build/golden-actual")

    @Suppress("DEPRECATION")
    override fun setUp() {
        super.setUp()
        // Same fixture setup as ControllerScannerTest (see the comment there).
        val jdkHome = JavaAwareProjectJdkTableImpl.getInstanceEx().internalJdk.homePath
        ModuleRootModificationUtil.addModuleLibrary(module, "java.base", listOf("jrt://$jdkHome!/java.base"), emptyList())
        PsiTestUtil.addLibrary(module, PathManager.getJarPathForClass(RestController::class.java)!!)
        PsiTestUtil.addLibrary(module, PathManager.getJarPathForClass(NotNull::class.java)!!)
        PsiTestUtil.addLibrary(module, PathManager.getJarPathForClass(MultipartFile::class.java)!!)
        PsiTestUtil.addLibrary(module, PathManager.getJarPathForClass(JsonProperty::class.java)!!)
        PsiTestUtil.addLibrary(module, PathManager.getJarPathForClass(Operation::class.java)!!)
    }

    fun `test a widget controller generates exactly the golden collection`() {
        addWidgetSources()
        val endpoints = ControllerScanner().scan(project)

        val settings = BrunoGeneratorState().apply {
            collectionName = "widgets"
            environments = mutableListOf(BrunoEnvironment("dev", "http://localhost:8080"))
            defaultEnvironmentName = "dev"
        }
        val projectRoot = Files.createTempDirectory("golden-project")
        val brunoRoot = BrunoWriter().writeCollection(projectRoot, endpoints, settings, outputRoot = actualDir)

        assertSameTree(goldenDir, brunoRoot)
    }

    private fun addWidgetSources() {
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
                    @RequestHeader("X-Trace-Id") String traceId
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
                @Min(1) @Max(9)
                public int quantity;
            }
            """.trimIndent()
        )
    }

    private fun assertSameTree(expectedRoot: Path, actualRoot: Path) {
        val expectedFiles = relativeFiles(expectedRoot)
        val actualFiles = relativeFiles(actualRoot)
        assertEquals("Files generated differ from golden", expectedFiles, actualFiles)

        for (file in expectedFiles) {
            val expected = Files.readString(expectedRoot.resolve(file))
            val actual = Files.readString(actualRoot.resolve(file))
            assertEquals("Content differs from golden: $file", expected, actual)
        }
    }

    private fun relativeFiles(root: Path): List<String> {
        Files.walk(root).use { paths ->
            return paths.filter { Files.isRegularFile(it) }
                .map { root.relativize(it).toString() }
                .sorted()
                .toList()
        }
    }
}
