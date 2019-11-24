package me.melijn.melijnbot.commands.administration

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.enums.ChannelType
import me.melijn.melijnbot.enums.LogChannelType
import me.melijn.melijnbot.enums.RoleType
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.embed.Embedder
import me.melijn.melijnbot.objects.utils.StringUtils
import me.melijn.melijnbot.objects.utils.sendEmbed
import me.melijn.melijnbot.objects.utils.sendMsg
import me.melijn.melijnbot.objects.utils.toUCSC
import net.dv8tion.jda.api.utils.MarkdownSanitizer
import okhttp3.internal.toHexString


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
                sendMsg(context, "unknown guildId :/")
                return
            }
            guild1
        } else {
            context.guild
        }

        val guildId = guild.idLong
        val daoManager = context.daoManager
        val roleCache = daoManager.roleWrapper.roleCache
        val channelCache = daoManager.channelWrapper.channelCache
        val logChannelCache = daoManager.logChannelWrapper.logChannelCache
        val logChannels = LogChannelType.values().joinToString("\n") { type ->
            "**${type.text}:** " + idToChannelMention(logChannelCache.get(Pair(guildId, type)).get())
        }
        val ec = daoManager.embedColorWrapper.embedColorCache.get(guildId).await()
        val pec = daoManager.userEmbedColorWrapper.userEmbedColorCache.get(context.authorId).await()

        val description = "MusicChannel:** " + idToChannelMention(daoManager.musicChannelWrapper.musicChannelCache.get(guildId).await()) +
            "\n**StreamUrl:** " + stringToString(daoManager.streamUrlWrapper.streamUrlCache.get(guildId).await()) +
            "\n" +
            "\n**MuteRole:** " + idToRoleMention(roleCache.get(Pair(guildId, RoleType.MUTE)).await()) +
            "\n**JoinRole:** " + idToRoleMention(roleCache.get(Pair(guildId, RoleType.JOIN)).await()) +
            "\n**UnverifiedRole:** " + idToRoleMention(roleCache.get(Pair(guildId, RoleType.UNVERIFIED)).await()) +
            "\n" +
            "\n**VerificationChannel:** " + idToChannelMention(channelCache.get(Pair(guildId, ChannelType.VERIFICATION)).await()) +
            "\n**JoinChannel:** " + idToChannelMention(channelCache.get(Pair(guildId, ChannelType.JOIN)).await()) +
            "\n**LeaveChannel:** " + idToChannelMention(channelCache.get(Pair(guildId, ChannelType.LEAVE)).await()) +
            "\n**SelfRoleChannel:** " + idToChannelMention(channelCache.get(Pair(guildId, ChannelType.SELFROLE)).await()) +
            "\n$logChannels" +
            "\n" +
            "\n**VerificationCode:** " + stringToString(daoManager.verificationCodeWrapper.verificationCodeCache.get(guildId).await()) +
            "\n**VerificationEmoteji:** " + stringToString(daoManager.verificationEmotejiWrapper.verificationEmotejiCache.get(guildId).await()) +
            "\n**VerificationType:** " + stringToString(daoManager.verificationTypeWrapper.verificationTypeCache.get(guildId).await().toUCSC()) +
            "\n**MaxVerificationFlowRate:** " + daoManager.verificationUserFlowRateWrapper.verificationUserFlowRateCache.get(guildId).await() +
            "\n" +
            "\n**Prefixes:" +
            daoManager.guildPrefixWrapper.prefixCache.get(guildId).await().joinToString { pref ->
                "\n  - **" + MarkdownSanitizer.escape(pref) + "**"
            } +
            "\nPrivatePrefixes:" +
            daoManager.userPrefixWrapper.prefixCache.get(context.authorId).await().joinToString { pref ->
                "\n  - **" + MarkdownSanitizer.escape(pref) + "**"
            } +
            "\nEmbedState: **" + booleanToString(context, !daoManager.embedDisabledWrapper.embedDisabledCache.contains(guildId)) +
            "\n**EmbedColor: **" + (if (ec == 0) "unset" else "#" + ec.toHexString()) +
            "\n**PrivateEmbedColor: **" + (if (pec == 0) "unset" else pec.toHexString()) +
            "\n**Language: **" + daoManager.guildLanguageWrapper.languageCache.get(guildId).await() +
            "\n**PrivateLanguage: **" + stringToString(daoManager.userLanguageWrapper.languageCache.get(context.authorId).await()) + "**"



        if (description.length > 2048) {
            val parts = StringUtils.splitMessage(description).toMutableList()
            val title = context.getTranslation("$root.title")
            val eb = Embedder(context)
                .setTitle(title)
                .setDescription(parts[0])

            sendEmbed(context, eb.build())
            parts.removeAt(0)
            eb.setTitle(null)
            for (part in parts) {
                eb.setDescription(part)
                sendEmbed(context, eb.build())
            }
        } else {
            val title = context.getTranslation("$root.title")
            val eb = Embedder(context)
                .setTitle(title)
                .setDescription(description)

            sendEmbed(context, eb.build())
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