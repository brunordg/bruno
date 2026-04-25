package com.codeteam

import com.codeteam.scanner.ControllerScanner
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

        try {
            ApplicationManager.getApplication().executeOnPooledThread {
                val endpoints = scanner.scan(project)
                writer.writeCollection(Path.of(basePath), endpoints)
                LocalFileSystem.getInstance()
                    .refreshAndFindFileByNioFile(Path.of(basePath).resolve("bruno"))

                NotificationGroupManager.getInstance()
                    .getNotificationGroup("Bruno Generator")
                    .createNotification(
                        "Bruno collection generated",
                        "Generated ${endpoints.size} requests in bruno/",
                        NotificationType.INFORMATION
                    )
                    .notify(project)
            }
        } catch (e: Exception) {
            NotificationGroupManager.getInstance()
                .getNotificationGroup("Bruno Generator")
                .createNotification(
                    "Failed to generate Bruno collection",
                    e.message ?: "Unknown error",
                    NotificationType.ERROR
                )
                .notify(project)
        }
    }
}