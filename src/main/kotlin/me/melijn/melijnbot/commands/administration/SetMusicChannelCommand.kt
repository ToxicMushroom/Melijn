package me.melijn.melijnbot.commands.administration

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.translation.PLACEHOLDER_CHANNEL
import me.melijn.melijnbot.internals.utils.checks.getAndVerifyMusicChannel
import me.melijn.melijnbot.internals.utils.getVoiceChannelByArgNMessage
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.withVariable

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
            val vc = context.guild.getAndVerifyMusicChannel(context.daoManager)
            val vcName = vc?.name
            val extra = if (vcName == null) {
                "unset"
            } else {
                "set"
            }

            val msg = context.getTranslation("$root.show.$extra")
                .withVariable(PLACEHOLDER_CHANNEL, vcName ?: "/")
            sendRsp(context, msg)

        } else {
            val msg = if (context.args[0] == "null") {
                wrapper.setChannel(context.guildId, -1)
                context.getTranslation("$root.unset.success")
            } else {
                val channel = getVoiceChannelByArgNMessage(context, 0) ?: return
                wrapper.setChannel(context.guildId, channel.idLong)
                context.getTranslation("$root.set.success")
                    .withVariable(PLACEHOLDER_CHANNEL, channel.name)
            }

            sendRsp(context, msg)
        }
    }
}