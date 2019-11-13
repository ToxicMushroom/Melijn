package me.melijn.melijnbot.commands.administration

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.PLACEHOLDER_CHANNEL
import me.melijn.melijnbot.objects.translation.i18n
import me.melijn.melijnbot.objects.utils.getVoiceChannelByArgNMessage
import me.melijn.melijnbot.objects.utils.sendMsg

class SetMusicChannelCommand : AbstractCommand("command.setmusicchannel") {

    init {
        id = 119
        name = "setMusicChannel"
        aliases = arrayOf("smc")
        commandCategory = CommandCategory.ADMINISTRATION
    }

    override suspend fun execute(context: CommandContext) {
        val wrapper = context.daoManager.musicChannelWrapper
        if (context.args.isEmpty()) {
            val channel = wrapper.musicChannelCache.get(context.guildId).await()
            val vc = context.guild.getVoiceChannelById(channel)
            val vcName = vc?.name
            val extra = if (vcName == null) "unset" else "set"
            val msg = i18n.getTranslation(context, "$root.show.$extra")
                .replace(PLACEHOLDER_CHANNEL, vcName ?: "/")
            sendMsg(context, msg)
        } else {
            val msg = if (context.args[0] == "null") {
                wrapper.setChannel(context.guildId, -1)
                i18n.getTranslation(context, "$root.unset.success")
            } else {
                val channel = getVoiceChannelByArgNMessage(context, 0) ?: return
                wrapper.setChannel(context.guildId, channel.idLong)
                i18n.getTranslation(context, "$root.set.success")
                    .replace(PLACEHOLDER_CHANNEL, channel.name)
            }
            sendMsg(context, msg)
        }
    }
}