package org.arend.ui.impl.session

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.util.EditorUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.JBPopupListener
import com.intellij.openapi.ui.popup.LightweightWindowEvent
import com.intellij.ui.ColoredListCellRenderer
import org.arend.ArendIcons
import org.arend.core.definition.Definition
import org.arend.extImpl.ui.DelegateQuery
import org.arend.extImpl.ui.SimpleQuery
import org.arend.ui.ArendDialog
import javax.swing.JList
import javax.swing.ListSelectionModel

class ArendEditorSession(private val project: Project, private val editor: Editor) : ComponentSession() {
    private var singleListRequest: ListRequest<Any>? = null

    private class ListRequest<T>(val message: String?, val list: List<T>, val default: T?) {
        val query = DelegateQuery<T>()
    }

    override val allItems: List<ComponentSessionItem>
        get() {
            mergeSingleListRequest()
            return super.allItems
        }

    private fun mergeSingleListRequest() {
        val singleListRequest = singleListRequest
        if (singleListRequest != null) {
            singleListRequest.query.setDelegate(comboBoxQuery(0, singleListRequest.message, singleListRequest.list, singleListRequest.default))
            this.singleListRequest = null
        }
    }

    override fun <T> listQuery(message: String?, options: List<T>, defaultOption: T?) =
        if (singleListRequest == null && items.isEmpty()) {
            val request = ListRequest(message, options, defaultOption)
            val query = request.query
            @Suppress("UNCHECKED_CAST")
            singleListRequest = request as ListRequest<Any>
            query
        } else {
            mergeSingleListRequest()
            super.listQuery(message, options, defaultOption)
        }

    private fun <T> showPopup(request: ListRequest<T>) {
        val query = SimpleQuery<T>()
        request.query.setDelegate(query)

        val builder = JBPopupFactory.getInstance().createPopupChooserBuilder(request.list)
            .setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
            .setFont(EditorUtil.getEditorFont())
            .setRenderer(object : ColoredListCellRenderer<T>() {
                override fun customizeCellRenderer(list: JList<out T>, value: T, index: Int, selected: Boolean, hasFocus: Boolean) {
                    if (value is Definition) {
                        icon = ArendIcons.definitionToIcon(value)
                    }
                    append(value.toString())
                }
            })
            .setItemChosenCallback {
                query.result = it
                endSession(true)
            }
        if (callback != null) {
            builder.addListener(object : JBPopupListener {
                override fun onClosed(event: LightweightWindowEvent) {
                    if (!event.isOk) {
                        endSession(false)
                    }
                }
            })
        }
        if (request.message != null) builder.setTitle(request.message)
        builder.createPopup().showInBestPositionFor(editor)
    }

    override fun doStart() {
        val singleListRequest = singleListRequest
        if (singleListRequest != null) {
            if (items.isEmpty()) {
                showPopup(singleListRequest)
                return
            } else {
                mergeSingleListRequest()
            }
        }

        endSession(ArendDialog(project, description, null, component, focused).showAndGet())
    }
}