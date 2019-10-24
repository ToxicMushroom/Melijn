package me.melijn.melijnbot.objects.command

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.enums.PermState
import me.melijn.melijnbot.objects.translation.i18n
import me.melijn.melijnbot.objects.utils.sendMsg
import net.dv8tion.jda.api.Permission

const val PREFIX_PLACE_HOLDER = "%prefix%"

abstract class AbstractCommand(val root: String) {

    var name: String = ""
    var id: Int = 0
    var description = "$root.description"
    var syntax = "$root.syntax"
    var help = "$root.help"
    var commandCategory: CommandCategory = CommandCategory.DEVELOPER
    var aliases: Array<String> = arrayOf()
    var discordPermissions: Array<Permission> = arrayOf()
    var runConditions: Array<RunCondition> = arrayOf()
    var children: Array<AbstractCommand> = arrayOf()

    init {
        description = "$root.description"
    }

    protected abstract suspend fun execute(context: CommandContext)
    suspend fun run(context: CommandContext, commandPartInvoke: Int = 1) {
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
                val map1 = context.daoManager.commandChannelCoolDownWrapper.executions[pair1]?.toMutableMap()
                    ?: hashMapOf()
                map1[id.toString()] = System.currentTimeMillis()
                context.daoManager.commandChannelCoolDownWrapper.executions[pair1] = map1

                val pair2 = Pair(context.getGuildId(), context.authorId)
                val map2 = context.daoManager.commandChannelCoolDownWrapper.executions[pair2]?.toMutableMap()
                    ?: hashMapOf()
                map2[id.toString()] = System.currentTimeMillis()
                context.daoManager.commandChannelCoolDownWrapper.executions[pair2] = map2
            }
            execute(context)
            context.daoManager.commandUsageWrapper.addUse(this.id)
        } else sendMissingPermissionMessage(context, permission)
    }

    private suspend fun sendMissingPermissionMessage(context: CommandContext, permission: String) {
        val language = context.getLanguage()
        val msg = i18n.getTranslation(language, "message.botpermission.missing")
            .replace("%permission%", permission)
        sendMsg(context, msg)
    }

    private suspend fun hasPermission(context: CommandContext, permission: String): Boolean {
        if (!context.isFromGuild) return true
        if (context.getMember()?.isOwner!! || context.getMember()?.hasPermission(Permission.ADMINISTRATOR) == true) return true
        val guildId = context.getGuildId()
        val authorId = context.getAuthor().idLong
        //Gives me better ability to help
        if (context.botDevIds.contains(authorId)) return true


        val channelId = context.getTextChannel().idLong
        val userMap = context.daoManager.userPermissionWrapper.guildUserPermissionCache.get(Pair(guildId, authorId)).await()
        val channelUserMap = context.daoManager.channelUserPermissionWrapper.channelUserPermissionCache.get(Pair(channelId, authorId)).await()

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
            channelRoleResult = when (context.daoManager.channelRolePermissionWrapper.channelRolePermissionCache.get(Pair(channelId, roleId)).await()[permission]) {
                PermState.ALLOW -> PermState.ALLOW
                PermState.DENY -> if (channelRoleResult == PermState.DEFAULT) PermState.DENY else channelRoleResult
                else -> channelRoleResult
            }
            if (channelRoleResult == PermState.ALLOW) break
            if (channelRoleResult != PermState.DEFAULT) continue
            if (roleResult != PermState.ALLOW) {
                roleResult = when (context.daoManager.rolePermissionWrapper.rolePermissionCache.get(roleId).await()[permission]) {
                    PermState.ALLOW -> PermState.ALLOW
                    PermState.DENY -> if (roleResult == PermState.DEFAULT) PermState.DENY else roleResult
                    else -> roleResult
                }
            }
        }
        if (channelRoleResult != PermState.DEFAULT) roleResult = channelRoleResult


        return if (commandCategory == CommandCategory.ADMINISTRATION || commandCategory == CommandCategory.MODERATION) {
            roleResult == PermState.ALLOW
        } else {
            roleResult != PermState.DENY
        }
    }


    fun isCommandFor(input: String): Boolean {
        if (name.equals(input, true)) {
            return true
        }
        for (alias in aliases) {
            if (alias.equals(input, true)) {
                return true
            }
        }
        return false
    }
}