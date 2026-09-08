package finder.ui.swing

import javax.swing.*
import javax.swing.text.Document

class ScrollablePreview(val textPane: JTextPane = JTextPane()) : JScrollPane(textPane) {
    init {
        textPane.apply {
            isEditable = false
            border = null
            font = MONOSPACE_FONT
        }
    }

    var document: Document
        get() = textPane.document
        set(d) {
            textPane.document = d
        }

    var text: String
        get() = textPane.text
        set(t) {
            textPane.text = t
        }
}