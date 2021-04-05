package me.melijn.melijnbot.internals.command.custom

import io.ktor.client.*
import me.melijn.melijnbot.Container
import me.melijn.melijnbot.database.command.CustomCommand
import me.melijn.melijnbot.database.message.MessageWrapper
import me.melijn.melijnbot.internals.command.CommandClient
import me.melijn.melijnbot.internals.command.SPACE_REGEX
import me.melijn.melijnbot.internals.command.hasPermission
import me.melijn.melijnbot.internals.jagtag.CCJagTagParser
import me.melijn.melijnbot.internals.jagtag.CCJagTagParserArgs
import me.melijn.melijnbot.internals.models.ModularMessage
import me.melijn.melijnbot.internals.translation.getLanguage
import me.melijn.melijnbot.internals.utils.SPACE_PATTERN
import me.melijn.melijnbot.internals.utils.message.sendAttachmentsAwaitN
import me.melijn.melijnbot.internals.utils.message.sendMissingPermissionMessage
import me.melijn.melijnbot.internals.utils.message.sendMsgAwaitN
import me.melijn.melijnbot.internals.utils.message.sendMsgWithAttachmentsAwaitN
import me.melijn.melijnbot.internals.utils.removeFirst
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import kotlin.random.Random

class CustomCommandClient(val container: Container) {

    fun findCustomCommands(
        customCommands: List<CustomCommand>,
        cmdParts: List<String>,
        prefix: Boolean,
        spaceMap: MutableMap<String, Int>,
        ccMatches: MutableList<CustomCommand>
    ) {
        for (cc in customCommands) {
            val aliases = cc.aliases
            val ccNameParts = cc.name.split(SPACE_REGEX)
            val offset = if (prefix) 1 else 0
            if (ccNameParts.size <= (cmdParts.size - offset)) {
                val matches = ccNameParts.withIndex().all { cmdParts[it.index + offset] == it.value }
                if (matches) {
                    val spaceCount = ccNameParts.size - 1
                    spaceMap["cc.${cc.id}"] = spaceCount
                    ccMatches.add(cc)
                }
            }
            if (aliases != null) {
                for (alias in aliases) {
                    val aliasParts = alias.split(SPACE_PATTERN)
                    if (aliasParts.size <= (cmdParts.size - offset)) {
                        val matches = aliasParts.withIndex().all { cmdParts[it.index + offset] == it.value }
                        if (!matches) continue
                        val spaceCount = aliasParts.size - 1

                        spaceMap["cc.${cc.id}"] = spaceCount
                        ccMatches.add(cc)
                    } else continue
                }
            }
        }
    }

    suspend fun runCustomCommandByChance(
        message: Message,
        httpClient: HttpClient,
        commandParts: List<String>,
        ccs: List<CustomCommand>,
        hasPrefix: Boolean
    ) {
        val cc: CustomCommand = if (ccs.size == 1) {
            ccs.first()
        } else {
            getCustomCommandByChance(ccs)
        }

        if (CommandClient.checksFailed(container.daoManager, cc, message)) return

        if (hasPermission(container, message, "cc.${cc.id}")) {
            val cParts = commandParts.toMutableList()
            executeCC(cc, httpClient, message, cParts, hasPrefix)
        } else {
            val language = getLanguage(container.daoManager, message.author.idLong, message.guild.idLong)
            sendMissingPermissionMessage(message.textChannel, container.daoManager, language, "cc.${cc.id}")
        }
    }

    private suspend fun executeCC(
        cc: CustomCommand,
        httpClient: HttpClient,
        message: Message,
        commandParts: List<String>,
        hasPrefix: Boolean
    ) {
        val member = message.member ?: return
        val channel = message.textChannel
        if (!channel.canTalk()) return

        val daoManager = container.daoManager
        val executions = daoManager.commandChannelCoolDownWrapper.executions

        //registering execution
        val pair1 = Pair(channel.idLong, member.idLong)
        val map1 = executions[pair1]?.toMutableMap()
            ?: mutableMapOf()
        map1["cc." + cc.id] = System.currentTimeMillis()
        executions[pair1] = map1

        val pair2 = Pair(member.guild.idLong, member.idLong)
        val map2 = executions[pair2]?.toMutableMap()
            ?: mutableMapOf()
        map2["cc." + cc.id] = System.currentTimeMillis()
        executions[pair2] = map2

        val rawArg = message.contentRaw
            .removeFirst(commandParts[0])
            .trim()
            .removeFirst(if (hasPrefix) commandParts[1] else "")
            .trim()
        val modularMessage = replaceVariablesInCCMessage(member, rawArg, daoManager.messageWrapper, cc)

        val rsp: Message? = modularMessage.toMessage()
        when {
            rsp == null -> sendAttachmentsAwaitN(channel, httpClient, modularMessage.attachments)
            modularMessage.attachments.isNotEmpty() -> sendMsgWithAttachmentsAwaitN(
                channel,
                httpClient,
                rsp,
                modularMessage.attachments
            )
            else -> sendMsgAwaitN(channel, rsp)
        }
    }


    private suspend fun replaceVariablesInCCMessage(
        member: Member,
        rawArg: String,
        messageWrapper: MessageWrapper,
        cc: CustomCommand
    ): ModularMessage {
        val modularMessage = cc.msgName?.let { messageWrapper.getMessage(member.guild.idLong, it) }
            ?: ModularMessage("not set")

        val ccArgs = CCJagTagParserArgs(member, rawArg, cc)

        return modularMessage.mapAllStringFields {
            if (it != null) {
                CCJagTagParser.parseJagTag(ccArgs, it)
            } else {
                null
            }
        }
    }

    private fun getCustomCommandByChance(ccs: List<CustomCommand>): CustomCommand {
        var range = 0
        for (cc in ccs) {
            range += cc.chance
        }
        val winner = Random.nextInt(range)
        range = 0
        for (cc in ccs) {
            val bool1 = (range <= winner)
            range += cc.chance
            val bool2 = (range <= winner)
            if (bool1 && !bool2) {
                return cc
            }
        }
        throw IllegalArgumentException("random int ($winner) out of range of ccs")
    }
}