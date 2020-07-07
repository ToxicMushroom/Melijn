package me.melijn.melijnbot.commands.developer

import groovy.lang.GroovyShell
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.utils.message.sendRsp

class EvalCommand : AbstractCommand("command.eval") {

    private val groovyShell: GroovyShell

    init {
        id = 22
        name = "eval"
        aliases = arrayOf("evaluate")
        commandCategory = CommandCategory.DEVELOPER
        groovyShell = GroovyShell()
    }


    override suspend fun execute(context: CommandContext) {
        groovyShell.setProperty("context", context)

        try {
            val result = groovyShell.evaluate(context.rawArg)
            sendRsp(context, "Success:\n```$result```")
        } catch (t: Throwable) {
            sendRsp(context, "ERROR:\n```${t.message}```")
        }
    }
}