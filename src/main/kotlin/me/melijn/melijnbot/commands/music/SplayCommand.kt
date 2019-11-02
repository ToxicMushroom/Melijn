package me.melijn.melijnbot.commands.music

import me.melijn.melijnbot.objects.command.*
import me.melijn.melijnbot.objects.music.LavaManager
import me.melijn.melijnbot.objects.translation.SC_SELECTOR
import me.melijn.melijnbot.objects.translation.YT_SELECTOR
import me.melijn.melijnbot.objects.utils.sendSyntax
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.VoiceChannel

class SplayCommand : AbstractCommand("command.splay") {

    init {
        id = 95
        name = "splay"
        aliases = arrayOf("sp", "search", "searchPlay")
        children = arrayOf(YTArg(root), SCArg(root))
        runConditions = arrayOf(RunCondition.VC_BOT_OR_USER_DJ)
        discordPermissions = arrayOf(Permission.MESSAGE_ADD_REACTION)
        commandCategory = CommandCategory.MUSIC
    }

    override suspend fun execute(context: CommandContext) {
        if (context.args.isEmpty() && context.getMessage().attachments.isEmpty()) {
            sendSyntax(context, syntax)
            return
        }

        val member = context.getMember() ?: return
        val senderVoiceChannel: VoiceChannel = member.voiceState?.channel ?: return
        val lava: LavaManager = context.lavaManager

        val songArg = context.rawArg.trim()


        if (!hasPermission(context, "$root.yt")) {
            sendMissingPermissionMessage(context, "$root.yt")
            return
        }
        if (!lava.tryToConnectToVCNMessage(context, senderVoiceChannel)) return

        context.audioLoader.loadNewTrackPickerNMessage(context, "$YT_SELECTOR$songArg")

    }

    class YTArg(root: String) : AbstractCommand("$root.yt") {

        init {
            name = "yt"
            aliases = arrayOf("youtube")
        }


        override suspend fun execute(context: CommandContext) {
            val member = context.getMember() ?: return
            val senderVoiceChannel: VoiceChannel = member.voiceState?.channel ?: return
            val lava: LavaManager = context.lavaManager

            val songArg = context.rawArg.trim()

            if (!lava.tryToConnectToVCNMessage(context, senderVoiceChannel)) return
            context.audioLoader.loadNewTrackPickerNMessage(context, "$YT_SELECTOR$songArg")
        }

    }

    class SCArg(root: String) : AbstractCommand("$root.sc") {

        init {
            name = "sc"
            aliases = arrayOf("soundcloud")
        }


        override suspend fun execute(context: CommandContext) {
            val member = context.getMember() ?: return
            val senderVoiceChannel: VoiceChannel = member.voiceState?.channel ?: return
            val lava: LavaManager = context.lavaManager

            val songArg = context.rawArg.trim()

            if (!lava.tryToConnectToVCNMessage(context, senderVoiceChannel)) return
            context.audioLoader.loadNewTrackPickerNMessage(context, "$SC_SELECTOR$songArg")
        }
    }

}