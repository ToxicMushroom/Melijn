package me.melijn.melijnbot.internals

class ConsoleColor {
    companion object {
        const val RESET = "\u001B[0m"
        const val BLACK = "\u001B[30m"
        const val RED = "\u001B[31m"
        const val GREEN = "\u001B[32m"
        const val YELLOW = "\u001B[33m"
        const val BLUE = "\u001B[34m"
        const val PURPLE = "\u001B[35m"
        const val CYAN = "\u001B[36m"
        const val WHITE = "\u001B[37m"
        const val ORANGE = "\u001B[38;2;255;140;0m"

        const val BLACK_BACKGROUND = "\u001B[40m"
        const val RED_BACKGROUND = "\u001B[41m"
        const val GREEN_BACKGROUND = "\u001B[42m"
        const val YELLOW_BACKGROUND = "\u001B[43m"
        const val BLUE_BACKGROUND = "\u001B[44m"
        const val PURPLE_BACKGROUND = "\u001B[45m"
        const val CYAN_BACKGROUND = "\u001B[46m"
        const val WHITE_BACKGROUND = "\u001B[47m"
    }
}
