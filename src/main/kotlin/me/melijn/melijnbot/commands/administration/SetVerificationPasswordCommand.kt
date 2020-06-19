package me.melijn.melijnbot.commands.administration

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.PLACEHOLDER_ARG
import me.melijn.melijnbot.objects.utils.sendMsg

class SetVerificationPasswordCommand : AbstractCommand("command.setverificationpassword") {

    init {
        id = 42
        name = "setVerificationPassword"
        aliases = arrayOf("svp")
        commandCategory = CommandCategory.ADMINISTRATION
    }

    override suspend fun execute(context: CommandContext) {
        val wrapper = context.daoManager.verificationPasswordWrapper
        if (context.args.isEmpty()) {
            val password = wrapper.verificationPasswordCache.get(context.guildId).await()
            val part = if (password.isBlank()) {
                "unset"
            } else {
                "set"
            }

            val msg = context.getTranslation("$root.show.$part")
                .replace("%password%", password)
            sendMsg(context, msg)
            return
        }

        val msg = if (context.rawArg == "null") {
            wrapper.remove(context.guildId)
            context.getTranslation("$root.unset")
        } else {
            wrapper.set(context.guildId, context.rawArg)
            context.getTranslation("$root.set")
                .replace(PLACEHOLDER_ARG, context.rawArg)
        }

        sendMsg(context, msg)
    }
}