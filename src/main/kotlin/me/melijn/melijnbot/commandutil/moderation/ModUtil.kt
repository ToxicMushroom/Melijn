package me.melijn.melijnbot.commandutil.moderation

import me.melijn.melijnbot.enums.SpecialPermission
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.command.hasPermission
import me.melijn.melijnbot.internals.translation.MESSAGE_INTERACT_MEMBER_HIARCHYEXCEPTION
import me.melijn.melijnbot.internals.translation.MESSAGE_SELFINTERACT_MEMBER_HIARCHYEXCEPTION
import me.melijn.melijnbot.internals.translation.PLACEHOLDER_USER
import me.melijn.melijnbot.internals.utils.asTag
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.withSafeVariable
import net.dv8tion.jda.api.entities.Member

object ModUtil {

    suspend fun cantPunishAndReply(context: ICommandContext, member: Member): Boolean {
        if (!context.guild.selfMember.canInteract(member)) {
            val msg = context.getTranslation(MESSAGE_SELFINTERACT_MEMBER_HIARCHYEXCEPTION)
                .withSafeVariable(PLACEHOLDER_USER, member.asTag)
            sendRsp(context, msg)
            return true
        }

        if (!context.member.canInteract(member) &&
            !hasPermission(context, SpecialPermission.PUNISH_BYPASS_HIGHER.node, true)
        ) {
            val msg = context.getTranslation(MESSAGE_INTERACT_MEMBER_HIARCHYEXCEPTION)
                .withSafeVariable(PLACEHOLDER_USER, member.asTag)
            sendRsp(context, msg)
            return true
        }

        return false
    }



}