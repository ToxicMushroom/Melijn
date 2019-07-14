package me.melijn.melijnbot.objects.command

import net.dv8tion.jda.api.Permission

abstract class ICommand {

    protected abstract var name: String
    protected abstract var id: Int
    protected var description: String = ""
    protected var syntax: String = ""
    protected var help: String = ""
    protected var commandCategory: CommandCategory = CommandCategory.DEVELOPER
    protected var aliases: Array<String> = arrayOf()
    protected var discordPermissions: Array<Permission> = arrayOf()
    protected var conditionsToPass: Array<CommandCondition> = arrayOf()

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