package me.melijn.melijnbot.commands.administration

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.database.DaoManager
import me.melijn.melijnbot.database.giveaway.Giveaway
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.embed.Embedder
import me.melijn.melijnbot.objects.translation.getLanguage
import me.melijn.melijnbot.objects.translation.i18n
import me.melijn.melijnbot.objects.utils.*
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Guild

class GiveawayCommand : AbstractCommand("command.giveaway") {

    init {
        id = 172
        name = "giveaway"
        children = arrayOf(
            AddArg(root),
            StartArg(root),
            EndArg(root),
            ReRollArg(root),
            ListArg(root)
        )
        commandCategory = CommandCategory.ADMINISTRATION
    }

    class ListArg(parent: String) : AbstractCommand("$parent.list") {

        init {
            name = "list"
            aliases = arrayOf("ls")
        }

        override suspend fun execute(context: CommandContext) {
            val giveaways = context.daoManager.giveawayWrapper.giveawayCache.get(context.guildId).await()
                .sortedBy { it.messageId }

            if (giveaways.isEmpty()) {
                val msg = context.getTranslation("$root.empty")
                sendMsg(context, msg)
                return
            }

            val title = context.getTranslation("$root.title")
            var content = "```INI\n"

            content += "[index] - [channel] - [messageId] - [winners] - [prize]"
            for ((index, giveaway) in giveaways.withIndex()) {
                val channel = context.guild.getTextChannelById(giveaway.channelId)
                content += "\n${index + 1} - [${channel?.asTag ?: "/"}] - ${giveaway.messageId} - [${giveaway.winners}] - ${giveaway.prize}"
            }

            val msg = "$title$content```"
            sendMsg(context, msg)
        }
    }

    class ReRollArg(parent: String) : AbstractCommand("$parent.reroll") {

        init {
            name = "reroll"
        }

        override suspend fun execute(context: CommandContext) {
            TODO("Not yet implemented")
        }
    }

    class EndArg(parent: String) : AbstractCommand("$parent.end") {

        init {
            name = "end"
        }

        override suspend fun execute(context: CommandContext) {
            TODO("Not yet implemented")
        }

    }

    class AddArg(parent: String) : AbstractCommand("$parent.add") {

        init {
            name = "add"
            aliases = arrayOf("setup", "create")
        }

        override suspend fun execute(context: CommandContext) {

        }
    }

    class StartArg(parent: String) : AbstractCommand("$parent.start") {

        init {
            name = "start"
        }

        override suspend fun execute(context: CommandContext) {
            val durationArgs = context.args[0].split("\\s+".toRegex())
            val giveawayDuration = (getDurationByArgsNMessage(context, 0, durationArgs.size, durationArgs)
                ?: return) * 1000
            val winners = getIntegerFromArgNMessage(context, 1, 1, 25) ?: return

            val prize = context.getRawArgPart(2)
            if (prize.isEmpty()) {
                sendSyntax(context)
                return
            }

            val newGiveAway = Giveaway(context.channelId, context.messageId, winners, prize, System.currentTimeMillis() + giveawayDuration)

            context.daoManager.giveawayWrapper.setGiveaway(context.guildId, newGiveAway)

            val eb = getGiveawayMessage(context.guild, context.daoManager, context.embedColor)

            sendEmbed(context, eb.build())
        }
    }

    override suspend fun execute(context: CommandContext) {
        sendSyntax(context)
    }

    companion object {
        suspend fun getGiveawayMessage(guild: Guild, daoManager: DaoManager, color: Int): EmbedBuilder {

            val language = getLanguage(daoManager, -1, guild.idLong)
            i18n.getTranslationN(language, "message.giveaway.")
            val eb = Embedder(daoManager, guild.idLong, -1, color)


            return eb
        }
    }
}