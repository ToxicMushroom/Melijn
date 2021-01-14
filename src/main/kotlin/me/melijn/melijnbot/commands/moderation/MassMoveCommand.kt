package me.melijn.melijnbot.commands.moderation

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.translation.PLACEHOLDER_CHANNEL
import me.melijn.melijnbot.internals.utils.getVoiceChannelByArgNMessage
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.message.sendSyntax
import me.melijn.melijnbot.internals.utils.notEnoughPermissionsAndMessage
import me.melijn.melijnbot.internals.utils.withSafeVariable
import me.melijn.melijnbot.internals.utils.withVariable
import net.dv8tion.jda.api.Permission

class MassMoveCommand : AbstractCommand("command.massmove") {

    init {
        id = 160
        name = "massMove"
        aliases = arrayOf("mm")
        discordPermissions = arrayOf(Permission.VOICE_MOVE_OTHERS)
        commandCategory = CommandCategory.MODERATION
    }

    override suspend fun execute(context: ICommandContext) {
        if (context.args.size < 2) {
            sendSyntax(context)
            return
        }

        var total = 0
        if (context.args[0] == "all") {
            if (context.args[1] == "null") {
                for (voiceChannel in context.guild.voiceChannels) {
                    voiceChannel.members.forEach {
                        voiceChannel.guild.moveVoiceMember(it, null).queue()
                        total++
                    }
                }

                val msg = context.getTranslation("$root.kicked.all")
                    .withVariable("amount", "$total")
                sendRsp(context, msg)
                return
            }

            val voiceChannelTarget = getVoiceChannelByArgNMessage(context, 1) ?: return
            if (notEnoughPermissionsAndMessage(
                    context,
                    voiceChannelTarget,
                    Permission.VOICE_CONNECT,
                    Permission.VOICE_MOVE_OTHERS
                )
            ) return

            for (voiceChannel in context.guild.voiceChannels) {
                voiceChannel.members.forEach {
                    if (voiceChannel.idLong != voiceChannelTarget.idLong) {
                        voiceChannel.guild.moveVoiceMember(it, voiceChannelTarget).queue()
                        total++
                    }
                }
            }

            val msg = context.getTranslation("$root.moved.all")
                .withVariable("amount", "$total")
                .withSafeVariable(PLACEHOLDER_CHANNEL, voiceChannelTarget.name)
            sendRsp(context, msg)
            return
        }

        val voiceChannel = getVoiceChannelByArgNMessage(context, 0) ?: return

        if (context.args[1] == "null") {
            voiceChannel.members.forEach {
                voiceChannel.guild.moveVoiceMember(it, null).queue()
                total++
            }

            val msg = context.getTranslation("$root.kicked")
                .withVariable("amount", "$total")
                .withSafeVariable(PLACEHOLDER_CHANNEL, voiceChannel.name)
            sendRsp(context, msg)
            return
        }

        val voiceChannelTarget = getVoiceChannelByArgNMessage(context, 1) ?: return
        if (notEnoughPermissionsAndMessage(
                context,
                voiceChannelTarget,
                Permission.VOICE_CONNECT,
                Permission.VOICE_MOVE_OTHERS
            )
        ) return

        voiceChannel.members.forEach {
            if (voiceChannel.idLong != voiceChannelTarget.idLong) {
                voiceChannel.guild.moveVoiceMember(it, voiceChannelTarget).queue()
                total++
            }
        }

        val msg = context.getTranslation("$root.moved")
            .withVariable("amount", "$total")
            .withSafeVariable(PLACEHOLDER_CHANNEL, voiceChannel.name)
            .withSafeVariable("channel1", voiceChannelTarget.name)
        sendRsp(context, msg)
    }
}