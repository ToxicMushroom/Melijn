package me.melijn.melijnbot.commands.games

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.command.RunCondition
import me.melijn.melijnbot.internals.models.EmbedEditor
import me.melijn.melijnbot.internals.utils.getLongFromArgNMessage
import me.melijn.melijnbot.internals.utils.message.sendOnShard0
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.message.sendSyntax
import me.melijn.melijnbot.internals.utils.retrieveMemberByArgsNMessage
import me.melijn.melijnbot.internals.utils.withSafeVariable
import me.melijn.melijnbot.internals.utils.withVariable
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.internal.entities.UserImpl

class RockPaperScissorsCommand : AbstractCommand("command.rockpaperscissors") {

    init {
        name = "rockPaperScissors"
        aliases = arrayOf("rps")
        runConditions = arrayOf(RunCondition.GUILD)
        commandCategory = CommandCategory.GAME
    }

    override suspend fun execute(context: ICommandContext) {
        if (context.args.isEmpty()) {
            sendSyntax(context)
            return
        }

        val member = retrieveMemberByArgsNMessage(context, 0) ?: return
        val bet = if (context.args.size == 1) 0
        else getLongFromArgNMessage(context, 1, 0) ?: return

        val target = member.user
        if (target.isBot || target.idLong == context.authorId) return
        if (activeGame(context, context.author, target)) return

        val daoManager = context.daoManager
        val balanceWrapper = daoManager.balanceWrapper
        val bal11 = balanceWrapper.getBalance(context.authorId)
        val bal22 = balanceWrapper.getBalance(target.idLong)

        if (bet > bal11) {
            val msg = context.getTranslation("$root.user1.notenoughbalance")
                .withVariable("bal", bal11)
            sendRsp(context, msg)
            return
        } else if (bet > bal22) {
            val msg = context.getTranslation(
                "$root.user2.notenoughbalance"
            ).withSafeVariable("user2", target.asTag)
                .withVariable("bal", bal22)
            sendRsp(context, msg)
            return
        }

        val msg2 = context.getTranslation(
            "$root.challenged"
        ).withVariable("user2", target.asMention)
        sendRsp(context, msg2)

        val user1 = context.author as UserImpl
        val user2 = target as UserImpl

        context.container.eventWaiter.waitFor(GuildMessageReceivedEvent::class.java, { event ->
            target.idLong == event.author.idLong &&
                event.channel.idLong == context.channelId
        }, { event ->
            val content = event.message.contentRaw
            if (content.equals("yes", true) ||
                content.equals("y", true) ||
                content.equals("accept", true)
            ) {
                val bal1 = balanceWrapper.getBalance(context.authorId)
                val bal2 = balanceWrapper.getBalance(target.idLong)

                if (bet > bal1) {
                    val msg = context.getTranslation("$root.user1.notenoughbalance")
                        .withVariable("bal", bal1)
                    sendRsp(context, msg)
                    return@waitFor
                } else if (bet > bal2) {
                    val msg = context.getTranslation(
                        "$root.user2.notenoughbalance"
                    ).withSafeVariable("user2", target.asTag)
                        .withVariable("bal", bal2)
                    sendRsp(context, msg)
                    return@waitFor
                }

                balanceWrapper.removeBalance(context.authorId, bet)
                balanceWrapper.removeBalance(target.idLong, bet)

                val title = context.getTranslation("$root.dmmenu.title")
                val description = context.getTranslation("$root.dmmenu.description")
                val optionMenu = EmbedEditor()
                    .setTitle(title)
                    .setDescription(description)


                val defaultDisabledDMsMessage = context.getTranslation("message.dmsdisabledfix")

                val success1 = sendOnShard0(context, user1, optionMenu, "RPS")
                if (!success1) {
                    val msg = defaultDisabledDMsMessage.withVariable("user", context.author.asMention)
                    sendRsp(context, msg)

                    balanceWrapper.addBalance(context.authorId, bet)
                    balanceWrapper.addBalance(target.idLong, bet)
                    return@waitFor
                }

                val success2 =  sendOnShard0(context, user2, optionMenu, "RPS")
                if (!success2) {
                    val msg = defaultDisabledDMsMessage.withVariable("user", target.asMention)
                    sendRsp(context, msg)

                    balanceWrapper.addBalance(context.authorId, bet)
                    balanceWrapper.addBalance(target.idLong, bet)
                    return@waitFor
                }

                val game = RockPaperScissorsGame(
                    context.authorId,
                    target.idLong,
                    bet,
                    null,
                    null,
                    System.currentTimeMillis()
                )
                if (activeGame(context, context.author, target)) {
                    balanceWrapper.addBalance(context.authorId, bet)
                    balanceWrapper.addBalance(target.idLong, bet)
                    return@waitFor
                }
                daoManager.rpsWrapper.addGame(game)

                val msg = if (bet > 0) {
                    context.getTranslation(
                        "$root.started.bet"
                    )
                } else {
                    context.getTranslation(
                        "$root.started"
                    )
                }.withVariable("bet", bet)

                sendRsp(context, msg)
                return@waitFor

            } else {
                val msg = context.getTranslation(
                    "$root.request.denied"
                ).withVariable("user1", context.author.asMention)
                    .withVariable("user2", target.asTag)
                sendRsp(context, msg)
            }
        }, {
            val msg = context.getTranslation(
                "$root.request.expired"
            ).withVariable("user1", context.author.asMention)
                .withVariable("user2", target.asTag)
            sendRsp(context, msg)
        }, 60)
    }

    private suspend fun activeGame(context: ICommandContext, user1: User, user2: User): Boolean {
        val rpsWrapper = context.daoManager.rpsWrapper
        when {
            rpsWrapper.getGame(user1.idLong) != null -> {
                val msg = context.getTranslation("$root.user1.ingame")
                sendRsp(context, msg)
            }
            rpsWrapper.getGame(user2.idLong) != null -> {
                val msg = context.getTranslation("$root.user2.ingame")
                sendRsp(context, msg)
            }
            else -> return false
        }
        return true
    }
}

data class RockPaperScissorsGame(
    val user1: Long,
    val user2: Long,
    val bet: Long,
    var choice1: RPS?,
    var choice2: RPS?,
    var startTime: Long,
) {
    enum class RPS(val unicode: String) {
        ROCK("\uD83E\uDEA8"),
        PAPER("\uD83D\uDCF0"),
        SCISSORS("✂");

        fun beats(): RPS {
            return when (this) {
                ROCK -> SCISSORS
                PAPER -> ROCK
                SCISSORS -> PAPER
            }
        }

        companion object {
            fun fromEmote(unicode: String): RPS? {
                return values().firstOrNull { it.unicode == unicode }
            }
        }
    }
}