package me.melijn.melijnbot.commands.music

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.command.hasPermission
import me.melijn.melijnbot.objects.translation.i18n
import me.melijn.melijnbot.objects.utils.RunConditionUtil
import me.melijn.melijnbot.objects.utils.getVoiceChannelByArgNMessage
import me.melijn.melijnbot.objects.utils.sendMsg

class SummonCommand : AbstractCommand("command.summon") {

    init {
        id = 94
        name = "summon"
        aliases = arrayOf("joinChannel")
        commandCategory = CommandCategory.MUSIC
    }

    override suspend fun execute(context: CommandContext) {
        if (context.args.isEmpty()) {
            if (!RunConditionUtil.checkOtherOrSameVCBotAloneOrUserDJ(context.container, context.event, this, context.getLanguage())) return
            val vc = context.member.voiceState?.channel ?: throw IllegalStateException("I messed up")
            context.lavaManager.openConnection(vc)
            val msg = i18n.getTranslation(context, "$root.summoned")
            sendMsg(context, msg)
        } else {
            val vc = getVoiceChannelByArgNMessage(context, 0) ?: return
            if (!hasPermission(context, "summon.other", true)) {
                sendMissingPermissionMessage(context, "summon.other")
                return
            }
            if (!RunConditionUtil.checkBotAloneOrUserDJ(context.container, context.event, this, context.getLanguage())) return
            context.lavaManager.openConnection(vc)
            val msg = i18n.getTranslation(context, "$root.summoned")
            sendMsg(context, msg)
        }
    }
}