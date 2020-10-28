package me.melijn.melijnbot.commands.moderation

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.translation.PLACEHOLDER_USER
import me.melijn.melijnbot.internals.utils.*
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.message.sendSyntax
import net.dv8tion.jda.api.entities.Member

class VerifyCommand : AbstractCommand("command.verify") {

    init {
        id = 45
        name = "verify"
        commandCategory = CommandCategory.MODERATION
    }

    override suspend fun execute(context: CommandContext) {
        if (context.args.isEmpty()) {
            sendSyntax(context)
            return
        }

        val role = VerificationUtils.getUnverifiedRoleNMessage(context.author, context.textChannel, context.daoManager, context.usedPrefix)
            ?: return

        val msg = if (context.args[0] == "*") {
            val members = context.guild.members.filter { member -> member.roles.contains(role) }
            val failures = mutableListOf<Member>()
            for (member in members) {
                try {
                    if (!VerificationUtils.verify(context.daoManager, context.webManager.proxiedHttpClient, role, context.author, member)) {
                        failures.add(member)
                    }
                } catch (t: Throwable) {
                    failures.add(member)
                }
            }

            if (failures.isEmpty()) {
                context.getTranslation("$root.all")
            } else {
                context.getTranslation("$root.all.failures")
                    .withVariable("failures", failures.joinToString("\n") { member ->
                        member.asTag.escapeMarkdown() + " - " + member.id
                    })

            }.withVariable("count", (members.size - failures.size).toString())

        } else {
            val member = retrieveMemberByArgsNMessage(context, 0) ?: return
            try {
                if (VerificationUtils.verify(context.daoManager, context.webManager.proxiedHttpClient, role, context.author, member)) {
                    context.getTranslation("$root.success")
                } else {
                    context.getTranslation("$root.failure")
                }
            } catch (t: Throwable) {
                context.getTranslation("$root.failure")
            }.withVariable(PLACEHOLDER_USER, member.asTag)
        }

        sendRsp(context, msg)
    }
}
