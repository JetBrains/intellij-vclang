package org.arend.toolWindow.errors

import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import org.arend.typechecking.error.ErrorService


class ArendMessagesService(private val project: Project) {
    var view: ArendMessagesView? = null
        private set
    var isErrorTextPinned: Boolean = false

    fun activate(project: Project, selectFirst: Boolean) {
        runInEdt {
            ToolWindowManager.getInstance(project).getToolWindow("Arend Errors")?.activate(if (selectFirst) Runnable {
                val service = project.service<ArendMessagesService>()
                val view = service.view
                if (view != null) {
                    view.update()
                    view.tree.selectFirst()
                }
            } else null, false, false)
        }
    }

    fun initView(toolWindow: ToolWindow) {
        view = ArendMessagesView(project, toolWindow)
    }

    fun update() {
        val view = view
        if (view == null) {
            if (project.service<ErrorService>().hasErrors) {
                activate(project, true)
            }
        } else {
            view.update()
        }
    }

    fun setActiveEditor() {
        view?.setActiveEditor()
    }

    fun updateErrorText() {
        view?.updateErrorText()
    }
}