package me.melijn.melijnbot.commands.developer

import me.melijn.melijnbot.database.ban.BotBanInfo
import me.melijn.melijnbot.database.ban.BotBannedWrapper
import me.melijn.melijnbot.database.ban.BotBannedWrapper.Companion.isBotBanned
import me.melijn.melijnbot.database.locking.EntityType
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.utils.*
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.message.sendSyntax

class BotBanCommand : AbstractCommand("command.botban") {

    init {
        name = "botBan"
        children = arrayOf(
            ListArg(root),
            AddUserArg(root),
            AddServerArg(root),
            RemoveArg(root),
            InfoArg(root)
        )
        commandCategory = CommandCategory.DEVELOPER
    }

    class RemoveArg(parent: String) : AbstractCommand("$parent.remove") {

        init {
            name = "remove"
            aliases = arrayOf("rm")
        }

        override suspend fun execute(context: ICommandContext) {
            val user = retrieveUserByArgsN(context, 0)
            val id = user?.idLong ?: getLongFromArgNMessage(context, 0) ?: return

            val banInfo = context.daoManager.botBannedWrapper.get(id)
            if (banInfo == null) {
                sendRsp(context, "No ban found")
            } else {
                context.daoManager.botBannedWrapper.remove(id)
                sendRsp(context, "Ban removed, ${banInfo.entityType.toLCC()} was previously banned for ```${banInfo.reason}```")
            }
        }
    }


    class AddServerArg(parent: String) : AbstractCommand("$parent.addserver") {

        init {
            name = "addServer"
            aliases = arrayOf("addGuild")
        }

        override suspend fun execute(context: ICommandContext) {
            val guildId = getLongFromArgNMessage(context, 0) ?: return
            val reason = context.getRawArgPart(1).ifEmpty { "/" }
            context.daoManager.botBannedWrapper.add(EntityType.GUILD, guildId, reason)
            sendRsp(context, "Banned $guildId from using the bot, reason:\n```$reason```")
        }
    }

    class AddUserArg(parent: String) : AbstractCommand("$parent.adduser") {

        init {
            name = "addUser"
        }

        override suspend fun execute(context: ICommandContext) {
            val user = retrieveUserByArgsNMessage(context, 0) ?: return
            val reason = context.getRawArgPart(1).ifEmpty { "/" }
            context.daoManager.botBannedWrapper.add(EntityType.USER, user.idLong, reason)
            sendRsp(context, "Banned ${user.asTag} from using the bot, reason:\n```$reason```")
        }
    }


    class InfoArg(parent: String) : AbstractCommand("$parent.info") {

        init {
            name = "info"
        }

        override suspend fun execute(context: ICommandContext) {
            val user = retrieveUserByArgsN(context, 0)
            val id = user?.idLong ?: getLongFromArgNMessage(context, 0) ?: return

            val banInfo = context.daoManager.botBannedWrapper.get(id)
            when {
                isBotBanned(EntityType.GUILD, id) -> showBanState(context, banInfo, EntityType.GUILD)
                isBotBanned(EntityType.USER, id) -> showBanState(context, banInfo, EntityType.USER)
                banInfo != null -> {
                    val moment = banInfo.moment.asEpochMillisToDateTime(context.getTimeZoneId())
                    sendRsp(
                        context, "This ${banInfo.entityType.toLCC()} is banned but not cached.\n" +
                            "**Moment:** $moment\n" +
                            "**Reason:** ${banInfo.reason}"
                    )
                }
                else -> sendRsp(context, "$id is not a banned user or server")
            }
        }

        private suspend fun showBanState(
            context: ICommandContext,
            banInfo: BotBanInfo?,
            entityType: EntityType
        ) {
            if (banInfo != null) {
                val moment = banInfo.moment.asEpochMillisToDateTime(context.getTimeZoneId())
                sendRsp(
                    context, "This ${entityType.toLCC()} is banned and cached.\n" +
                        "**Moment:** $moment\n" +
                        "**Reason:** ${banInfo.reason}"
                )
            } else {
                sendRsp(context, "This ${entityType.toLCC()} is not banned but in ban cache :?")
            }
        }

    }

    class ListArg(parent: String) : AbstractCommand("$parent.list") {

        init {
            name = "list"
        }

        override suspend fun execute(context: ICommandContext) {
            val servers = BotBannedWrapper.bannedGuilds.joinToString(" ")
            val users = BotBannedWrapper.bannedUsers.joinToString(" ")
            sendRsp(context, "**Banned Servers**\n$servers\n**Banned Users**\n$users")
        }

    }

    override suspend fun execute(context: ICommandContext) {
        sendSyntax(context)
    }
}