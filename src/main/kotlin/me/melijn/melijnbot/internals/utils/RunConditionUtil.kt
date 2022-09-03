package me.melijn.melijnbot.internals.utils

import me.melijn.melijnbot.Container
import me.melijn.melijnbot.commands.utility.VOTE_URL
import me.melijn.melijnbot.enums.Environment
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.RunCondition
import me.melijn.melijnbot.internals.translation.getLanguage
import me.melijn.melijnbot.internals.translation.i18n
import me.melijn.melijnbot.internals.utils.message.getNicerUsedPrefix
import me.melijn.melijnbot.internals.utils.message.sendMsg
import net.dv8tion.jda.api.entities.Message

object RunConditionUtil {

    /**
     * @returns true if the check passed
     * **/
    suspend fun runConditionCheckPassed(
        container: Container,
        runCondition: RunCondition,
        message: Message,
        command: AbstractCommand,
        commandParts: List<String>
    ): Boolean {
        val userId = message.author.idLong
        val language = getLanguage(container.daoManager, userId, if (message.isFromGuild) message.guild.idLong else -1)
        val prefix = getNicerUsedPrefix(message.jda.selfUser, commandParts[0])
        return when (runCondition) {
            RunCondition.GUILD -> checkGuild(container, message, language)
            RunCondition.DEV_ONLY -> checkDevOnly(container, message, language)
            RunCondition.CHANNEL_NSFW -> checkChannelNSFW(container, message, language)
            RunCondition.VOTED -> checkVoted(container, message, language)
            RunCondition.USER_SUPPORTER -> checkUserSupporter(container, message, language, prefix)
            RunCondition.GUILD_SUPPORTER -> checkGuildSupporter(container, message, language, prefix)
            RunCondition.EXPLICIT_MELIJN_PERMISSION -> true // this is checked in other places later
        }
    }

    private suspend fun checkGuildSupporter(
        container: Container,
        message: Message,
        language: String,
        prefix: String
    ): Boolean {
        val supporterGuilds = container.daoManager.supporterWrapper.getGuilds()
        return if (!supporterGuilds.contains(message.guild.idLong)) {
            val msg = i18n.getTranslation(language, "message.runcondition.failed.server.supporter")
                .replacePrefix(prefix)
            message.channel.sendMessage(msg).queue()
            false
        } else {
            true
        }
    }

    private suspend fun checkUserSupporter(
        container: Container,
        message: Message,
        language: String,
        prefix: String
    ): Boolean {
        val supporters = container.daoManager.supporterWrapper.getUsers()
        return if (
            supporters.contains(message.author.idLong) ||
            container.settings.botInfo.developerIds.contains(message.author.idLong)
        ) {
            true
        } else {
            val msg = i18n.getTranslation(language, "message.runcondition.failed.user.supporter")
                .replacePrefix(prefix)
            message.channel.sendMessage(msg).queue()
            false
        }
    }

    private suspend fun checkVoted(container: Container, message: Message, language: String): Boolean {
        return if (
            container.settings.botInfo.developerIds.contains(message.author.idLong) ||
            container.daoManager.supporterWrapper.getUsers().contains(message.author.idLong) ||
            container.settings.environment == Environment.TESTING ||
            !container.voteReq
        ) {
            true
        } else {
            val vote = container.daoManager.voteWrapper.getUserVote(message.author.idLong)
            val lastTime =
                listOf(vote?.bfdLastTime, vote?.topggLastTime, vote?.dblLastTime, vote?.dboatsLastTime).maxByOrNull {
                    it ?: 0
                }

            if (vote != null && lastTime != null && (System.currentTimeMillis() - lastTime) < 86_400_000) {
                true
            } else {
                val msg = i18n.getTranslation(language, "message.runcondition.failed.voted")
                    .withVariable("url", VOTE_URL)
                message.channel.sendMessage(msg).queue()
                false
            }
        }
    }

    private fun checkChannelNSFW(container: Container, message: Message, language: String): Boolean {
        return if (
            (message.isFromGuild || container.settings.botInfo.developerIds.contains(message.author.idLong)) &&
            message.textChannel.isNSFW
        ) {
            true
        } else {
            val msg = i18n.getTranslation(language, "message.runcondition.failed.nsfw")
            message.channel.sendMessage(msg).queue()
            false
        }
    }

    private fun checkDevOnly(container: Container, message: Message, language: String): Boolean {
        return if (container.settings.botInfo.developerIds.contains(message.author.idLong)) {
            true
        } else {
            val msg = i18n.getTranslation(language, "message.runcondition.failed.devonly")
            message.channel.sendMessage(msg).queue()
            false
        }
    }

    private fun checkGuild(container: Container, message: Message, language: String): Boolean {
        return if (message.isFromGuild) {
            true
        } else {
            val msg = i18n.getTranslation(language, "message.runcondition.serveronly")
            sendMsg(message.privateChannel, msg)
            false
        }
    }


}