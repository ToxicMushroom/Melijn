package me.melijn.melijnbot.objects.command

import me.melijn.melijnbot.objects.translation.Translateable
import net.dv8tion.jda.api.Permission

const val PREFIX_PLACE_HOLDER = "%PREFIX%"

abstract class AbstractCommand {

    var name: String = ""
    var id: Int = 0
    var description: Translateable = Translateable("empty")
    var syntax: Translateable = Translateable("empty")
    var help: Translateable = Translateable("empty")
    var commandCategory: CommandCategory = CommandCategory.DEVELOPER
    var aliases: Array<String> = arrayOf()
    var discordPermissions: Array<Permission> = arrayOf()
    var runConditions: Array<RunCondition> = arrayOf()
    var children: Array<AbstractCommand> = arrayOf()

    protected abstract fun execute(context: CommandContext)
    public final fun run(context: CommandContext, commandPartInvoke: Int = 1) {
        if (context.commandParts.size > commandPartInvoke + 1 && children.isNotEmpty()) {
            for (child in children) {
                if (child.isCommandFor(context.commandParts[commandPartInvoke + 1])) {
                    child.run(context, commandPartInvoke + 1)
                    return
                }

            }
        }
        execute(context)
    }

    fun isCommandFor(input: String): Boolean {
        if (name.equals(input, true)) return true
        for (alias in aliases)
            if (alias.equals(input, true)) return true
        return false
    }
}