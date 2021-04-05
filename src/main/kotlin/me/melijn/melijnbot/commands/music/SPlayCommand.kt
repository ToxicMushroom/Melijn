package me.melijn.melijnbot.commands.music

import me.melijn.melijnbot.enums.SearchType
import me.melijn.melijnbot.internals.command.*
import me.melijn.melijnbot.internals.music.LavaManager
import me.melijn.melijnbot.internals.utils.message.sendMissingPermissionMessage
import me.melijn.melijnbot.internals.utils.message.sendSyntax
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

    suspend fun execute(context: ICommandContext) {
        if (context.args.isEmpty()) {
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

        var songPosition = NextSongPosition.getPosByTrigger(args[0])
        if (songPosition == null) {
            songArg = context.rawArg.trim()
            songPosition = NextSongPosition.BOTTOM
        }

        if (!hasPermission(context, "$root.yt")) {
            sendMissingPermissionMessage(context, "$root.yt")
            return
        }

        val groupId = context.getGuildMusicPlayer().groupId
        if (botChannel == null && senderVoiceChannel != null && !lava.tryToConnectToVCNMessage(
                context,
                senderVoiceChannel,
                groupId
            )
        ) return

        context.audioLoader.loadNewTrackPickerNMessage(context, songArg, SearchType.YT, songPosition)
    }

    class YTArg(parent: String) : AbstractCommand("$parent.yt") {

        init {
            name = "yt"
            aliases = arrayOf("youtube")
        }


        suspend fun execute(context: ICommandContext) {
            val member = context.member
            val senderVoiceChannel = member.voiceState?.channel
            val botChannel = context.lavaManager.getConnectedChannel(context.guild)
            if (senderVoiceChannel == null && botChannel == null) throw IllegalArgumentException("Fix vc_bot_or_user_dj")
            val lava: LavaManager = context.lavaManager

            val args = context.oldArgs
            var songArg = context.getRawArgPart(1, -1)

            var songPosition = NextSongPosition.getPosByTrigger(args[0])
            if (songPosition == null) {
                songArg = context.rawArg.trim()
                songPosition = NextSongPosition.BOTTOM
            }

            val groupId = context.getGuildMusicPlayer().groupId
            if (botChannel == null && senderVoiceChannel != null && !lava.tryToConnectToVCNMessage(
                    context,
                    senderVoiceChannel,
                    groupId
                )
            ) return
            context.audioLoader.loadNewTrackPickerNMessage(context, songArg, SearchType.YT, songPosition)
        }

    }

    class SCArg(parent: String) : AbstractCommand("$parent.sc") {

        init {
            name = "sc"
            aliases = arrayOf("soundcloud")
        }


        suspend fun execute(context: ICommandContext) {
            val member = context.member
            val senderVoiceChannel = member.voiceState?.channel
            val botChannel = context.lavaManager.getConnectedChannel(context.guild)
            if (senderVoiceChannel == null && botChannel == null) throw IllegalArgumentException("Fix vc_bot_or_user_dj")
            val lava: LavaManager = context.lavaManager

            val args = context.oldArgs
            var songArg = context.getRawArgPart(1, -1)

            var songPosition = NextSongPosition.getPosByTrigger(args[0])
            if (songPosition == null) {
                songArg = context.rawArg.trim()
                songPosition = NextSongPosition.BOTTOM
            }

            val groupId = context.getGuildMusicPlayer().groupId
            if (botChannel == null && senderVoiceChannel != null && !lava.tryToConnectToVCNMessage(
                    context,
                    senderVoiceChannel,
                    groupId
                )
            ) return
            context.audioLoader.loadNewTrackPickerNMessage(context, songArg, SearchType.SC, songPosition)
        }
    }
}