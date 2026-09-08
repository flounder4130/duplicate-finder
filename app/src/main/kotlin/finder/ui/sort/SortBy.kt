package finder.ui.sort

import finder.ui.MenuItem

enum class SortBy(val description: String): MenuItem {
    MAX_DUPLICATES("Number of duplicates"),
    MAX_LENGTH("Length"),
    MAX_AVG_SIMILARITY("Average similarity");

    override fun uiText() = description

    override fun toString() = description
}