package me.melijn.melijnbot.commands.administration

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.enums.ChannelType
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.PLACEHOLDER_ARG
import me.melijn.melijnbot.objects.translation.PLACEHOLDER_CHANNEL
import me.melijn.melijnbot.objects.translation.i18n
import me.melijn.melijnbot.objects.utils.*

class SetChannelCommand : AbstractCommand("command.setchannel") {

    init {
        id = 33
        name = "setChannel"
        aliases = arrayOf("sc")
        commandCategory = CommandCategory.ADMINISTRATION
    }

    override suspend fun execute(context: CommandContext) {
        if (context.args.isEmpty()) {
            sendSyntax(context)
            return
        }

        val type = enumValueOrNull<ChannelType>(context.args[0])
        if (type == null) {
            val language = context.getLanguage()
            val msg = i18n.getTranslation(language, "message.unknown.channeltype")
                .replace(PLACEHOLDER_ARG, context.args[0])
            sendMsg(context, msg)
            return
        }

        if (context.args.size > 1) {
            setChannel(context, type)
        } else {
            showChannel(context, type)
        }
    }

    private suspend fun showChannel(context: CommandContext, type: ChannelType) {
        val channelWrapper = context.daoManager.channelWrapper
        val channelId = channelWrapper.channelCache.get(Pair(context.guildId, type)).await()
        val channel = if (channelId == -1L) null else context.guild.getTextChannelById(channelId)
        if (channel == null) {
            val language = context.getLanguage()
            val msg = i18n.getTranslation(language, "$root.show.unset")
                .replace("%channelType%", type.toString().toUpperWordCase())

            sendMsg(context, msg)
            return
        }
        val language = context.getLanguage()
        val msg = i18n.getTranslation(language, "$root.show.set")
            .replace("%channelType%", type.toString().toUpperWordCase())
            .replace(PLACEHOLDER_CHANNEL, channel.asTag)

        sendMsg(context, msg)
    }

    private suspend fun setChannel(context: CommandContext, type: ChannelType) {
        val channel = if (context.args[1].equals("null", true)) {
            null
        } else {
            getTextChannelByArgsNMessage(context, 1) ?: return
        }

        val channelWrapper = context.daoManager.channelWrapper


        val msg = if (channel == null) {
            channelWrapper.removeChannel(context.guildId, type)
            val language = context.getLanguage()
            i18n.getTranslation(language, "$root.unset")

        } else {
            channelWrapper.setChannel(context.guildId, type, channel.idLong)
            val language = context.getLanguage()
            i18n.getTranslation(language, "$root.set")
                .replace(PLACEHOLDER_CHANNEL, channel.asTag)
        }.replace("%channelType%", type.toString().toUpperWordCase())

        sendMsg(context, msg)
    }
}