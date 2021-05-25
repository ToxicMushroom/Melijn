package me.melijn.melijnbot.commands.administration

import me.melijn.melijnbot.commands.administration.LockCommand.Companion.channelFilter
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.command.PLACEHOLDER_PREFIX
import me.melijn.melijnbot.internals.embed.Embedder
import me.melijn.melijnbot.internals.translation.PLACEHOLDER_CHANNEL
import me.melijn.melijnbot.internals.utils.*
import me.melijn.melijnbot.internals.utils.message.sendEmbedRsp
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.message.sendSyntax
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.GuildChannel
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.entities.VoiceChannel
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.api.requests.restaction.PermissionOverrideAction

class UnlockCommand : AbstractCommand("command.unlock") {

    init {
        id = 242
        name = "unlock"
        aliases = arrayOf("unlockChannel")

        // need the server permission in order to create overrides for it
        discordPermissions = (LockCommand.textDenyList + LockCommand.voiceDenyList)
        cooldown = 10_000
        commandCategory = CommandCategory.ADMINISTRATION
    }

    override suspend fun execute(context: ICommandContext) {
        if (context.args.isEmpty()) {
            sendSyntax(context)
            return
        }

        val voice = mutableListOf<VoiceChannel>()
        val text = mutableListOf<TextChannel>()
        val unknown = mutableListOf<String>()
        for ((index, arg) in context.args.withIndex()) {
            val textChannel = getTextChannelByArgsN(context, index)
            if (textChannel != null) {
                text.addIfNotPresent(textChannel)
                break
            }

            val voiceChannel = getVoiceChannelByArgsN(context, index)
            if (voiceChannel != null) {
                voice.addIfNotPresent(voiceChannel)
                break
            }

            when (arg.lowercase()) {
                "all" -> {
                    text.addAllIfNotPresent(context.guild.textChannels.filter { channelFilter(context, it) })
                    voice.addAllIfNotPresent(context.guild.voiceChannels.filter { channelFilter(context, it) })
                    break
                }
                "all-text" -> {
                    text.addAllIfNotPresent(context.guild.textChannels.filter { channelFilter(context, it) })
                    break
                }
                "all-vc", "all-voice" -> {
                    voice.addAllIfNotPresent(context.guild.voiceChannels.filter { channelFilter(context, it) })
                    break
                }
            }

            unknown.addIfNotPresent(arg)
        }

        if (text.size == 0 && voice.size == 1 && unknown.size == 0) {
            unlockChannel(context, voice.first())
        } else if (text.size == 1 && voice.size == 0 && unknown.size == 0) {
            unlockChannel(context, text.first())
        } else if (text.size == 0 && voice.size == 0) {
            val msg = context.getTranslation("$root.notfound")
                .withSafeVariable("arg", unknown.joinToString())
            sendRsp(context, msg)
        } else {
            val msg = context.getTranslation("$root.questionmany")
                .withVariable("text", text.size)
                .withVariable("voice", voice.size)
                .withSafeVariable("unknown", unknown.joinToString())
            sendRsp(context, msg)

            context.container.eventWaiter.waitFor(GuildMessageReceivedEvent::class.java, {
                it.author.idLong == context.authorId && it.channel.idLong == context.channelId
            }, {
                if (it.message.contentRaw == "yes") {
                    unlockMany(context, voice + text)
                } else {
                    val rsp = context.getTranslation("$root.manyquestion.denied")
                    sendRsp(context, rsp)
                }
            }, {
                val rsp = context.getTranslation("$root.manyquestion.expired")
                sendRsp(context, rsp)
            }, 120)
        }
    }

    private suspend fun unlockMany(context: ICommandContext, list: List<GuildChannel>) {
        for (channel in list) {
            if (notEnoughPermissionsAndMessage(
                    context,
                    channel,
                    Permission.MANAGE_CHANNEL,
                    Permission.MANAGE_ROLES
                )
            ) return
        }

        val notLocked = mutableListOf<GuildChannel>()
        val unlocked = mutableListOf<GuildChannel>()
        var textOverrides = 0
        var textPermChanges = 0
        var voiceOverrides = 0
        var voicePermChanges = 0
        for (channel in list) {
            val unlockStatus = internalUnlock(context, channel)
            when (unlockStatus.third) {
                LockStatus.SUCCESS, LockStatus.NO_OVERRIDE -> unlocked.add(channel)
                LockStatus.NOT_LOCKED -> notLocked.add(channel)
                LockStatus.ALREADY_LOCKED -> throw IllegalStateException("already locked in unlockcommand")
            }
            if (channel is TextChannel) {
                textOverrides += unlockStatus.first
                textPermChanges += unlockStatus.second
            } else {
                voiceOverrides += unlockStatus.first
                voicePermChanges += unlockStatus.second
            }
        }

        context.daoManager.discordChannelOverridesWrapper.removeAll(context.guildId, unlocked.map { it.idLong })
        val msg = context.getTranslation("$root.unlockedmany")
            .withVariable("notLocked", notLocked.size)
            .withVariable("unlocked", unlocked.size)
            .withVariable("text", unlocked.filterIsInstance(TextChannel::class.java).size)
            .withVariable("textOverrides", textOverrides)
            .withVariable("textPermChanges", textPermChanges)
            .withVariable("voice", unlocked.filterIsInstance(VoiceChannel::class.java).size)
            .withVariable("voiceOverrides", voiceOverrides)
            .withVariable("voicePermChanges", voicePermChanges)


        val eb = Embedder(context)
            .setDescription(msg)
        sendEmbedRsp(context, eb.build())

    }

