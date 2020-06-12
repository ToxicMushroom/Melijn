package me.melijn.melijnbot.objects.command

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.Container
import me.melijn.melijnbot.enums.PermState
import me.melijn.melijnbot.objects.utils.addIfNotPresent
import me.melijn.melijnbot.objects.utils.sendInGuild
import me.melijn.melijnbot.objects.utils.sendMsg
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

const val PLACEHOLDER_PREFIX = "%prefix%"

abstract class AbstractCommand(val root: String) {

    var name: String = ""
    var id: Int = 0
    var description = "$root.description"
    var syntax = "$root.syntax"
    var help = "$root.help"
    var arguments = "$root.arguments"
    var examples = "$root.examples"
    var commandCategory: CommandCategory = CommandCategory.DEVELOPER
    var aliases: Array<String> = arrayOf()
    var discordChannelPermissions: Array<Permission> = arrayOf()
    var discordPermissions: Array<Permission> = arrayOf()
    var runConditions: Array<RunCondition> = arrayOf()
    var children: Array<AbstractCommand> = arrayOf()
    var permissionRequired: Boolean = false
    //var args: Array<CommandArg> = arrayOf() cannot put extra information after global definitions with this

    init {
        description = "$root.description"
    }

    protected abstract suspend fun execute(context: CommandContext)
    suspend fun run(context: CommandContext) {
        context.commandOrder = ArrayList(context.commandOrder + this).toList()

        val indexedCommand = context.commandOrder.withIndex().sortedBy { it.index }.last()
        val cmd = indexedCommand.value

        if (context.calculatedRoot.isEmpty()) context.calculatedRoot += cmd.id
        else context.calculatedRoot += "." + cmd.name

        context.calculatedCommandPartsOffset += (context.partSpaceMap[context.calculatedRoot]
            ?: 0) + 1 // +1 is for the index of the commandpart

        // Check for child commands
        if (context.commandParts.size > context.calculatedCommandPartsOffset && children.isNotEmpty()) {
            val currentRoot = context.calculatedRoot
            val currentOffset = context.calculatedCommandPartsOffset

            // Searches if needed for aliases
            if (!context.searchedAliases) {
                val aliasCache = context.daoManager.aliasWrapper.aliasCache
                if (context.isFromGuild) {
                    context.aliasMap.putAll(aliasCache.get(context.guildId).await())
                }
                for ((cmd2, ls) in aliasCache.get(context.authorId).await()) {
                    val currentList = (context.aliasMap[cmd2] ?: emptyList()).toMutableList()
                    for (alias in ls) {
                        currentList.addIfNotPresent(alias)
                    }

                    context.aliasMap[cmd2] = currentList
                }
                context.searchedAliases = true
            }

            // Searched for correct child that matches a custom alias
            for (child in children) {
                for ((cmdPath, aliases) in context.aliasMap) {
                    val subRoot = currentRoot + "." + child.name
                    if (cmdPath == subRoot) {
                        for (alias in aliases) {
                            val aliasParts = alias.split(SPACE_REGEX)
                            if (aliasParts.size <= (context.commandParts.size - currentOffset)) {
                                val matches = aliasParts.withIndex().all {
                                    context.commandParts[it.index + currentOffset] == it.value
                                }
                                if (!matches) continue

                                // Matched a subcommand v
                                context.partSpaceMap[subRoot] = aliasParts.size - 1
                                child.run(context)
                                return
                            }
                        }
                    }
                }
            }

            for (child in children) {
                if (child.isCommandFor(context.commandParts[currentOffset])) {
                    child.run(context)
                    return
                }
            }
        }

        val permission = context.commandOrder.joinToString(".", transform = { command -> command.name.toLowerCase() })
        if (hasPermission(context, permission)) {
            context.initArgs()
            if (context.isFromGuild) {
                // Check for cooldowns
                val pair1 = Pair(context.channelId, context.authorId)
                val map1 = context.daoManager.commandChannelCoolDownWrapper.executions[pair1]?.toMutableMap()
                    ?: hashMapOf()
                map1[id.toString()] = System.currentTimeMillis()
                context.daoManager.commandChannelCoolDownWrapper.executions[pair1] = map1

                val pair2 = Pair(context.guildId, context.authorId)
                val map2 = context.daoManager.commandChannelCoolDownWrapper.executions[pair2]?.toMutableMap()
                    ?: hashMapOf()
                map2[id.toString()] = System.currentTimeMillis()
                context.daoManager.commandChannelCoolDownWrapper.executions[pair2] = map2
            }
            try {
                if (CommandClient.checksFailed(context.container, context.commandOrder.last(), context.event, true, context.commandParts)) return
                execute(context)
            } catch (t: Throwable) {
                t.sendInGuild(context)
            }
            context.daoManager.commandUsageWrapper.addUse(context.commandOrder[0].id)
        } else sendMissingPermissionMessage(context, permission)
    }

