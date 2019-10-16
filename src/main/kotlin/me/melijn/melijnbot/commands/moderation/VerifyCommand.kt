package me.melijn.melijnbot.commands.moderation

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.PLACEHOLDER_USER
import me.melijn.melijnbot.objects.translation.i18n
import me.melijn.melijnbot.objects.utils.*
import net.dv8tion.jda.api.entities.Member

class VerifyCommand : AbstractCommand("command.verify") {

    init {
        id = 45
        name = "verify"
        commandCategory = CommandCategory.MODERATION
    }

    override suspend fun execute(context: CommandContext) {
        if (context.args.isEmpty()) {
            sendSyntax(context, syntax)
            return
        }

        val language = context.getLanguage()
        val role = VerificationUtils.getUnverifiedRoleNMessage(context.getAuthor(), context.getTextChannel(), context.daoManager)
            ?: return

        val msg = if (context.args[0] == "*") {
            val members = context.getGuild().members.filter { member -> member.roles.contains(role) }
            val failures = mutableListOf<Member>()
            for (member in members) {
                try {
                    VerificationUtils.verify(context.daoManager, role, context.getAuthor(), member)
                } catch (t: Throwable) {
                    failures.add(member)
                }
            }

            if (failures.isEmpty()) {
                i18n.getTranslation(language, "$root.all")
            } else {
                i18n.getTranslation(language, "$root.all.failures")
                    .replace("%failures%", failures.joinToString("\n") { member ->
                        member.asTag + " - " + member.id
                    }
                    )

            }.replace("%count%", (members.size - failures.size).toString())

        } else {
            val member = getMemberByArgsNMessage(context, 0) ?: return
            try {
                VerificationUtils.verify(context.daoManager, role, context.getAuthor(), member)
                i18n.getTranslation(language, "$root.success")
            } catch (t: Throwable) {
                i18n.getTranslation(language, "$root.failure")
            }.replace(PLACEHOLDER_USER, member.asTag)
        }
        sendMsg(context, msg)


    }
}
