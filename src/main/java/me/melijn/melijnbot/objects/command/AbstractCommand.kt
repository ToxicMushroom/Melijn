package me.melijn.melijnbot.objects.command

import me.melijn.melijnbot.enums.PermState
import me.melijn.melijnbot.objects.translation.Translateable
import net.dv8tion.jda.api.Permission

const val PREFIX_PLACE_HOLDER = "%PREFIX%"

abstract class AbstractCommand {

    var name: String = ""
    var id: Int = 0
    var description: Translateable = Translateable("empty")
    var syntax: String = PREFIX_PLACE_HOLDER + name
    var help: Translateable = Translateable("empty")
    var commandCategory: CommandCategory = CommandCategory.DEVELOPER
    var aliases: Array<String> = arrayOf()
    var discordPermissions: Array<Permission> = arrayOf()
    var runConditions: Array<RunCondition> = arrayOf()
    var children: Array<AbstractCommand> = arrayOf()

    protected abstract fun execute(context: CommandContext)
    public final fun run(context: CommandContext, commandPartInvoke: Int = 1) {
        context.commandOrder = ArrayList(context.commandOrder + this).toList()
        if (context.commandParts.size > commandPartInvoke + 1 && children.isNotEmpty()) {
            for (child in children) {
                if (child.isCommandFor(context.commandParts[commandPartInvoke + 1])) {
                    child.run(context, commandPartInvoke + 1)
                    return
                }

            }
        }

        val permission = context.commandOrder.joinToString(".")
        if (hasPermission(context, permission))
            execute(context)
        else
            sendMissingPermissionMessage(context, permission)
    }

    private fun sendMissingPermissionMessage(context: CommandContext, permission: String) {

    }

    private fun hasPermission(context: CommandContext, permission: String): Boolean {
        if (!context.isFromGuild) return true
        val authorId = context.author.idLong
        val userMap = context.daoManager.userPermissionWrapper.userPermissionCache.get(authorId).get()
        if (userMap.containsKey(permission) && userMap[permission] != PermState.DEFAULT) {
            return userMap[permission] == PermState.ALLOW
        }

        val guildMap = context.daoManager.rolePermissionWrapper



        return false
    }

    fun isCommandFor(input: String): Boolean {
        if (name.equals(input, true)) return true
        for (alias in aliases)
            if (alias.equals(input, true)) return true
        return false
    }
}