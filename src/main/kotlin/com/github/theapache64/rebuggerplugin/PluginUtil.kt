package com.github.theapache64.rebuggerplugin

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.command.UndoConfirmationPolicy
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import java.util.concurrent.atomic.AtomicReference


inline fun <T> runOnEdtWithWriteLock(crossinline f: () -> T): T =
    runOnEdt {
        ApplicationManager.getApplication().runWriteAction(Computable { f() })
    }


fun Document.executeCommand(project: Project, description: String? = null, callback: Document.() -> Unit) {
    runOnEdtWithWriteLock {
        val command = { callback(this) }
        CommandProcessor.getInstance().executeCommand(project, command, description, null,
            UndoConfirmationPolicy.DEFAULT, this)
    }
}


inline fun <T> runOnEdt(crossinline f: () -> T): T {
    val result = AtomicReference<T>()
    ApplicationManager.getApplication().invokeAndWait {
        result.set(f())
    }
    return result.get()
}