package me.melijn.melijnbot.internals.utils.message

import me.melijn.melijnbot.database.DaoManager
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.translation.i18n
import me.melijn.melijnbot.internals.utils.asTag
import me.melijn.melijnbot.internals.utils.toUCSC
import me.melijn.melijnbot.internals.utils.withSafeVariable
import me.melijn.melijnbot.internals.utils.withVariable
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.TextChannel

suspend fun sendMissingPermissionMessage(context: CommandContext, permission: String) {
    sendMissingPermissionMessage(context.textChannel, context.daoManager, context.getLanguage(), permission)
}


fun sendMissingPermissionMessage(tc: TextChannel, daoManager: DaoManager, language: String, permission: String) {
    val msg = i18n.getTranslation(language, "message.botpermission.missing")
        .withVariable("permission", permission)
    sendRsp(tc, daoManager, msg)
}

suspend fun sendMelijnMissingChannelPermissionMessage(context: CommandContext, permissions: List<Permission>) {
    sendMelijnMissingChannelPermissionMessage(context.textChannel, context.getLanguage(), context.daoManager, permissions)
}

suspend fun sendMelijnMissingChannelPermissionMessage(textChannel: TextChannel, language: String, daoManager: DaoManager, permissions: List<Permission>) {
    val more = if (permissions.size > 1) "s" else ""
    val permString = permissions.joinToString("\n") {
        "    ⁎ `${it.toUCSC()}`"
    }

    val msg = i18n.getTranslation(language, "message.discordchannelpermission$more.missing")
        .withVariable("permissions", "\n" + permString)
        .withSafeVariable("channel", textChannel.asTag)

    sendRspOrMsg(textChannel, daoManager, msg)
}

suspend fun sendMelijnMissingPermissionMessage(textChannel: TextChannel, language: String, daoManager: DaoManager, permissions: List<Permission>) {
    val more = if (permissions.size > 1) "s" else ""
    val permString = permissions.joinToString("\n") {
        "    ⁎ `${it.toUCSC()}`"
    }

    val msg = i18n.getTranslation(language, "message.discordpermission$more.missing")
        .withVariable("permissions", "\n" + permString)

    sendRspOrMsg(textChannel, daoManager, msg)
}