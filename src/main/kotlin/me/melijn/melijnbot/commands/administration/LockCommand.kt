package me.melijn.melijnbot.commands.administration

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.translation.PLACEHOLDER_CHANNEL
import me.melijn.melijnbot.internals.utils.*
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.message.sendSyntax
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.GuildChannel
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.entities.VoiceChannel

class LockCommand : AbstractCommand("command.lock") {

    init {
        id = 241
        name = "lock"
        aliases = arrayOf("lockChannel")
        discordPermissions = (textDenyList + voiceDenyList).toTypedArray() // need the server permission in order to create overrides for it
        cooldown = 10_000
        commandCategory = CommandCategory.ADMINISTRATION
    }

    companion object {
        val textDenyList = mutableListOf(Permission.MESSAGE_WRITE, Permission.MESSAGE_ADD_REACTION)
        val voiceDenyList = mutableListOf(Permission.VOICE_CONNECT)
    }

    override suspend fun execute(context: CommandContext) {
        val textChannel = getTextChannelByArgsN(context, 0)
        if (textChannel != null) {
            lockChannel(context, textChannel)
            context.initCooldown()
            return
        }

        val voiceChannel = getVoiceChannelByArgsN(context, 0)
        if (voiceChannel != null) {
            lockChannel(context, voiceChannel)
            context.initCooldown()
            return
        } else {
            sendSyntax(context)
            return
        }
    }

    private suspend fun lockChannel(context: CommandContext, channel: GuildChannel) {
        if (notEnoughPermissionsAndMessage(context, channel, Permission.MANAGE_CHANNEL, Permission.MANAGE_ROLES)) return
        val denyList = if (channel is TextChannel) textDenyList else if (channel is VoiceChannel) voiceDenyList else return
        val overrides = channel.rolePermissionOverrides.filterNotNull()
        val overrideMap = overrides.map { it.idLong to Pair(it.allowedRaw, it.deniedRaw) }.toMap()
        val discordChannelOverridesWrapper = context.daoManager.discordChannelOverridesWrapper

        if (discordChannelOverridesWrapper.getAll(context.guildId, channel.idLong).isNotEmpty()) {
            val msg = context.getTranslation("$root.alreadylocked")
                .withVariable(PLACEHOLDER_CHANNEL, channel.name)
            sendRsp(context, msg)
            return
        }

        discordChannelOverridesWrapper.bulkPut(context.guildId, channel.idLong, overrideMap)

        val everyoneRole = context.guild.publicRole
        for (override in overrides) {
            override.role?.let { holder: Role -> // ignore member overrides
                channel.upsertPermissionOverride(holder) // interact permission not needed
                    .deny(denyList)
                    .reason("(lock) " + context.author.asTag)
                    .await()
            }
        }
        if (overrides.none { it.role?.idLong == everyoneRole.idLong }) {
            channel.upsertPermissionOverride(everyoneRole) // interact permission not needed
                .deny(denyList)
                .reason("(lock) " + context.author.asTag)
                .await()

            discordChannelOverridesWrapper.put(
                context.guildId, channel.idLong, everyoneRole.idLong, 0, 0
            )
        }

        val msg = context.getTranslation("$root.locked")
            .withVariable(PLACEHOLDER_CHANNEL, channel.name)
        sendRsp(context, msg)
    }
}