package me.melijn.melijnbot.commands.administration

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.translation.PLACEHOLDER_CHANNEL
import me.melijn.melijnbot.internals.utils.*
import me.melijn.melijnbot.internals.utils.message.sendRsp
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent

class ClearChannelCommand : AbstractCommand("command.clearchannel") {

    init {
        id = 40
        name = "clearChannel"
        aliases = arrayOf("cChannel")
        discordPermissions = arrayOf(
            Permission.MANAGE_CHANNEL
        )
        cooldown = 10_000
        commandCategory = CommandCategory.ADMINISTRATION
    }

    override suspend fun execute(context: ICommandContext) {
        val textChannel = if (context.args.isEmpty()) {
            context.textChannel
        } else {
            getTextChannelByArgsNMessage(context, 0) ?: return
        }

        // permission check for bot
        if (notEnoughPermissionsAndMessage(context, textChannel, Permission.MANAGE_CHANNEL)) return
        if (missingPermsForOverrideCheck(textChannel, context)) return
        if ((textChannel.parent?.channels?.size ?: 0) == 50) { // category size check
            sendRsp(context, "I cant create a new channel here, the limit under each category is 50 channels")
            return
        }

        val msg = context.getTranslation("$root.clearquestion")
            .withVariable(PLACEHOLDER_CHANNEL, textChannel.asTag)
        sendRsp(context, msg)

        context.container.eventWaiter.waitFor(GuildMessageReceivedEvent::class.java, { event ->
            event.channel.idLong == context.channelId && event.author.idLong == context.authorId
        }, { event ->
            val content = event.message.contentRaw
            if (content.equals("confirm", true)) {
                context.initCooldown()
                // permission check for bot
                if (notEnoughPermissionsAndMessage(context, textChannel, Permission.MANAGE_CHANNEL)) return@waitFor
                if (missingPermsForOverrideCheck(textChannel, context)) return@waitFor
                // Explicit check for the parent category if present
                if (textChannel.parent?.let {
                        notEnoughPermissionsAndMessage(
                            context,
                            it,
                            Permission.MANAGE_CHANNEL
                        )
                    } == true) return@waitFor

                if ((textChannel.parent?.channels?.size ?: 0) == 50) { // category size check
                    sendRsp(context, "I cant create a new channel here, the limit under each category is 50 channels")
                    return@waitFor
                }

                val copy = textChannel.createCopy().reason("(clearChannel) ${context.author.asTag}").await()
                copy.manager.setPosition(textChannel.position)

                textChannel.delete().reason("(clearChannel) ${context.author.asTag}").queue()
                val oldId = textChannel.idLong
                val newId = copy.idLong

                migrateSettings(context, oldId, newId)

            } else {
                val nonConfirm = context.getTranslation("$root.notconfirm")
                    .withVariable(PLACEHOLDER_CHANNEL, textChannel.asTag)
                sendRsp(context, nonConfirm)
            }
        })
    }

    private suspend fun missingPermsForOverrideCheck(
        textChannel: TextChannel,
        context: ICommandContext
    ): Boolean {
        val missingPerms = mutableSetOf<Permission>()
        textChannel.permissionOverrides.forEach {
            (it.allowed + it.denied).all { perm ->
                if (context.selfMember.hasPermission(perm)) true
                else {
                    missingPerms.add(perm)
                    false
                }
            }
        }
        if (missingPerms.isNotEmpty()) {
            sendRsp(
                context,
                "I cannot copy the channel because I am missing: `${missingPerms.joinToString(limit = 5) { it.getName() }}` permissions.\n" +
                    "Alternatively you could temporarily grant `Administrator` permissions\n" +
                    "If these permissions seem unnecessary for clearChannel: they are required because I am required to have all permissions that had overrides, to restore the overrides on the new channel."
            )
            return true
        }
        return false
    }

    private fun migrateSettings(context: ICommandContext, oldId: Long, newId: Long) {
        val daoManager = context.daoManager
        daoManager.channelWrapper.migrateChannel(oldId, newId)
        daoManager.channelWrapper.invalidate(context.guildId)

        daoManager.logChannelWrapper.migrateChannel(oldId, newId)
        daoManager.logChannelWrapper.invalidate(context.guildId)

        // VVV channel bound cache, wont do harm not invalidating VVV
        daoManager.commandChannelCoolDownWrapper.migrateChannel(oldId, newId)
        daoManager.commandChannelCoolDownWrapper.invalidate(oldId)

        daoManager.channelCommandStateWrapper.migrateChannel(oldId, newId)
        daoManager.channelCommandStateWrapper.invalidate(oldId)

        daoManager.channelRolePermissionWrapper.migrateChannel(oldId, newId)
        daoManager.channelUserPermissionWrapper.migrateChannel(oldId, newId)
    }
}