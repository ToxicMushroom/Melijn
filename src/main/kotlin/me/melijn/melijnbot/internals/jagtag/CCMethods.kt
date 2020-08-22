package me.melijn.melijnbot.internals.jagtag

import com.jagrosh.jagtag.Method

object CCMethods {
    fun getMethods(): List<Method> = listOf(
        Method("argsCount", { env ->
            val args: List<String> = env.getReifiedX("args")
            args.size.toString()
        }),
        Method("arg", complex = { env, input ->
            val args: List<String> = env.getReifiedX("args")
            val arg = input[0].toIntOrNull() ?: 0
            if (args.size > arg) args[arg]
            else "null"
        }),
        Method("rawArg", { env ->
            env.getReifiedX("rawArg")
        })
    )
}