package me.melijn.melijnbot.objects.command

import me.melijn.melijnbot.enums.PermState
import me.melijn.melijnbot.objects.translation.Translateable
import me.melijn.melijnbot.objects.utils.sendMsg
import net.dv8tion.jda.api.Permission

const val PREFIX_PLACE_HOLDER = "%prefix%"

abstract class AbstractCommand(val root: String) {

    var name: String = ""
    var id: Int = 0
    var description = Translateable("$root.description")
    var syntax = Translateable("$root.syntax")
    var help = Translateable("$root.help")
    var commandCategory: CommandCategory = CommandCategory.DEVELOPER
    var aliases: Array<String> = arrayOf()
    var discordPermissions: Array<Permission> = arrayOf()
    var runConditions: Array<RunCondition> = arrayOf()
    var children: Array<AbstractCommand> = arrayOf()

    init {
        description = Translateable("$root.description")
    }

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

        val permission = context.commandOrder.joinToString(".", transform = { command -> command.name.toLowerCase() })
        if (hasPermission(context, permission)) {
            context.initArgs()
            if (context.isFromGuild) {
                val pair1 = Pair(context.getTextChannel().idLong, context.authorId)
                val map1 = context.daoManager.commandChannelCoolDownWrapper.executions[pair1]?.toMutableMap() ?: hashMapOf()
                map1[id] = System.currentTimeMillis()
                context.daoManager.commandChannelCoolDownWrapper.executions[pair1] = map1

                val pair2 = Pair(context.guildId, context.authorId)
                val map2 = context.daoManager.commandChannelCoolDownWrapper.executions[pair2]?.toMutableMap() ?: hashMapOf()
                map2[id] = System.currentTimeMillis()
                context.daoManager.commandChannelCoolDownWrapper.executions[pair2] = map2
            }
            execute(context)
        } else sendMissingPermissionMessage(context, permission)
    }

    private fun sendMissingPermissionMessage(context: CommandContext, permission: String) {
        sendMsg(context, Translateable("message.botpermission.missing").string(context)
                .replace("%permission%", permission))
    }

    private fun hasPermission(context: CommandContext, permission: String): Boolean {
        if (!context.isFromGuild) return true
        if (context.getMember()?.isOwner!! || context.getMember()?.hasPermission(Permission.ADMINISTRATOR) == true) return true
        val guildId = context.guildId
        val authorId = context.getAuthor().idLong
        //Gives me better ability to help
        if (context.botDevIds.contains(authorId)) return true


        val channelId = context.getTextChannel().idLong
        val userMap = context.daoManager.userPermissionWrapper.guildUserPermissionCache.get(Pair(guildId, authorId)).get()
        val channelUserMap = context.daoManager.channelUserPermissionWrapper.channelUserPermissionCache.get(Pair(channelId, authorId)).get()

        //permission checking for user specific channel overrides (these override all)
        if (channelUserMap.containsKey(permission) && channelUserMap[permission] != PermState.DEFAULT) {
            return channelUserMap[permission] == PermState.ALLOW
        }

        //permission checking for user specific permissions (these override all role permissions)
        if (userMap.containsKey(permission) && userMap[permission] != PermState.DEFAULT) {
            return userMap[permission] == PermState.ALLOW
        }

        var roleResult = PermState.DEFAULT
        var channelRoleResult = PermState.DEFAULT

        //Permission checking for roles
        for (roleId in (context.getMember()!!.roles.map { role -> role.idLong } + context.getGuild().publicRole.idLong)) {
            channelRoleResult = when (context.daoManager.channelRolePermissionWrapper.channelRolePermissionCache.get(Pair(channelId, roleId)).get()[permission]) {
                PermState.ALLOW -> PermState.ALLOW
                PermState.DENY -> if (channelRoleResult == PermState.DEFAULT) PermState.DENY else channelRoleResult
                else -> channelRoleResult
            }
            if (channelRoleResult == PermState.ALLOW) break
            if (channelRoleResult != PermState.DEFAULT) continue
            if (roleResult != PermState.ALLOW) {
                roleResult = when (context.daoManager.rolePermissionWrapper.rolePermissionCache.get(roleId).get()[permission]) {
                    PermState.ALLOW -> PermState.ALLOW
                    PermState.DENY -> if (roleResult == PermState.DEFAULT) PermState.DENY else roleResult
                    else -> roleResult
                }
            }
        }
        if (channelRoleResult != PermState.DEFAULT) roleResult = channelRoleResult


        return if (commandCategory == CommandCategory.ADMINISTRATION || commandCategory == CommandCategory.MODERATION)
            roleResult == PermState.ALLOW
        else roleResult != PermState.DENY
    }


    fun isCommandFor(input: String): Boolean {
        if (name.equals(input, true)) return true
        for (alias in aliases)
            if (alias.equals(input, true)) return true
        return false
    }
}