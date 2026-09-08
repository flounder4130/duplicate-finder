package finder.ui.swing

import javax.swing.*

class ChunkList<T>(model: DefaultListModel<T>): JList<T>(model) {
    init {
        selectionMode = ListSelectionModel.SINGLE_SELECTION
        visibleRowCount = -1
        font = MONOSPACE_FONT
    }
}