package me.melijn.melijnbot.enums

enum class DiscordSize {
    Original,
    X64,
    X128,
    X256,
    X512,
    X1024,
    X2048;

    fun getParam(): String {
        return if (this == Original) ""
        else "?size=" + this.toString().drop(1)
    }
}