package me.melijn.melijnbot.commands.music

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.command.hasPermission
import me.melijn.melijnbot.internals.translation.PLACEHOLDER_CHANNEL
import me.melijn.melijnbot.internals.utils.RunConditionUtil
import me.melijn.melijnbot.internals.utils.getVoiceChannelByArgNMessage
import me.melijn.melijnbot.internals.utils.message.sendMissingPermissionMessage
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.notEnoughPermissionsAndMessage
import me.melijn.melijnbot.internals.utils.withSafeVariable
import net.dv8tion.jda.api.Permission

class SummonCommand : AbstractCommand("command.summon") {

    init {
        id = 94
        name = "summon"
        aliases = arrayOf("join", "joinChannel")
        commandCategory = CommandCategory.MUSIC
    }

    override suspend fun execute(context: CommandContext) {
        if (context.args.isEmpty()) {
            if (!RunConditionUtil.checkOtherBotAloneOrDJOrSameVC(
                    context.container,
                    context.event,
                    this,
                    context.getLanguage()
                )
            ) return
            val vc = context.member.voiceState?.channel ?: throw IllegalStateException("I messed up")
            if (notEnoughPermissionsAndMessage(context, vc, Permission.VOICE_SPEAK, Permission.VOICE_CONNECT)) return

            context.lavaManager.openConnection(vc, context.getGuildMusicPlayer().groupId)
            val msg = context.getTranslation("$root.summoned")
            sendRsp(context, msg)

        } else {
            val vc = getVoiceChannelByArgNMessage(context, 0) ?: return
            if (!hasPermission(context, "summon.other", true)) {
                sendMissingPermissionMessage(context, "summon.other")
                return
            }
            if (notEnoughPermissionsAndMessage(context, vc, Permission.VOICE_SPEAK, Permission.VOICE_CONNECT)) return
            if (!RunConditionUtil.checkBotAloneOrUserDJ(
                    context.container,
                    context.event,
                    this,
                    context.getLanguage()
                )
            ) return

            context.lavaManager.openConnection(vc, context.getGuildMusicPlayer().groupId)
            val msg = context.getTranslation("$root.summoned.other")
                .withSafeVariable(PLACEHOLDER_CHANNEL, vc.name)
            sendRsp(context, msg)
        }
    }
}