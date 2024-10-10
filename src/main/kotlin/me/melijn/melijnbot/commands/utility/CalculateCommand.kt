package me.melijn.melijnbot.commands.utility

import kotlinx.coroutines.delay
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.utils.message.sendSyntax

class CalculateCommand : AbstractCommand("command.calculate") {

    init {
        id = 220
        name = "calculate"
        aliases = arrayOf("calc", "calculator", "math")
        commandCategory = CommandCategory.UTILITY
        cooldown = 2000
    }

    override suspend fun execute(context: ICommandContext) {
        if (context.rawArg.isBlank()) {
            sendSyntax(context)
            return
        }

        context.initCooldown()
        val proc = Runtime.getRuntime().exec(arrayOf("qalc" , "--time", "100", context.rawArg))
        val pid = proc.pid()
        var t = 0
        while (t < 20) {
            if (proc.isAlive) {
                delay(100)
                t++
            } else {
                val resp = proc.inputStream.readAllBytes()
                context.reply("Resp: " + String(resp))
                return
            }
        }
        Runtime.getRuntime().exec("kill -9 ${pid}")
        val err = proc.errorStream.readAllBytes()
        context.reply("Err ($pid): " + String(err))
    }
}