package finder.ui.compose

import finder.ui.MenuItem

enum class FontSize(val size: Int): MenuItem {
    SMALL(10),
    MEDIUM(14),
    LARGE(18);

    override fun uiText() = name
}