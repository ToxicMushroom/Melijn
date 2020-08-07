package me.melijn.melijnbot.commands.administration

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.enums.ChannelType
import me.melijn.melijnbot.enums.LogChannelType
import me.melijn.melijnbot.enums.RoleType
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.embed.Embedder
import me.melijn.melijnbot.internals.utils.StringUtils
import me.melijn.melijnbot.internals.utils.message.sendEmbedRsp
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.toHexString
import me.melijn.melijnbot.internals.utils.toUCSC
import net.dv8tion.jda.api.utils.MarkdownSanitizer


class SettingsCommand : AbstractCommand("command.settings") {

    init {
        id = 121
        name = "settings"
        aliases = arrayOf("preferences")
        commandCategory = CommandCategory.ADMINISTRATION
    }

    override suspend fun execute(context: CommandContext) {
        val guild = if (context.args.isNotEmpty() && context.botDevIds.contains(context.authorId)) {
            val guild1 = context.shardManager.getGuildById(context.args[0])
            if (guild1 == null) {
                sendRsp(context, "unknown guildId :/")
                return
            }
            guild1
        } else {
            context.guild
        }

        val guildId = guild.idLong
        val daoManager = context.daoManager
        val roleWrapper = daoManager.roleWrapper
        val channelCache = daoManager.channelWrapper.channelCache
        val logChannelWrapper = daoManager.logChannelWrapper
        val logChannels = StringBuilder()
        for (type in LogChannelType.values()) {
            logChannels.append("**")
                .append(type.text)
                .append(":** ")
                .append(idToChannelMention(logChannelWrapper.getChannelId(guildId, type)))
                .append("\n")
        }
        logChannels.removeSuffix("\n")
        val ec = daoManager.embedColorWrapper.getColor(guildId)
        val pec = daoManager.userEmbedColorWrapper.getColor(context.authorId)

        val description = "MusicChannel:** " + idToChannelMention(daoManager.musicChannelWrapper.musicChannelCache.get(guildId).await()) +
            "\n**StreamUrl:** " + stringToString(daoManager.streamUrlWrapper.streamUrlCache.get(guildId).await()) +
            "\n" +
            "\n**MuteRole:** " + idToRoleMention(roleWrapper.getRoleId(guildId, RoleType.MUTE)) +
            "\n**UnverifiedRole:** " + idToRoleMention(roleWrapper.getRoleId(guildId, RoleType.UNVERIFIED)) +
            "\n**BirthDayRole:** " + idToRoleMention(roleWrapper.getRoleId(guildId, RoleType.BIRTHDAY)) +
            "\n" +
            "\n**VerificationChannel:** " + idToChannelMention(channelCache.get(Pair(guildId, ChannelType.VERIFICATION)).await()) +
            "\n**JoinChannel:** " + idToChannelMention(channelCache.get(Pair(guildId, ChannelType.JOIN)).await()) +
            "\n**LeaveChannel:** " + idToChannelMention(channelCache.get(Pair(guildId, ChannelType.LEAVE)).await()) +
            "\n**SelfRoleChannel:** " + idToChannelMention(channelCache.get(Pair(guildId, ChannelType.SELFROLE)).await()) +
            "\n**BirthDayChannel:** " + idToChannelMention(channelCache.get(Pair(guildId, ChannelType.BIRTHDAY)).await()) +
            "\n**PreVerificationJoinChannel:** " + idToChannelMention(channelCache.get(Pair(guildId, ChannelType.PRE_VERIFICATION_JOIN)).await()) +
            "\n**PreVerificationLeaveChannel:** " + idToChannelMention(channelCache.get(Pair(guildId, ChannelType.PRE_VERIFICATION_LEAVE)).await()) +
            "\n" +
            "\n$logChannels" +
            "\n" +
            "\n**VerificationPassword:** " + stringToString(daoManager.verificationPasswordWrapper.getPassword(guildId)) +
            "\n**VerificationEmoteji:** " + stringToString(daoManager.verificationEmotejiWrapper.getEmoteji(guildId)) +
            "\n**VerificationType:** " + stringToString(daoManager.verificationTypeWrapper.getType(guildId).toUCSC()) +
            "\n**MaxVerificationFlowRate:** " + daoManager.verificationUserFlowRateWrapper.getFlowRate(guildId) +
            "\n" +
            "\n**Prefixes:" +
            daoManager.guildPrefixWrapper.getPrefixes(guildId).joinToString { pref ->
                "\n  - **" + MarkdownSanitizer.escape(pref) + "**"
            } +
            "\nPrivatePrefixes:" +
            daoManager.userPrefixWrapper.getPrefixes(context.authorId).joinToString { pref ->
                "\n  - **" + MarkdownSanitizer.escape(pref) + "**"
            } +
            "\nEmbedState: **" + booleanToString(context, !daoManager.embedDisabledWrapper.embedDisabledCache.contains(guildId)) +
            "\n**EmbedColor: **" + (if (ec == 0) "unset" else "#" + ec.toHexString()) +
            "\n**PrivateEmbedColor: **" + (if (pec == 0) "unset" else pec.toHexString()) +
            "\n**Language: **" + daoManager.guildLanguageWrapper.getLanguage(guildId) +
            "\n**PrivateLanguage: **" + stringToString(daoManager.userLanguageWrapper.getLanguage(context.authorId)) + "**"



        if (description.length > 2048) {
            val parts = StringUtils.splitMessage(description).toMutableList()
            val title = context.getTranslation("$root.title")
            val eb = Embedder(context)
                .setTitle(title)
                .setDescription(parts[0])

            sendEmbedRsp(context, eb.build())
            parts.removeAt(0)
            eb.setTitle(null)
            for (part in parts) {
                eb.setDescription(part)
                sendEmbedRsp(context, eb.build())
            }
        } else {
            val title = context.getTranslation("$root.title")
            val eb = Embedder(context)
                .setTitle(title)
                .setDescription(description)

            sendEmbedRsp(context, eb.build())
        }
    }

    private suspend fun booleanToString(context: CommandContext, contains: Boolean): String {
        val enabled = context.getTranslation("enabled")
        val disabled = context.getTranslation("disabled")
        return if (contains) enabled else disabled
    }

    private fun idToChannelMention(channelId: Long): String {
        return if (channelId == -1L) {
            "unset"
        } else {
            "<#$channelId>"
        }
    }

    private fun idToRoleMention(roleId: Long): String {
        return if (roleId == -1L) {
            "unset"
        } else {
            "<@&$roleId>"
        }
    }

    private fun stringToString(text: String, encapsulate: Boolean = false): String {
        return if (text == "") {
            "unset"
        } else {
            if (encapsulate) {
                "```$text```"
            } else {
                text
            }
        }
    }
}