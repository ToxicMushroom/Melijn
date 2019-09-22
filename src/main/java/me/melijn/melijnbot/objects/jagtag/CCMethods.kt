package me.melijn.melijnbot.objects.jagtag

import com.jagrosh.jagtag.Method

object CCMethods {
    fun getMethods(): List<Method> = listOf(
        Method("argsCount", { env ->
            val args: List<String> = env.getReifiedX("args")
            args.size.toString()
        }),
        Method("arg", complex = { env, input ->
            val args: List<String> = env.getReifiedX("args")
            args[input[0].toInt()]
        }),
        Method("rawArg", { env ->
            env.getReifiedX("rawArg")
        })
    )
}