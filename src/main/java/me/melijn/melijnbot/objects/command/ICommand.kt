package me.melijn.melijnbot.objects.command

import net.dv8tion.jda.api.Permission

abstract class ICommand {

    var name: String = ""
    var id: Int = 0
    var description: String = ""
    var syntax: String = ""
    var help: String = ""
    var commandCategory: CommandCategory = CommandCategory.DEVELOPER
    var aliases: Array<String> = arrayOf()
    var discordPermissions: Array<Permission> = arrayOf()
    var runConditions: Array<CommandCondition> = arrayOf()

    protected abstract fun execute(context: CommandContext)
    public final fun run(context: CommandContext) {
        execute(context)
    }

    fun isCommandFor(input: String): Boolean {
        if (name.equals(input, true)) return true
        for (alias in aliases)
            if (alias.equals(input, ignoreCase = true)) return true
        return false
    }
}