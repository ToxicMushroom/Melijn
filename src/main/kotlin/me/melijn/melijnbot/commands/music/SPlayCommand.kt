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
        runConditions = arrayOf(RunCondition.VC_BOT_OR_USER_DJ)
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

        val songArg = context.rawArg.trim()


        if (!hasPermission(context, "$root.yt")) {
            sendMissingPermissionMessage(context, "$root.yt")
            return
        }
        if (botChannel == null && senderVoiceChannel != null && !lava.tryToConnectToVCNMessage(context, senderVoiceChannel)) return

        context.audioLoader.loadNewTrackPickerNMessage(context, "$YT_SELECTOR$songArg")
    }

    class YTArg(root: String) : AbstractCommand("$root.yt") {

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

            val songArg = context.rawArg.trim()

            if (botChannel == null && senderVoiceChannel != null && !lava.tryToConnectToVCNMessage(context, senderVoiceChannel)) return
            context.audioLoader.loadNewTrackPickerNMessage(context, "$YT_SELECTOR$songArg")
        }

    }

    class SCArg(root: String) : AbstractCommand("$root.sc") {

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

            val songArg = context.rawArg.trim()

            if (botChannel == null && senderVoiceChannel != null && !lava.tryToConnectToVCNMessage(context, senderVoiceChannel)) return
            context.audioLoader.loadNewTrackPickerNMessage(context, "$SC_SELECTOR$songArg")
        }
    }
}