    private suspend fun unlockChannel(context: ICommandContext, channel: GuildChannel) {
        if (notEnoughPermissionsAndMessage(context, channel, Permission.MANAGE_CHANNEL, Permission.MANAGE_ROLES)) return

        val status = internalUnlock(context, channel)
        val msg = when (status.third) {
            LockStatus.SUCCESS, LockStatus.NO_OVERRIDE -> {
                context.daoManager.discordChannelOverridesWrapper.remove(context.guildId, channel.idLong)
                context.getTranslation("$root.unlocked")
                    .withVariable(PLACEHOLDER_CHANNEL, channel.name)
                    .withVariable("overrides", status.first)
                    .withVariable("permChanges", status.second)
            }
            LockStatus.NOT_LOCKED -> {
                context.getTranslation("$root.notlocked")
                    .withVariable(PLACEHOLDER_CHANNEL, channel.name)
                    .withSafeVariable(PLACEHOLDER_PREFIX, context.usedPrefix)
            }
            LockStatus.ALREADY_LOCKED -> throw IllegalStateException("already locked in unlockcommand")
        }

        sendRsp(context, msg)
    }

    // -> overrideCount, permissionsSwitched, status
    private suspend fun internalUnlock(context: ICommandContext, channel: GuildChannel): Triple<Int, Int, LockStatus> {
        val denyList = when (channel) {
            is TextChannel -> LockCommand.textDenyList
            is VoiceChannel -> LockCommand.voiceDenyList
            else -> throw IllegalStateException("unknown channeltype")
        }

        val overrideMap = context.daoManager.discordChannelOverridesWrapper.getAll(
            context.guildId, channel.idLong
        )

        if (overrideMap.isEmpty()) {
            return Triple(0, 0, LockStatus.NOT_LOCKED)
        }

        // Remove denied perms (unlock)
        var overrideCounter = 0
        var permsChangedCounter = 0
        for ((id, flags) in overrideMap) {
            val role = context.guild.getRoleById(id) ?: continue

            val manager = channel.upsertPermissionOverride(role)
            val permsChangedHere = revertPermsToOriginal(denyList, flags, manager)

            permsChangedCounter += permsChangedHere
            if (permsChangedHere > 0) {
                overrideCounter++
                manager.reason("(unlock) " + context.author.asTag).queue()
            }
        }

        // Remove Melijn grants
        val melFlags = overrideMap[context.selfUserId]
        if (melFlags != null) {
            val melManager = channel.upsertPermissionOverride(context.selfMember)
            val permsChangedHere = revertPermsToOriginal(denyList, melFlags, melManager)

            permsChangedCounter += permsChangedHere
            if (permsChangedHere > 0) {
                overrideCounter++

                // Execute the restaction to completely remove the override or execute the overrideAction
                if (melFlags.first == 0L && melFlags.second == 0L) {
                    channel.getPermissionOverride(context.selfMember)?.delete()
                } else {
                    melManager
                }?.reason("(unlock) " + context.author.asTag)?.queue()
            }
        }

        val status = if (overrideCounter != 0) {
            LockStatus.SUCCESS
        } else {
            LockStatus.NO_OVERRIDE
        }
        return Triple(overrideCounter, permsChangedCounter, status)
    }

    private fun revertPermsToOriginal(
        denyList: Array<Permission>,
        flags: Pair<Long, Long>,
        manager: PermissionOverrideAction
    ): Int {
        var permsChangedHere = 0
        for (perm in denyList) {
            when {
                (flags.first and perm.rawValue) != 0L -> { // if the role's first state had this permission allowed
                    if (((manager.allow shr perm.offset) and 0x1) == 0L) { // if the channel doesnt already have this permission set to allowed
                        manager.grant(perm)
                        permsChangedHere++
                    }
                }
                (flags.second and perm.rawValue) != 0L -> { // if the role's first state had this permission denied
                    if (((manager.deny shr perm.offset) and 0x1) == 0L) { // if the channel doesnt already have this permission set to denied
                        manager.deny(perm)
                        permsChangedHere++
                    }
                }
                else -> { // if the role's first state was to interhit this permission
                    if (!(((manager.deny shr perm.offset) and 0x1) == 0L && ((manager.allow shr perm.offset) and 0x1) == 0L)) { // Check if a clear is needed
                        manager.clear(perm)
                        permsChangedHere++
                    }
                }
            }
        }
        return permsChangedHere
    }

    enum class LockStatus {
        SUCCESS,
        NO_OVERRIDE,
        NOT_LOCKED,
        ALREADY_LOCKED
    }
}