    suspend fun sendMissingPermissionMessage(context: CommandContext, permission: String) {
        val msg = context.getTranslation("message.botpermission.missing")
            .replace("%permission%", permission)
        sendMsg(context, msg)
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

suspend fun hasPermission(context: CommandContext, permission: String, required: Boolean = false): Boolean {
    if (!context.isFromGuild) return true
    if (context.member.isOwner || context.member.hasPermission(Permission.ADMINISTRATOR)) return true
    val guildId = context.guildId
    val authorId = context.authorId
    val daoManager = context.daoManager
    // Gives me better ability to help
    if (context.botDevIds.contains(authorId)) return true


    val channelId = context.channelId
    val userMap = daoManager.userPermissionWrapper.guildUserPermissionCache.get(Pair(guildId, authorId)).await()
    val channelUserMap = daoManager.channelUserPermissionWrapper.channelUserPermissionCache.get(Pair(channelId, authorId)).await()

    val lPermission = permission.toLowerCase()

    // permission checking for user specific channel overrides (these override all)
    if (channelUserMap.containsKey(lPermission) && channelUserMap[lPermission] != PermState.DEFAULT) {
        return channelUserMap[lPermission] == PermState.ALLOW
    }

    // permission checking for user specific permissions (these override all role permissions)
    if (userMap.containsKey(lPermission) && userMap[lPermission] != PermState.DEFAULT) {
        return userMap[lPermission] == PermState.ALLOW
    }

    var roleResult = PermState.DEFAULT
    var channelRoleResult = PermState.DEFAULT


    // Permission checking for roles
    for (roleId in (context.member.roles.map { role -> role.idLong } + context.guild.publicRole.idLong)) {
        channelRoleResult = when (
            daoManager.channelRolePermissionWrapper.channelRolePermissionCache
                .get(Pair(channelId, roleId))
                .await()[lPermission]
            ) {
            PermState.ALLOW -> PermState.ALLOW
            PermState.DENY -> if (channelRoleResult == PermState.DEFAULT) {
                PermState.DENY
            } else {
                channelRoleResult
            }
            else -> channelRoleResult
        }
        if (channelRoleResult == PermState.ALLOW) break
        if (channelRoleResult != PermState.DEFAULT) continue
        if (roleResult != PermState.ALLOW) {
            roleResult = when (context.daoManager.rolePermissionWrapper.rolePermissionCache.get(roleId).await()[lPermission]) {
                PermState.ALLOW -> PermState.ALLOW
                PermState.DENY -> if (roleResult == PermState.DEFAULT) PermState.DENY else roleResult
                else -> roleResult
            }
        }
    }

    if (channelRoleResult != PermState.DEFAULT) {
        roleResult = channelRoleResult
    }


    return if (
        context.commandOrder[0].commandCategory == CommandCategory.ADMINISTRATION ||
        context.commandOrder[0].commandCategory == CommandCategory.MODERATION ||
        context.commandOrder[0].permissionRequired ||
        context.commandOrder.last().permissionRequired ||
        required
    ) {
        roleResult == PermState.ALLOW
    } else {
        roleResult != PermState.DENY
    }
}

suspend fun hasPermission(command: AbstractCommand, container: Container, event: MessageReceivedEvent, permission: String, required: Boolean = false): Boolean {
    val member = event.member ?: return true
    if (member.isOwner || member.hasPermission(Permission.ADMINISTRATOR)) return true
    val guild = member.guild
    val guildId = guild.idLong
    val authorId = member.idLong
    // Gives me better ability to help
    if (container.settings.developerIds.contains(authorId)) return true


    val channelId = event.textChannel.idLong
    val userMap = container.daoManager.userPermissionWrapper.guildUserPermissionCache.get(Pair(guildId, authorId)).await()
    val channelUserMap = container.daoManager.channelUserPermissionWrapper.channelUserPermissionCache.get(Pair(channelId, authorId)).await()

    val lPermission = permission.toLowerCase()

    // permission checking for user specific channel overrides (these override all)
    if (channelUserMap.containsKey(lPermission) && channelUserMap[lPermission] != PermState.DEFAULT) {
        return channelUserMap[lPermission] == PermState.ALLOW
    }

    // permission checking for user specific permissions (these override all role permissions)
    if (userMap.containsKey(lPermission) && userMap[lPermission] != PermState.DEFAULT) {
        return userMap[lPermission] == PermState.ALLOW
    }

    var roleResult = PermState.DEFAULT
    var channelRoleResult = PermState.DEFAULT

    // Permission checking for roles
    for (roleId in (member.roles.map { role -> role.idLong } + guild.publicRole.idLong)) {
        channelRoleResult = when (container.daoManager.channelRolePermissionWrapper.channelRolePermissionCache.get(Pair(channelId, roleId)).await()[lPermission]) {
            PermState.ALLOW -> PermState.ALLOW
            PermState.DENY -> if (channelRoleResult == PermState.DEFAULT) PermState.DENY else channelRoleResult
            else -> channelRoleResult
        }
        if (channelRoleResult == PermState.ALLOW) break
        if (channelRoleResult != PermState.DEFAULT) continue
        if (roleResult != PermState.ALLOW) {
            roleResult = when (container.daoManager.rolePermissionWrapper.rolePermissionCache.get(roleId).await()[lPermission]) {
                PermState.ALLOW -> PermState.ALLOW
                PermState.DENY -> if (roleResult == PermState.DEFAULT) PermState.DENY else roleResult
                else -> roleResult
            }
        }
    }
    if (channelRoleResult != PermState.DEFAULT) roleResult = channelRoleResult


    return if (
        command.commandCategory == CommandCategory.ADMINISTRATION ||
        command.commandCategory == CommandCategory.MODERATION ||
        required
    ) {
        roleResult == PermState.ALLOW
    } else {
        roleResult != PermState.DENY
    }
}