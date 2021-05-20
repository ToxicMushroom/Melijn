package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.message.sendSyntax
import org.mariuszgromada.math.mxparser.Expression

class CalculateCommand : AbstractCommand("command.calculate") {

    init {
        id = 220
        name = "calculate"
        aliases = arrayOf("calc", "calculator", "math")
        commandCategory = CommandCategory.UTILITY
    }

    override suspend fun execute(context: ICommandContext) {
        if (context.rawArg.isBlank()) {
            sendSyntax(context)
            return
        }

        var exp = try {
            Expression(context.rawArg).calculate().toString()
        } catch (t: Throwable) {
            "error"
        }
        exp = if (exp.endsWith(".0")) exp.dropLast(2) else exp
        sendRsp(context, "Result: $exp")
    }
}