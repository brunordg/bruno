package com.codeteam

import com.codeteam.scanner.ControllerScanner
import com.codeteam.settings.BrunoGeneratorSettings
import com.codeteam.writer.BrunoWriter
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbAware
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

        ApplicationManager.getApplication().executeOnPooledThread {
            runCatching {
                val settings = BrunoGeneratorSettings.getInstance(project).state
                val endpoints = scanner.scan(project)
                val projectRoot = Path.of(basePath)
                val outputRoot = resolveOutputRoot(projectRoot, settings.outputDirectory)
                val brunoRoot = writer.writeCollection(projectRoot, endpoints, settings, outputRoot)
                LocalFileSystem.getInstance()
                    .refreshAndFindFileByNioFile(brunoRoot)
                endpoints to brunoRoot
            }.onSuccess { (endpoints, brunoRoot) ->
                NotificationGroupManager.getInstance()
                    .getNotificationGroup("Bruno Generator")
                    .createNotification(
                        "Bruno collection generated",
                        "Generated ${endpoints.size} requests in ${brunoRoot.fileName}/",
                        NotificationType.INFORMATION
                    )
                    .notify(project)
            }.onFailure { t ->
                NotificationGroupManager.getInstance()
                    .getNotificationGroup("Bruno Generator")
                    .createNotification(
                        "Failed to generate Bruno collection",
                        t.message ?: "Unknown error",
                        NotificationType.ERROR
                    )
                    .notify(project)
            }
        }
    }

    private fun resolveOutputRoot(projectRoot: Path, outputDirectory: String): Path {
        val trimmed = outputDirectory.trim()
        if (trimmed.isBlank()) return projectRoot
        val configured = Path.of(trimmed)
        return if (configured.isAbsolute) configured else projectRoot.resolve(configured)
    }
}