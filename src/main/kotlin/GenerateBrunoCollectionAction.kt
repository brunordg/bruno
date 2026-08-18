package com.codeteam

import com.codeteam.model.Endpoint
import com.codeteam.scanner.ControllerScanner
import com.codeteam.settings.BrunoGeneratorSettings
import com.codeteam.settings.BrunoGeneratorState
import com.codeteam.ui.EndpointSelectionDialog
import com.codeteam.writer.BrunoWriter
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import java.nio.file.Path

class GenerateBrunoCollectionAction : AnAction(), DumbAware {

    private val scanner = ControllerScanner()
    private val writer = BrunoWriter()

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project?.basePath != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val basePath = project.basePath ?: return

        object : Task.Backgroundable(project, "Scanning Spring controllers", false) {
            private lateinit var endpoints: List<Endpoint>
            private lateinit var settings: BrunoGeneratorState

            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = true
                indicator.text = "Scanning Spring controllers…"
                settings = BrunoGeneratorSettings.getInstance(project).state
                endpoints = scanner.scan(project)
            }

            override fun onSuccess() {
                continueAfterScan(project, basePath, endpoints, settings)
            }

            override fun onThrowable(error: Throwable) {
                notify(project, "Failed to scan controllers", error.message ?: "Unknown error", NotificationType.ERROR)
            }
        }.queue()
    }

    private fun continueAfterScan(
        project: Project,
        basePath: String,
        endpoints: List<Endpoint>,
        settings: BrunoGeneratorState
    ) {
        if (endpoints.isEmpty()) {
            notify(project, "No endpoints found", "No @RestController endpoints were found.", NotificationType.WARNING)
            return
        }

        val projectRoot = Path.of(basePath)
        val outputRoot = resolveOutputRoot(projectRoot, settings.outputDirectory)

        val dialog = EndpointSelectionDialog(project, endpoints, projectRoot, settings, outputRoot, writer)
        if (!dialog.showAndGet()) return
        val selected = dialog.selectedEndpoints()
        if (selected.isEmpty()) {
            notify(project, "Nothing selected", "No endpoints were selected; generation cancelled.", NotificationType.INFORMATION)
            return
        }

        object : Task.Backgroundable(project, "Generating Bruno collection", false) {
            private lateinit var brunoRoot: Path

            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = true
                indicator.text = "Writing Bruno collection…"
                brunoRoot = writer.writeCollection(projectRoot, selected, settings, outputRoot)
                LocalFileSystem.getInstance().refreshAndFindFileByNioFile(brunoRoot)
            }

            override fun onSuccess() {
                notify(
                    project,
                    "Bruno collection generated",
                    "Generated ${selected.size} requests in ${brunoRoot.fileName}/",
                    NotificationType.INFORMATION
                )
            }

            override fun onThrowable(error: Throwable) {
                notify(project, "Failed to generate Bruno collection", error.message ?: "Unknown error", NotificationType.ERROR)
            }
        }.queue()
    }

    private fun notify(project: Project, title: String, content: String, type: NotificationType) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Bruno Generator")
            .createNotification(title, content, type)
            .notify(project)
    }

    private fun resolveOutputRoot(projectRoot: Path, outputDirectory: String): Path {
        val trimmed = outputDirectory.trim()
        if (trimmed.isBlank()) return projectRoot
        val configured = Path.of(trimmed)
        return if (configured.isAbsolute) configured else projectRoot.resolve(configured)
    }
}
