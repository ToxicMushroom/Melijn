package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.translation.MESSAGE_SELFINTERACT_MEMBER_HIARCHYEXCEPTION
import me.melijn.melijnbot.internals.translation.PLACEHOLDER_USER
import me.melijn.melijnbot.internals.utils.*
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.message.sendSyntax
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Member

class ChangeNameCommand : AbstractCommand("command.changename") {

    init {
        id = 249
        name = "changeName"
        aliases = arrayOf("nickname", "nick")
        discordPermissions = arrayOf(Permission.NICKNAME_MANAGE)
        permissionRequired = true
        commandCategory = CommandCategory.UTILITY
    }

    override suspend fun execute(context: ICommandContext) {
        if (context.args.isEmpty()) {
            sendSyntax(context)
            return
        }

        val member: Member = if (context.args.size == 1) {
            context.member
        } else {
            retrieveMemberByArgsNMessage(context, 0) ?: return
        }
        if (!context.selfMember.canInteract(member)) {
            val msg = context.getTranslation(MESSAGE_SELFINTERACT_MEMBER_HIARCHYEXCEPTION)
                .withSafeVariable(PLACEHOLDER_USER, member.asTag)
            sendRsp(context, msg)
            return
        }

        val newNickname = getStringFromArgsNMessage(context, context.args.size - 1, 0, 32) ?: return
        val msg = if (newNickname == "-reset") {
            member.modifyNickname(newNickname).reason("(changeName) ${context.author.asTag}").await()

            if (member.idLong == context.member.idLong) {
                context.getTranslation("$root.reset.self")
            } else {
                context.getTranslation("$root.reset.other")
                    .withSafeVariable("user", member.asTag)
            }.withSafeVariable("newName", newNickname)
        } else {
            member.modifyNickname(null).reason("(changeName) ${context.author.asTag}").await()

            if (member.idLong == context.member.idLong) {
                context.getTranslation("$root.changed.self")
            } else {
                context.getTranslation("$root.changed.other")
                    .withSafeVariable("user", member.asTag)
            }.withSafeVariable("newName", newNickname)
        }

        sendRsp(context, msg)
    }
}

