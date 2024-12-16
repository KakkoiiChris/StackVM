/*      ___  _______  ___    _______
 *     |   ||       ||   |  |       |
 *     |   ||   _   ||   |  |_     _|
 *     |   ||  | |  ||   |    |   |
 *  ___|   ||  |_|  ||   |___ |   |
 * |       ||       ||       ||   |
 * |_______||_______||_______||___|
 *         SCRIPTING LANGUAGE
 */
package kakkoiichris.stackvm.util

/**
 * The 'Lightning Bolt' emoji, used in many places in the UI.
 */
const val JOLT = '⚡'

/**
 * The double horizontal box drawing character, used to underline locations of errors.
 */
const val UNDERLINE = '═'

/**
 * A vertical box drawing character.
 */
private const val VERTICAL = "│"

/**
 *A horizontal box drawing character.
 */
private const val HORIZONTAL = "─"

/**
 * An upper left corner box drawing character.
 */
private const val UP_LEFT = "╭"

/**
 * An upper right corner box drawing character.
 */
private const val UP_RIGHT = "╮"

/**
 * A lower left corner box drawing character.
 */
private const val DOWN_LEFT = "╰"

/**
 * A lower right corner box drawing character.
 */
private const val DOWN_RIGHT = "╯"

/**
 * A left facing T box drawing character.
 */
private const val T_LEFT = "├"

/**
 * A right facing T box drawing character.
 */
private const val T_RIGHT = "┤"

/**
 * Creates a box of best fit around the receiving string.
 *
 * When the string has multiple lines in it, the box will fit the width of the longest line. All lines are left aligned.
 *
 * If any of the lines are blank, a horizontal dividing line will be put in its place.
 *
 * @receiver A [String], preferably of length 1 or more, possibly containing multiple newline characters
 *
 * @return A copy of the string with a box around it
 */
fun String.wrapBox() = buildString {
    val lines = this@wrapBox.lines()

    val maxWidth = lines.maxOf { it.length }

    appendLine("$UP_LEFT${HORIZONTAL.repeat(maxWidth + 2)}$UP_RIGHT")

    for (line in lines) {
        if (line.isEmpty()) {
            appendLine("$T_LEFT${HORIZONTAL.repeat(maxWidth + 2)}$T_RIGHT")
        }
        else {
            appendLine("$VERTICAL ${line.padEnd(maxWidth)} $VERTICAL")
        }
    }

    append("$DOWN_LEFT${HORIZONTAL.repeat(maxWidth + 2)}$DOWN_RIGHT")
}
