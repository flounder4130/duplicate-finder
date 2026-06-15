package finder.ui.swing

import finder.*
import finder.indexing.*
import finder.similarity.similarity
import finder.ui.sort.SortBy
import java.awt.*
import javax.swing.*

const val DEFAULT_FONT_SIZE = 16
val MONOSPACE_FONT = Font("Monospaced", Font.PLAIN, DEFAULT_FONT_SIZE)

class SwingUi(
    val report: DuplicateFinderReport,
    val options: DuplicateFinderOptions,
    val index: Index,
) {
    val listsData = ListsData(report.duplicates, options)

    val referenceChunkList = ChunkList(listsData.referenceChunksListModel).apply {
        cellRenderer = object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(
                list: JList<*>?,
                value: Any,
                index: Int,
                isSelected: Boolean,
                cellHasFocus: Boolean
            ): Component {
                val component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
                if (value is Chunk) {
                    val nDuplicates = listsData.report[value]?.size ?: 0
                    text = "($nDuplicates) ${value.preview}"
                }
                return component
            }
        }
        addListSelectionListener {
            if (!it.valueIsAdjusting && selectedValue != null) {
                listsData.showDuplicatesFor(selectedValue)
                referenceChunkPreview.document = heatMapDocument(
                    selectedValue,
                    report.duplicates[selectedValue] ?: error("No duplicates for key $selectedValue"),
                    options
                )
            }
        }
    }

    val duplicateChunkList = ChunkList(listsData.duplicateChunksListModel).apply {
        cellRenderer = object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(
                list: JList<*>?,
                value: Any,
                index: Int,
                isSelected: Boolean,
                cellHasFocus: Boolean
            ): Component {
                val component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
                if (value is Chunk) {
                    val similarity = value.similarity(referenceChunkList.selectedValue, options)
                    text = "$similarity% ${value.preview}"
                }
                return component
            }
        }
        addListSelectionListener {
            if (!it.valueIsAdjusting) duplicateChunkPreview.text = selectedValue?.content ?: ""
        }
    }

    val referenceChunkPreview = ScrollablePreview()
    val duplicateChunkPreview = ScrollablePreview()

    val listsPane = JSplitPane(
        JSplitPane.HORIZONTAL_SPLIT,
        JScrollPane(referenceChunkList),
        JScrollPane(duplicateChunkList)
    ).apply {
        resizeWeight = 0.5
        border = null
    }

    val previewsPane = JPanel().apply {
        layout = GridLayout(2, 1)
        add(referenceChunkPreview)
        add(duplicateChunkPreview)
    }

    val mainSplitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listsPane, previewsPane).apply {
        resizeWeight = 0.25
        border = null
    }

    val toolbar: JPanel = JPanel().apply {
        layout = GridBagLayout()
        val sortByLabel = JLabel("Sort by:").apply { horizontalAlignment = SwingConstants.RIGHT }
        val sortByComboBox = JComboBox(SortBy.entries.toTypedArray()).apply {
            addActionListener {
                referenceChunkPreview.text = ""
                listsData.sort(selectedItem as SortBy)
            }
        }
        val fontSizeLabel = JLabel("Font size:").apply { horizontalAlignment = SwingConstants.RIGHT }
        val fontSizeSlider = FontSizeSlider(
            referenceChunkList,
            duplicateChunkList,
            referenceChunkPreview.textPane,
            duplicateChunkPreview.textPane,
        )

        val showInClustersLabel = JLabel("Show in clusters:").apply { horizontalAlignment = SwingConstants.RIGHT }
        val clusterCheckBox = JCheckBox().apply {
            isSelected = true
            addActionListener { listsData.showInClusters(isSelected) }
        }

        val textSearchButton = JButton("Text search").apply {
            toolTipText = "Fuzzy search based on text query"
            addActionListener { TextSearchDialog(frame, fontSizeSlider.value, options, index).isVisible = true }
        }

        add(sortByLabel, toolbarPosition(0))
        add(sortByComboBox, toolbarPosition(1))
        add(showInClustersLabel, toolbarPosition(2))
        add(clusterCheckBox, toolbarPosition(3))
        add(fontSizeLabel, toolbarPosition(4))
        add(fontSizeSlider, toolbarPosition(5))
        add(textSearchButton, toolbarPosition(6))
    }

    val frame = JFrame().apply {
        layout = BorderLayout()
        add(toolbar, BorderLayout.NORTH)
        add(mainSplitPane, BorderLayout.CENTER)

        defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        val screenSize = Toolkit.getDefaultToolkit().screenSize
        size = Dimension(screenSize.width, screenSize.height)
        extendedState = JFrame.MAXIMIZED_BOTH
    }

    fun show() {
        frame.isVisible = true
    }
}

private fun toolbarPosition(x: Int) = GridBagConstraints().apply {
    fill = GridBagConstraints.HORIZONTAL
    weightx = 1.0
    insets = Insets(10, 10, 10, 10)
    gridx = x
    gridy = 0
}
