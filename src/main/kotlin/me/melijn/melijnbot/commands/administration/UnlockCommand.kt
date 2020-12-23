package me.melijn.melijnbot.commands.administration

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.translation.PLACEHOLDER_CHANNEL
import me.melijn.melijnbot.internals.utils.getTextChannelByArgsN
import me.melijn.melijnbot.internals.utils.getVoiceChannelByArgsN
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.message.sendSyntax
import me.melijn.melijnbot.internals.utils.notEnoughPermissionsAndMessage
import me.melijn.melijnbot.internals.utils.withVariable
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.GuildChannel

class UnlockCommand : AbstractCommand("command.unlock") {

    init {
        id = 242
        name = "unlock"
        aliases = arrayOf("unlockChannel")
        discordPermissions =
            arrayOf(Permission.MESSAGE_WRITE) // need the server permission in order to create overrides for it
        commandCategory = CommandCategory.ADMINISTRATION
    }

    override suspend fun execute(context: CommandContext) {
        val textChannel = getTextChannelByArgsN(context, 0)
        if (textChannel != null) {
            unlockChannel(context, textChannel)
            return
        }

        val voiceChannel = getVoiceChannelByArgsN(context, 0)
        if (voiceChannel != null) {
            unlockChannel(context, voiceChannel)

            return
        } else {
            sendSyntax(context)
            return
        }
    }

    private suspend fun unlockChannel(context: CommandContext, channel: GuildChannel) {
        if (notEnoughPermissionsAndMessage(context, channel, Permission.MANAGE_CHANNEL, Permission.MANAGE_ROLES)) return
        val overrideMap = context.daoManager.discordChannelOverridesWrapper.getAll(
            context.guildId, channel.idLong
        )

        if (overrideMap.isEmpty()) {
            val msg = context.getTranslation("$root.notlocked")
                .withVariable(PLACEHOLDER_CHANNEL, channel.name)
            sendRsp(context, msg)
            return
        }

        for ((id, flags) in overrideMap) {
            val role = context.guild.getRoleById(id) ?: continue

            val manager = channel.upsertPermissionOverride(role)

            for (perm in LockCommand.denyList) {
                when {
                    (flags.first and perm.rawValue) != 0L -> {
                        manager.grant(perm)
                    }
                    (flags.second and perm.rawValue) != 0L -> {
                        manager.deny(perm)
                    }
                    else -> {
                        manager.clear(perm)
                    }
                }
            }
            manager.reason("(unlock) " + context.author.asTag).queue()
        }

        context.daoManager.discordChannelOverridesWrapper.remove(context.guildId, context.channelId)

        sendRsp(context, "🔓 Unlocked **" + channel.name + "**")
    }
}