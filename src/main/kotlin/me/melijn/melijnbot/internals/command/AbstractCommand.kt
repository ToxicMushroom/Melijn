package me.melijn.melijnbot.internals.command

import kotlinx.coroutines.delay
import me.melijn.melijnbot.Container
import me.melijn.melijnbot.enums.PermState
import me.melijn.melijnbot.internals.command.AbstractCommand.Companion.comparator
import me.melijn.melijnbot.internals.threading.TaskManager
import me.melijn.melijnbot.internals.utils.SPACE_PATTERN
import me.melijn.melijnbot.internals.utils.addIfNotPresent
import me.melijn.melijnbot.internals.utils.message.sendInGuild
import me.melijn.melijnbot.internals.utils.message.sendMissingPermissionMessage
import net.dv8tion.jda.api.MessageBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Message
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*
import kotlin.collections.ArrayList

const val PLACEHOLDER_PREFIX = "prefix"

abstract class AbstractCommand(val root: String) {

    var name: String = ""
    var id: Int = 0
    var description = "$root.description"
    var syntax = "$root.syntax"
    var help = "$root.help"
    var arguments = "$root.arguments"
    var examples = "$root.examples"
    var cooldown: Long = 0 // millis
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

    companion object {
        val comparator: (o1: String, o2: String) -> Int = { o1, o2 ->
            val s = o2.count { it == '.' }.compareTo(o1.count { it == '.' })
            if (s == 0) 1
            else s
        }
    }

    private val cmdlogger = LoggerFactory.getLogger("cmd")

    protected abstract suspend fun execute(context: ICommandContext)
    suspend fun run(context: ICommandContext) {
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
                val aliasCache = context.daoManager.aliasWrapper
                if (context.isFromGuild) {
                    context.aliasMap.putAll(aliasCache.getAliases(context.guildId))
                }
                for ((cmd2, ls) in aliasCache.getAliases(context.authorId)) {
                    val currentList = (context.aliasMap[cmd2] ?: emptyList()).toMutableList()
                    for (alias in ls) {
                        currentList.addIfNotPresent(alias, true)
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
                            val aliasParts = alias.split(SPACE_PATTERN)
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
                // Update cooldowns
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
                val cmdId = context.commandOrder.first().id.toString() + context.commandOrder.drop(1)
                    .joinToString(".") { it.name }
                if (CommandClient.checksFailed(
                        context.container,
                        context.commandOrder.last(),
                        cmdId,
                        context.message,
                        true,
                        context.commandParts
                    )
                ) return

                // Console log of commmand
                cmdlogger.info("${context.guildN?.name ?: ""}/${context.author.name}◠: ${context.message.contentRaw}")
                val start = System.currentTimeMillis()
                try {
                    execute(context)
                } catch (t: Throwable) {
                    cmdlogger.error(
                        "↱ ${context.guildN?.name ?: ""}/${context.author.name}◡: ${context.message.contentRaw}",
                        t
                    )
                    t.sendInGuild(context, shouldSend = true)
                }

                // new year check
                // checkNewYear(context)

                if (context.isFromGuild && context.daoManager.supporterWrapper.getGuilds().contains(context.guildId)) {
                    TaskManager.async {
                        val timeMap = context.daoManager.removeInvokeWrapper.getMap(context.guildId)
                        val seconds = timeMap[context.textChannel.idLong] ?: timeMap[context.guildId] ?: return@async

                        if (!context.selfMember.hasPermission(
                                context.textChannel,
                                Permission.MESSAGE_MANAGE
                            )
                        ) return@async

                        delay(seconds * 1000L)
                        val message = context.message
                        context.container.botDeletedMessageIds.add(message.idLong)

                        if (!context.selfMember.hasPermission(
                                context.textChannel,
                                Permission.MESSAGE_MANAGE
                            )
                        ) return@async
                        message.delete().queue(null, { context.container.botDeletedMessageIds.remove(message.idLong) })
                    }
                }
                val second = System.currentTimeMillis()
                cmdlogger.info("${context.guildN?.name ?: ""}/${context.author.name}◡${(second - start) / 1000.0}: ${context.message.contentRaw}")
            } catch (t: Throwable) {
                cmdlogger.error(
                    "↱ ${context.guildN?.name ?: ""}/${context.author.name}◡: ${context.message.contentRaw}",
                    t
                )
                t.sendInGuild(context)
            }
            context.daoManager.commandUsageWrapper.addUse(context.commandOrder[0].id)
        } else {
            sendMissingPermissionMessage(context, permission)
        }
    }

