package finder.ui.swing

import javax.swing.*

const val FONT_SIZE_RANGE = 16
const val HALF_FONT_SIZE_RANGE = FONT_SIZE_RANGE / 2

class FontSizeSlider(vararg targets: JComponent) : JSlider(
    DEFAULT_FONT_SIZE - HALF_FONT_SIZE_RANGE,
    DEFAULT_FONT_SIZE + HALF_FONT_SIZE_RANGE
) {
    init {
        majorTickSpacing = HALF_FONT_SIZE_RANGE
        minorTickSpacing = 1
        paintTicks = true
        paintLabels = true
        addChangeListener {
            val newSize = value.toFloat()
            targets.forEach { it.font = it.font.deriveFont(newSize) }
        }
    }
}