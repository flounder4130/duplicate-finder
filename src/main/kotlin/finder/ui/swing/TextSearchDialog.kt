package finder.ui.swing

import finder.*
import finder.indexing.*
import finder.similarity.similarity
import java.awt.*
import java.awt.event.*
import javax.swing.*
import javax.swing.text.SimpleAttributeSet

val MIN_PANE_SIZE = Dimension(400, 250)
val PREFERRED_PANE_SIZE = Dimension(400, 250)
val MIN_DIALOG_SIZE = Dimension(800, 700)
val PREFERRED_DIALOG_SIZE = Dimension(1000, 900)

class TextSearchDialog(
    parent: JFrame,
    fontSize: Int,
    private val options: DuplicateFinderOptions,
    private val index: Index,
) : JDialog(parent, "Text search", true) {

    private val queryPane = JTextPane().apply {
        font = MONOSPACE_FONT.deriveFont(fontSize.toFloat())
        minimumSize = MIN_PANE_SIZE
        preferredSize = PREFERRED_PANE_SIZE
        resetStyleOnFocus()
    }

    private val similaritySlider = JSlider(50, 100, (options.minSimilarity * 100).toInt()).apply {
        majorTickSpacing = 25
        minorTickSpacing = 5
        paintTicks = true
        paintLabels = true
        border = BorderFactory.createEmptyBorder(0, 10, 0, 50)
    }

    private val sliderPanel = JPanel(BorderLayout()).apply {
        add(JLabel("Min. similarity"), BorderLayout.WEST)
        add(similaritySlider, BorderLayout.CENTER)
        add(JButton("Find").apply {
            addActionListener { refresh() }
        }, BorderLayout.EAST)
    }

    private val matchPane = JTextPane().apply {
        font = MONOSPACE_FONT.deriveFont(fontSize.toFloat())
        minimumSize = MIN_PANE_SIZE
        preferredSize = PREFERRED_PANE_SIZE
    }

    private val resultsListModel = DefaultListModel<Chunk>()
    private val resultsList = JList(resultsListModel).apply {
        font = MONOSPACE_FONT.deriveFont(fontSize.toFloat())
        minimumSize = MIN_PANE_SIZE
        preferredSize = PREFERRED_PANE_SIZE

        cellRenderer = object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(
                list: JList<*>?,
                value: Any,
                index: Int,
                isSelected: Boolean,
                cellHasFocus: Boolean
            ): Component {
                val component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
                font = MONOSPACE_FONT.deriveFont(fontSize.toFloat())
                if (value is Chunk) {
                    val similarity = value.similarity(queryPane.text.toChunk(), options)
                    text = "$similarity% ${value.preview}"
                }
                return component
            }
        }

        addListSelectionListener { matchPane.text = selectedValue?.content ?: "" }
    }

    init {
        layout = GridBagLayout()
        minimumSize = MIN_DIALOG_SIZE
        preferredSize = PREFERRED_DIALOG_SIZE

        fun dialogPosition(y: Int, weight: Double = 1.0) = GridBagConstraints().apply {
            fill = GridBagConstraints.BOTH
            weightx = 1.0
            weighty = weight
            insets = Insets(10, 10, 10, 10)
            gridx = 0
            gridy = y
        }

        add(JLabel("Query:"), dialogPosition(0, weight = 0.0))
        add(JScrollPane(queryPane), dialogPosition(1))
        add(sliderPanel, dialogPosition(2, weight = 0.0))
        add(JScrollPane(resultsList), dialogPosition(3))
        add(JScrollPane(matchPane), dialogPosition(4))
    }

    private fun refresh() {
        val searchChunk = queryPane.text.toChunk()
        val results = findForChunk(searchChunk, index, options.withMinSimilarity(similaritySlider.value / 100.0))
        queryPane.document = heatMapDocument(searchChunk, results, options)
        resultsListModel.clear()
        resultsListModel.addAll(results)
        val match = results.firstOrNull { it.content == searchChunk.content }
        matchPane.text = match?.content ?: ""
    }
}

private fun String.toChunk() = LineChunk(this, "", LineCoordinates(0))

private fun JTextPane.resetStyleOnFocus() = addFocusListener(object : FocusListener {
    override fun focusGained(e: FocusEvent?) {
        styledDocument.setCharacterAttributes(
            0, document.length, SimpleAttributeSet.EMPTY, true
        )
    }

    override fun focusLost(e: FocusEvent?) {}
})