    private suspend fun checkNewYear(context: ICommandContext) {
        val tz = context.getTimeZoneId()
        val now = Instant.now().atZone(tz)
        if (now.dayOfYear == 1 && !context.daoManager.newYearWrapper.contains(now.year, context.authorId)) {
            context.channel.sendMessage(
                MessageBuilder().setContent(
                    "\uD83D\uDDD3 **Happy New Year ${now.year}** **" + if (context.isFromGuild) {
                        context.member.asMention
                    } else {
                        context.author.asMention
                    } + "** \uD83C\uDF8A"
                )
                    .setAllowedMentions(EnumSet.allOf(Message.MentionType::class.java))
                    .build()
            ).queue()

            context.daoManager.newYearWrapper.add(now.year, context.authorId)
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

/**
 * permMap: map with permission nodes mapped to states (can be for user, userchannel, role, rolechannel)
 * lPermission: lowercase permission node to check
 * cPermState: current permission state
 * returns: new permission state or the cPermState depending on if arguments supplied
 */
fun getStateFromMap(
    permMap: Map<String, PermState>,
    lPermission: String,
    commands: Set<AbstractCommand>,
    cPermState: PermState
): PermState {
    var nPermState = cPermState
    for ((rolePerm, state) in permMap) {
        val getSuitableResult = when (state) {
            PermState.ALLOW -> PermState.ALLOW
            PermState.DENY -> if (nPermState == PermState.DEFAULT) PermState.DENY else nPermState
            else -> nPermState
        }
        if (rolePerm.last() == '*' && (lPermission.length > rolePerm.length || lPermission.length == rolePerm.length - 2)) { // rolePerm.* and rolePerm.something > 9
            if (rolePerm == "*") {
                nPermState = getSuitableResult
                break
            } else {
                val begin = rolePerm.dropLast(2)
                if (lPermission.startsWith(begin, true)) {
                    nPermState = getSuitableResult
                }
                break
            }
        } else {
            if (lPermission == rolePerm) {
                nPermState = getSuitableResult
                break
            } else {
                val category = try {
                    CommandCategory.valueOf(rolePerm.toUpperCase())
                } catch (t: Throwable) {
                    null
                }
                if (category != null) {
                    if (commands.firstOrNull { cmd ->
                            lPermission.takeWhile { it != '.' }.equals(cmd.name, true)
                        }?.commandCategory == category) {
                        nPermState = state
                        break
                    }
                }
            }
        }
    }
    return nPermState
}


suspend fun hasPermission(context: ICommandContext, permission: String, required: Boolean? = null): Boolean {
    val commandOrder = context.commandOrder
    val rootCommand = commandOrder.first()
    val lowestCommand = commandOrder.last()
    return hasPermission(
        context.container,
        context.message,
        permission,
        rootCommand.commandCategory,
        required ?: (rootCommand.permissionRequired || lowestCommand.permissionRequired)
    )
}

suspend fun hasPermission(
    container: Container,
    message: Message,
    permission: String,
    category: CommandCategory? = null,
    required: Boolean = false
): Boolean {
    val member = message.member ?: return true
    if (member.isOwner || member.hasPermission(Permission.ADMINISTRATOR)) return true
    val guild = member.guild
    val guildId = guild.idLong
    val authorId = member.idLong

    // Gives me better ability to help
    if (container.settings.botInfo.developerIds.contains(authorId)) return true
    val commands = container.commandSet
    val daoManager = container.daoManager
    val channelId = message.channel.idLong

    val userMap = daoManager.userPermissionWrapper.getPermMap(guildId, authorId).toSortedMap(comparator)
    val channelUserMap = daoManager.channelUserPermissionWrapper.getPermMap(channelId, authorId).toSortedMap(comparator)

    val lPermission = permission.toLowerCase()

    // permission checking for user specific channel overrides (these override all)
    val channelUserState = getStateFromMap(channelUserMap, lPermission, commands, PermState.DEFAULT)
    if (channelUserState != PermState.DEFAULT) {
        return channelUserState == PermState.ALLOW
    }

    // permission checking for user specific permissions (these override all role permissions)
    val userState = getStateFromMap(userMap, lPermission, commands, PermState.DEFAULT)
    if (userState != PermState.DEFAULT) {
        return userState == PermState.ALLOW
    }

    var roleResult = PermState.DEFAULT
    var channelRoleResult = PermState.DEFAULT


    // Permission checking for roles
    for (roleId in (member.roles.map { role -> role.idLong } + guild.publicRole.idLong)) {
        val channelPermMap =
            daoManager.channelRolePermissionWrapper.getPermMap(channelId, roleId).toSortedMap(comparator)
        channelRoleResult = getStateFromMap(channelPermMap, lPermission, commands, channelRoleResult)

        if (channelRoleResult == PermState.ALLOW) break
        if (roleResult != PermState.ALLOW) {
            val rolePermMap = daoManager.rolePermissionWrapper.getPermMap(roleId).toSortedMap(comparator)
            roleResult = getStateFromMap(rolePermMap, lPermission, commands, roleResult)
        }
    }

    if (channelRoleResult != PermState.DEFAULT) {
        roleResult = channelRoleResult
    }


    return if (
        category == CommandCategory.ADMINISTRATION ||
        category == CommandCategory.MODERATION ||
        required
    ) {
        roleResult == PermState.ALLOW
    } else {
        roleResult != PermState.DENY
    }
}