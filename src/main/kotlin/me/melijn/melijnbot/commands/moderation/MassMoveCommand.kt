package me.melijn.melijnbot.commands.moderation

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.PLACEHOLDER_CHANNEL
import me.melijn.melijnbot.objects.utils.getVoiceChannelByArgNMessage
import me.melijn.melijnbot.objects.utils.notEnoughPermissionsAndMessage
import me.melijn.melijnbot.objects.utils.sendMsg
import me.melijn.melijnbot.objects.utils.sendSyntax
import net.dv8tion.jda.api.Permission

class MassMoveCommand : AbstractCommand("command.massmove") {

    init {
        id = 160
        name = "massMove"
        aliases = arrayOf("mm")
        discordPermissions = arrayOf(Permission.VOICE_MOVE_OTHERS)
        commandCategory = CommandCategory.MODERATION
    }

    override suspend fun execute(context: CommandContext) {
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
                    .replace("%amount%", "$total")
                sendMsg(context, msg)
                return
            }

            val voiceChannelTarget = getVoiceChannelByArgNMessage(context, 1) ?: return
            if (notEnoughPermissionsAndMessage(context, voiceChannelTarget, Permission.VOICE_CONNECT, Permission.VOICE_MOVE_OTHERS)) return

            for (voiceChannel in context.guild.voiceChannels) {
                voiceChannel.members.forEach {
                    if (voiceChannel.idLong != voiceChannelTarget.idLong) {
                        voiceChannel.guild.moveVoiceMember(it, voiceChannelTarget).queue()
                        total++
                    }
                }
            }

            val msg = context.getTranslation("$root.moved.all")
                .replace("%amount%", "$total")
                .replace(PLACEHOLDER_CHANNEL, voiceChannelTarget.name)
            sendMsg(context, msg)
            return
        }

        val voiceChannel = getVoiceChannelByArgNMessage(context, 0) ?: return

        if (context.args[1] == "null") {
            voiceChannel.members.forEach {
                voiceChannel.guild.moveVoiceMember(it, null).queue()
                total++
            }

            val msg = context.getTranslation("$root.kicked")
                .replace("%amount%", "$total")
                .replace(PLACEHOLDER_CHANNEL, voiceChannel.name)
            sendMsg(context, msg)
            return
        }

        val voiceChannelTarget = getVoiceChannelByArgNMessage(context, 1) ?: return
        if (notEnoughPermissionsAndMessage(context, voiceChannelTarget, Permission.VOICE_CONNECT, Permission.VOICE_MOVE_OTHERS)) return

        voiceChannel.members.forEach {
            if (voiceChannel.idLong != voiceChannelTarget.idLong) {
                voiceChannel.guild.moveVoiceMember(it, voiceChannelTarget).queue()
                total++
            }
        }

        val msg = context.getTranslation("$root.moved")
            .replace("%amount%", "$total")
            .replace(PLACEHOLDER_CHANNEL, voiceChannel.name)
            .replace("%channel1%", voiceChannelTarget.name)
        sendMsg(context, msg)

    }
}