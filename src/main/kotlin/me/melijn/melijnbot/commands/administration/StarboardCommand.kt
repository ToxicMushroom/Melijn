package me.melijn.melijnbot.commands.administration

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.utils.getIntegerFromArgNMessage
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.message.sendSyntax
import me.melijn.melijnbot.internals.utils.withVariable

class StarboardCommand : AbstractCommand("command.starboard") {

    init {
        id = 224
        name = "starboard"
        children = arrayOf(
            SetMinStarsArg(root),
            ExcludeChannelArg(root),
            IncludeChannelArg(root),
            ExcludedChannelsArg(root),
            DeleteMessage(root)
        )
        commandCategory = CommandCategory.ADMINISTRATION
    }

    class ExcludeChannelArg(parent: String) : AbstractCommand("$parent.excludechannel") {

        init {
            name = "excludeChannel"
            aliases = arrayOf("ec")
        }

        override suspend fun execute(context: CommandContext) {

        }
    }

    class IncludeChannelArg(parent: String) : AbstractCommand("$parent.includechannel") {

        init {
            name = "includeChannel"
            aliases = arrayOf("ic")
        }

        override suspend fun execute(context: CommandContext) {

        }
    }

    class ExcludedChannelsArg(parent: String) : AbstractCommand("$parent.excludedchannels") {

        init {
            name = "excludeChannel"
            aliases = arrayOf("ecs")
        }

        override suspend fun execute(context: CommandContext) {

        }
    }

    class DeleteMessage(parent: String) : AbstractCommand("$parent.deletedmessage") {

        init {
            name = "excludeChannel"
            aliases = arrayOf("rm", "del")
        }

        override suspend fun execute(context: CommandContext) {

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
                val minStars = getIntegerFromArgNMessage(context, 0) ?: return
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