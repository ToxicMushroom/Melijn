package me.melijn.melijnbot.internals.jagtag

import com.jagrosh.jagtag.Method

object RegexMethods {
    fun getMethods(): List<Method> = listOf(
        Method(".", {
            "\\E.\\Q"
        }),

        Method(".*", {
            "\\E.*\\Q"
        }),

        Method(".+", {
            "\\E.*\\Q"
        }),

        Method("[a-zA-Z]", {
            "\\E[a-zA-Z]\\Q"
        }),

        Method("\\d", {
            "\\E\\d\\Q"
        }),

        Method("|", {
            "\\E|\\Q"
        }),

        Method("(", {
            "\\E(\\Q"
        }),

        Method(")", {
            "\\E)\\Q"
        }),

        Method("*", {
            "\\E*\\Q"
        }),

        Method("+", {
            "\\E+\\Q"
        }),

        Method("amount", { "*" }, { _, input ->
            "\\E{${input[0].toInt()}}\\Q"
        })
    )
}