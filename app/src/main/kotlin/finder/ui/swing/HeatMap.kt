package finder.ui.swing

import finder.model.HeatMap
import java.awt.Color
import javax.swing.JTextPane
import javax.swing.text.*

fun heatMapDocument(heatMap: HeatMap): Document = JTextPane().document.apply {
    heatMap.content.forEachIndexed { index, char ->
        val attributes = SimpleAttributeSet()
        StyleConstants.setForeground(attributes, Color.getHSBColor(heatMap.scores[index] * 0.33f, 1.0f, 0.75f))
        insertString(length, char.toString(), attributes)
    }
}