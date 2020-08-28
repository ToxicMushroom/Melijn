package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.embed.Embedder
import me.melijn.melijnbot.internals.utils.asEpochMillisToDateTime
import me.melijn.melijnbot.internals.utils.getTextChannelByArgsN
import me.melijn.melijnbot.internals.utils.getVoiceChannelByArgsN
import me.melijn.melijnbot.internals.utils.message.sendEmbedRsp
import me.melijn.melijnbot.internals.utils.message.sendSyntax
import net.dv8tion.jda.api.Permission

class ChannelInfoCommand : AbstractCommand("command.channelinfo") {

    init {
        id = 211
        name = "channelInfo"
        aliases = arrayOf("ci", "channel")
        commandCategory = CommandCategory.UTILITY
    }

    override suspend fun execute(context: CommandContext) {
        if (context.args.isEmpty()) {
            sendSyntax(context)
            return
        }
        val text = getTextChannelByArgsN(context, 0)
        val voice = getVoiceChannelByArgsN(context, 0)
        val selfMember = context.selfMember
        if (text != null) {
            val eb = Embedder(context)
                .setTitle("TextChannel Info")
                .setDescription("**Name** ${text.name}\n" +
                    "**ID** ${text.id}\n" +
                    "**Creation Time** ${text.timeCreated.asEpochMillisToDateTime(context.getTimeZoneId())}\n" +
                    "**Position** ${text.position}\n" +
                    "**Topic** ${text.topic}\n" +
                    "**Nsfw** ${text.isNSFW}\n" +
                    "**Slowmode** ${text.slowmode}s\n" +
                    "**Mention** ${text.asMention}\n" +
                    "**Link** [click](https://discord.com/channels/${text.guild.id}/${text.id})\n" +
                    "**IsNews** ${text.isNews}\n" +
                    "**Can Melijn Talk** ${text.canTalk()}")
            sendEmbedRsp(context, eb.build())

        } else if (voice != null) {
            val eb = Embedder(context)
                .setTitle("VoiceChannel Info")
                .setDescription("**Name** ${voice.name}\n" +
                    "**Creation Time** ${voice.timeCreated.asEpochMillisToDateTime(context.getTimeZoneId())}\n" +
                    "**Position** ${voice.position}\n" +
                    "**UserLimit** ${voice.userLimit}\n" +
                    "**Connected Users** ${voice.members.size}\n" +
                    "**Link** [click](https://discord.com/channels/${voice.guild.id}/${voice.id})\n" +
                    "**Can Melijn Speak** ${selfMember.hasPermission(voice, Permission.VOICE_SPEAK)}\n" +
                    "**Can Melijn Connect** ${selfMember.hasPermission(voice, Permission.VOICE_CONNECT)}")
            sendEmbedRsp(context, eb.build())

        } else {
            sendSyntax(context)
            return
        }
    }
}