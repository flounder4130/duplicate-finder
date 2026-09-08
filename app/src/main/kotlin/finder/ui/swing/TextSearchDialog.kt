package finder.ui.swing

import finder.*
import finder.model.*
import finder.ui.preview
import finder.ui.similarityPercent
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
    private val finder: DuplicateFinder,
) : JDialog(parent, "Text search", true) {

    private val queryPane = JTextPane().apply {
        font = MONOSPACE_FONT.deriveFont(fontSize.toFloat())
        minimumSize = MIN_PANE_SIZE
        preferredSize = PREFERRED_PANE_SIZE
        resetStyleOnFocus()
    }

    private val similaritySlider = JSlider(50, 100, (finder.options.analysis.minSimilarity * 100).toInt()).apply {
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

    private val resultsListModel = DefaultListModel<DuplicateMatch>()
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
                if (value is DuplicateMatch) {
                    text = "${value.similarityPercent}% ${value.chunk.preview}"
                }
                return component
            }
        }

        addListSelectionListener { matchPane.text = selectedValue?.chunk?.content ?: "" }
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
        val results = finder.findFuzzy(FuzzyQuery.Text(queryPane.text), similaritySlider.value / 100.0)
        queryPane.document = heatMapDocument(finder.heatMap(results.referenceContent, results.matches))
        resultsListModel.clear()
        resultsListModel.addAll(results.matches)
        val match = results.matches.firstOrNull { it.chunk.content == results.referenceContent }
        matchPane.text = match?.chunk?.content ?: ""
    }
}

private fun JTextPane.resetStyleOnFocus() = addFocusListener(object : FocusListener {
    override fun focusGained(e: FocusEvent?) {
        styledDocument.setCharacterAttributes(
            0, document.length, SimpleAttributeSet.EMPTY, true
        )
    }

    override fun focusLost(e: FocusEvent?) {}
})