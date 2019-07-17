package me.melijn.melijnbot.objects.command

import me.melijn.melijnbot.objects.translation.Translateable
import net.dv8tion.jda.api.Permission

const val PREFIX_PLACE_HOLDER = "%PREFIX%"
abstract class ICommand {

    var name: Translateable = Translateable("empty")
    var id: Int = 0
    var description: Translateable = Translateable("empty")
    var syntax: Translateable = Translateable("empty")
    var help: Translateable = Translateable("empty")
    var commandCategory: CommandCategory = CommandCategory.DEVELOPER
    var aliases: Array<Translateable> = arrayOf()
    var discordPermissions: Array<Permission> = arrayOf()
    var runConditions: Array<RunCondition> = arrayOf()

    protected abstract fun execute(context: CommandContext)
    public final fun run(context: CommandContext) {
        execute(context)
    }

    fun isCommandFor(input: String): Boolean {
        if (name.toString().equals(input, true)) return true
        for (alias in aliases)
            if (alias.equals(input, ignoreCase = true)) return true
        return false
    }
}