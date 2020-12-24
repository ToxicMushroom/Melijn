package me.melijn.melijnbot.commands.games

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.command.RunCondition
import me.melijn.melijnbot.internals.embed.Embedder
import me.melijn.melijnbot.internals.utils.*
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.message.sendSyntax
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

class RockPaperScissorsCommand : AbstractCommand("command.rockpaperscissors") {

    init {
        name = "rockPaperScissors"
        aliases = arrayOf("rps")
        runConditions = arrayOf(RunCondition.GUILD)
        commandCategory = CommandCategory.GAME
    }

    companion object {
        val activeGames: MutableList<RockPaperScissorsGame> = mutableListOf()
    }

    override suspend fun execute(context: CommandContext) {
        if (context.args.isEmpty()) {
            sendSyntax(context)
            return
        }

        val member = retrieveMemberByArgsNMessage(context, 0) ?: return
        val user = member.user
        val bet = if (context.args.size == 1) {
            0
        } else {
            getLongFromArgNMessage(context, 1, 0) ?: return
        }
        if (user.isBot || user.idLong == context.authorId) return
        if (activeGame(context, context.author, user)) return

        val balanceWrapper = context.daoManager.balanceWrapper
        val bal11 = balanceWrapper.getBalance(context.authorId)
        val bal22 = balanceWrapper.getBalance(user.idLong)

        if (bet > bal11) {
            val msg = context.getTranslation("$root.user1.notenoughbalance")
                .withVariable("bal", bal11)
            sendRsp(context, msg)
            return
        } else if (bet > bal22) {
            val msg = context.getTranslation(
                "$root.user2.notenoughbalance"
            ).withSafeVariable("user2", user.asTag)
                .withVariable("bal", bal22)
            sendRsp(context, msg)
            return
        }

        val msg2 = context.getTranslation(
            "$root.challenged"
        ).withVariable("user2", user.asMention)
        sendRsp(context, msg2)

        val channel1 = context.author.openPrivateChannel().awaitOrNull()
        val channel2 = user.openPrivateChannel().awaitOrNull()

        context.container.eventWaiter.waitFor(MessageReceivedEvent::class.java, { event ->
            user.idLong == event.author.idLong
        }, { event ->
            val content = event.message.contentRaw
            if (content.equals("yes", true) ||
                content.equals("y", true) ||
                content.equals("accept", true)
            ) {
                val bal1 = balanceWrapper.getBalance(context.authorId)
                val bal2 = balanceWrapper.getBalance(user.idLong)

                if (bet > bal1) {
                    val msg = context.getTranslation("$root.user1.notenoughbalance")
                        .withVariable("bal", bal1)
                    sendRsp(context, msg)
                    return@waitFor
                } else if (bet > bal2) {
                    val msg = context.getTranslation(
                        "$root.user2.notenoughbalance"
                    ).withSafeVariable("user2", user.asTag)
                        .withVariable("bal", bal2)
                    sendRsp(context, msg)
                    return@waitFor
                }

                balanceWrapper.removeBalance(context.authorId, bet)
                balanceWrapper.removeBalance(user.idLong, bet)

                val title = context.getTranslation("$root.dmmenu.title")
                val description = context.getTranslation("$root.dmmenu.description")
                val optionMenu = Embedder(context)
                    .setTitle(title)
                    .setDescription(description)
                    .build()

                val defaultDisabledDMsMessage = context.getTranslation("message.dmsdisabledfix")
                val msgMenu1 = channel1?.sendMessage(optionMenu)?.awaitOrNull()
                if (msgMenu1 == null) {
                    val msg = defaultDisabledDMsMessage.withVariable("user", context.author.asMention)
                    sendRsp(context, msg)

                    balanceWrapper.addBalance(context.authorId, bet)
                    balanceWrapper.addBalance(user.idLong, bet)
                    return@waitFor
                }

                val msgMenu2 = channel2?.sendMessage(optionMenu)?.awaitOrNull()
                if (msgMenu2 == null) {
                    msgMenu1.delete().queue()

                    val msg = defaultDisabledDMsMessage.withVariable("user", user.asMention)
                    sendRsp(context, msg)

                    balanceWrapper.addBalance(context.authorId, bet)
                    balanceWrapper.addBalance(user.idLong, bet)
                    return@waitFor
                }

                val game = RockPaperScissorsGame(
                    context.authorId,
                    user.idLong,
                    bet,
                    null,
                    null,
                    System.currentTimeMillis()
                )
                if (activeGame(context, context.author, user)) {
                    msgMenu1.delete().queue()
                    msgMenu2.delete().queue()

                    balanceWrapper.addBalance(context.authorId, bet)
                    balanceWrapper.addBalance(user.idLong, bet)
                    return@waitFor
                }
                activeGames.add(game)

                msgMenu1.addReaction(RockPaperScissorsGame.RPS.ROCK.unicode).queue() // rock 🪨
                msgMenu1.addReaction(RockPaperScissorsGame.RPS.PAPER.unicode).queue() // paper 📰
                msgMenu1.addReaction(RockPaperScissorsGame.RPS.SCISSORS.unicode).queue() // scissors ✂

                msgMenu2.addReaction(RockPaperScissorsGame.RPS.ROCK.unicode).queue() // rock 🪨
                msgMenu2.addReaction(RockPaperScissorsGame.RPS.PAPER.unicode).queue() // paper 📰
                msgMenu2.addReaction(RockPaperScissorsGame.RPS.SCISSORS.unicode).queue() // scissors ✂

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
                    .withVariable("user2", user.asTag)
                sendRsp(context, msg)
            }
        }, {
            val msg = context.getTranslation(
                "$root.request.expired"
            ).withVariable("user1", context.author.asMention)
                .withVariable("user2", user.asTag)
            sendRsp(context, msg)
        }, 60)
    }

    private suspend fun activeGame(context: CommandContext, user1: User, user2: User): Boolean {
        return when {
            activeGames.any {
                it.user1 == user1.idLong || it.user2 == user1.idLong
            } -> {
                val msg = context.getTranslation(
                    "$root.user1.ingame"
                )
                sendRsp(context, msg)
                true
            }
            activeGames.any {
                it.user1 == user2.idLong || it.user2 == user2.idLong
            } -> {
                val msg = context.getTranslation(
                    "$root.user2.ingame"
                ).withVariable("user", user2.asTag)
                sendRsp(context, msg)
                true
            }
            else -> false
        }
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