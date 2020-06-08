package me.melijn.melijnbot.commands.music

import me.melijn.melijnbot.objects.command.*
import me.melijn.melijnbot.objects.music.LavaManager
import me.melijn.melijnbot.objects.translation.SC_SELECTOR
import me.melijn.melijnbot.objects.translation.YT_SELECTOR
import me.melijn.melijnbot.objects.utils.sendSyntax
import net.dv8tion.jda.api.Permission

class SPlayCommand : AbstractCommand("command.splay") {

    init {
        id = 95
        name = "splay"
        aliases = arrayOf("sp", "search", "searchPlay")
        children = arrayOf(
            YTArg(root),
            SCArg(root)
        )
        runConditions = arrayOf(RunCondition.VC_BOT_OR_USER_DJ, RunCondition.VOTED)
        discordChannelPermissions = arrayOf(Permission.MESSAGE_ADD_REACTION)
        commandCategory = CommandCategory.MUSIC
    }

    override suspend fun execute(context: CommandContext) {
        if (context.args.isEmpty() && context.message.attachments.isEmpty()) {
            sendSyntax(context)
            return
        }

        val member = context.member
        val senderVoiceChannel = member.voiceState?.channel
        val botChannel = context.lavaManager.getConnectedChannel(context.guild)
        if (senderVoiceChannel == null && botChannel == null) throw IllegalArgumentException("Fix vc_bot_or_user_dj")
        val lava: LavaManager = context.lavaManager

        val args = context.oldArgs
        var songArg = context.getRawArgPart(1, -1)

        val songPosition = when {
            args[0] == "-t" || args[0] == "-top" -> NextSongPosition.TOP
            args[0] == "-r" || args[0] == "-random" -> NextSongPosition.RANDOM
            args[0] == "-b" || args[0] == "-bottom" -> NextSongPosition.BOTTOM
            else -> {
                songArg = context.rawArg.trim()
                NextSongPosition.BOTTOM
            }
        }

        if (!hasPermission(context, "$root.yt")) {
            sendMissingPermissionMessage(context, "$root.yt")
            return
        }

        val premium = context.daoManager.musicNodeWrapper.isPremium(context.guildId)
        if (botChannel == null && senderVoiceChannel != null && !lava.tryToConnectToVCNMessage(context, senderVoiceChannel, premium)) return

        context.audioLoader.loadNewTrackPickerNMessage(context, "$YT_SELECTOR$songArg", songPosition)
    }

    class YTArg(parent: String) : AbstractCommand("$parent.yt") {

        init {
            name = "yt"
            aliases = arrayOf("youtube")
        }


        override suspend fun execute(context: CommandContext) {
            val member = context.member
            val senderVoiceChannel = member.voiceState?.channel
            val botChannel = context.lavaManager.getConnectedChannel(context.guild)
            if (senderVoiceChannel == null && botChannel == null) throw IllegalArgumentException("Fix vc_bot_or_user_dj")
            val lava: LavaManager = context.lavaManager

            val args = context.oldArgs
            var songArg = context.getRawArgPart(1, -1)

            val songPosition = when {
                args[0] == "-t" || args[0] == "-top" -> NextSongPosition.TOP
                args[0] == "-r" || args[0] == "-random" -> NextSongPosition.RANDOM
                args[0] == "-b" || args[0] == "-bottom" -> NextSongPosition.BOTTOM
                else -> {
                    songArg = context.rawArg.trim()
                    NextSongPosition.BOTTOM
                }
            }

            val premium = context.daoManager.musicNodeWrapper.isPremium(context.guildId)
            if (botChannel == null && senderVoiceChannel != null && !lava.tryToConnectToVCNMessage(context, senderVoiceChannel, premium)) return
            context.audioLoader.loadNewTrackPickerNMessage(context, "$YT_SELECTOR$songArg", songPosition)
        }

    }

    class SCArg(parent: String) : AbstractCommand("$parent.sc") {

        init {
            name = "sc"
            aliases = arrayOf("soundcloud")
        }


        override suspend fun execute(context: CommandContext) {
            val member = context.member
            val senderVoiceChannel = member.voiceState?.channel
            val botChannel = context.lavaManager.getConnectedChannel(context.guild)
            if (senderVoiceChannel == null && botChannel == null) throw IllegalArgumentException("Fix vc_bot_or_user_dj")
            val lava: LavaManager = context.lavaManager

            val args = context.oldArgs
            var songArg = context.getRawArgPart(1, -1)

            val songPosition = when {
                args[0] == "-t" || args[0] == "-top" -> NextSongPosition.TOP
                args[0] == "-r" || args[0] == "-random" -> NextSongPosition.RANDOM
                args[0] == "-b" || args[0] == "-bottom" -> NextSongPosition.BOTTOM
                else -> {
                    songArg = context.rawArg.trim()
                    NextSongPosition.BOTTOM
                }
            }

            val premium = context.daoManager.musicNodeWrapper.isPremium(context.guildId)
            if (botChannel == null && senderVoiceChannel != null && !lava.tryToConnectToVCNMessage(context, senderVoiceChannel, premium)) return
            context.audioLoader.loadNewTrackPickerNMessage(context, "$SC_SELECTOR$songArg", songPosition)
        }
    }
}