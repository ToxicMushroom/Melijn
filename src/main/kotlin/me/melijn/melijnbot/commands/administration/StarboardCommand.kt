package me.melijn.melijnbot.commands.administration

import me.melijn.melijnbot.enums.ChannelType
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.utils.*
import me.melijn.melijnbot.internals.utils.checks.getAndVerifyChannelByType
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.message.sendSyntax

class StarboardCommand : AbstractCommand("command.starboard") {

    init {
        id = 224
        name = "starboard"
        children = arrayOf(
            SetMinStarsArg(root),
            ExcludeChannelArg(root),
            IncludeChannelArg(root),
            ExcludedChannelsArg(root),
            HideMessage(root)
        )
        commandCategory = CommandCategory.ADMINISTRATION
    }

    class ExcludeChannelArg(parent: String) : AbstractCommand("$parent.excludechannel") {

        init {
            name = "excludeChannel"
            aliases = arrayOf("ec")
        }

        override suspend fun execute(context: CommandContext) {
            if (context.args.isEmpty()) {
                sendSyntax(context)
                return
            }

            val channel = getTextChannelByArgsNMessage(context, 0, true) ?: return
            val starboardSettingsWrapper = context.daoManager.starboardSettingsWrapper
            val starboardSettings = starboardSettingsWrapper.getStarboardSettings(context.guildId)
            val currentExcluded = starboardSettings.excludedChannelIds.splitIETEL(",").toMutableList()
            currentExcluded.addIfNotPresent(channel.id)
            starboardSettings.excludedChannelIds = currentExcluded.joinToString(",")
            starboardSettingsWrapper.setStarboardSettings(context.guildId, starboardSettings)

            val msg = context.getTranslation("$root.excluded")
                .withVariable("channel", channel.asMention)
            sendRsp(context, msg)

        }
    }

    class IncludeChannelArg(parent: String) : AbstractCommand("$parent.includechannel") {

        init {
            name = "includeChannel"
            aliases = arrayOf("ic")
        }

        override suspend fun execute(context: CommandContext) {
            if (context.args.isEmpty()) {
                sendSyntax(context)
                return
            }

            val channel = getTextChannelByArgsNMessage(context, 0, true) ?: return
            val starboardSettingsWrapper = context.daoManager.starboardSettingsWrapper
            val starboardSettings = starboardSettingsWrapper.getStarboardSettings(context.guildId)
            val currentExcluded = starboardSettings.excludedChannelIds.splitIETEL(",").toMutableList()
            currentExcluded.remove(channel.id)
            starboardSettings.excludedChannelIds = currentExcluded.joinToString(",")
            starboardSettingsWrapper.setStarboardSettings(context.guildId, starboardSettings)

            val msg = context.getTranslation("$root.included")
                .withVariable("channel", channel.asMention)
            sendRsp(context, msg)
        }
    }

    class ExcludedChannelsArg(parent: String) : AbstractCommand("$parent.excludedchannels") {

        init {
            name = "excludeChannel"
            aliases = arrayOf("ecs")
        }

        override suspend fun execute(context: CommandContext) {
            if (context.args.isEmpty()) {
                sendSyntax(context)
                return
            }

            val starboardSettingsWrapper = context.daoManager.starboardSettingsWrapper
            val starboardSettings = starboardSettingsWrapper.getStarboardSettings(context.guildId)
            val currentExcluded = starboardSettings.excludedChannelIds.splitIETEL(",").toMutableList()

            val msg = context.getTranslation("$root.excludedlist")
            var content = ""
            for (channel in currentExcluded.withIndex()) {
                content += "\n- <#${channel}>"
            }
            sendRsp(context, msg + content)
        }
    }

    class HideMessage(parent: String) : AbstractCommand("$parent.hidemessage") {

        init {
            name = "hideMessage"
            aliases = arrayOf("hm")
        }

        override suspend fun execute(context: CommandContext) {
            if (context.args.isEmpty()){
                sendSyntax(context)
                return
            }

            val id = getLongFromArgNMessage(context, 0, 0) ?: return
            val starboardMessageWrapper = context.daoManager.starboardMessageWrapper
            val info = starboardMessageWrapper.getStarboardInfo(id)
            if (info == null) {
                val msg = context.getTranslation("$root.unknown")
                sendRsp(context, msg)
                return
            }

            val channel = context.guild.getAndVerifyChannelByType(context.daoManager, ChannelType.STARBOARD)
            if (channel == null) {
                val blub = context.getTranslation("$root.channelgone")
                sendRsp(context, blub)
                return
            }

            val starMessage = channel.retrieveMessageById(info.starboardMessageId).awaitOrNull()
            if (starMessage == null) {
                val blub = context.getTranslation("$root.stargone")
                sendRsp(context, blub)
                return
            }

            starMessage.delete().reason("(starboard hideMessage) ${context.author.asTag}").queue()
            starboardMessageWrapper.updateDeleted(info.starboardMessageId, true)
            val msg = context.getTranslation("$root.hidden")
            sendRsp(context, msg)
        }
    }

    class DeleteMessage(parent: String) : AbstractCommand("$parent.deletemessage") {

        init {
            name = "deleteMessage"
            aliases = arrayOf("dm", "removeMessage", "rm")
        }

        override suspend fun execute(context: CommandContext) {
            if (context.args.isEmpty()){
                sendSyntax(context)
                return
            }

            val id = getLongFromArgNMessage(context, 0, 0) ?: return
            val starboardMessageWrapper = context.daoManager.starboardMessageWrapper
            val info = starboardMessageWrapper.getStarboardInfo(id)
            if (info == null) {
                val msg = context.getTranslation("$root.unknown")
                sendRsp(context, msg)
                return
            }

            val channel = context.guild.getAndVerifyChannelByType(context.daoManager, ChannelType.STARBOARD)
            if (channel == null) {
                val blub = context.getTranslation("$root.channelgone")
                sendRsp(context, blub)
                return
            }

            val starMessage = channel.retrieveMessageById(info.starboardMessageId).awaitOrNull()
            if (starMessage == null) {
                val blub = context.getTranslation("$root.stargone")
                sendRsp(context, blub)
                return
            }

            starMessage.delete().reason("(starboard deleteMessage) ${context.author.asTag}").queue()
            starboardMessageWrapper.delete(info.starboardMessageId)
            val msg = context.getTranslation("$root.hidden")
            sendRsp(context, msg)
        }
    }

    class SetMinStarsArg(parent: String) : AbstractCommand("$parent.setminstars") {

        init {
            name = "setMinStars"
            aliases = arrayOf("sms")
        }

        override suspend fun execute(context: CommandContext) {
            if (context.args.isEmpty()) {
                val starboardSettingsWrapper = context.daoManager.starboardSettingsWrapper
                val starboardSettings = starboardSettingsWrapper.getStarboardSettings(context.guildId)
                val msg = context.getTranslation("$root.get")
                    .withVariable("stars", starboardSettings.minStars)
                sendRsp(context, msg)
            } else {
                val minStars = getIntegerFromArgNMessage(context, 0, 3, 10) ?: return
                val starboardSettingsWrapper = context.daoManager.starboardSettingsWrapper
                val starboardSettings = starboardSettingsWrapper.getStarboardSettings(context.guildId)
                starboardSettings.minStars = minStars
                starboardSettingsWrapper.setStarboardSettings(context.guildId, starboardSettings)

                val msg = context.getTranslation("$root.set")
                    .withVariable("stars", minStars)
                sendRsp(context, msg)
            }
        }
    }

    override suspend fun execute(context: CommandContext) {
        sendSyntax(context)
    }
}