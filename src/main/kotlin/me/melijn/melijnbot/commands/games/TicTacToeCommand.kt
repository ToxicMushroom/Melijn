package me.melijn.melijnbot.commands.games

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.embed.Embedder
import me.melijn.melijnbot.internals.utils.*
import me.melijn.melijnbot.internals.utils.message.sendMsg
import me.melijn.melijnbot.internals.utils.message.sendSyntax
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

class TicTacToeCommand : AbstractCommand("command.tictactoe") {

    init {
        id = 236
        name = "ticTacToe"
        aliases = arrayOf("ttt")
        commandCategory = CommandCategory.GAME
    }

    companion object {
        val activeGames: MutableList<TicTacToeGame> = mutableListOf()

        private const val fieldTemplate = "```" +
            "    ╭───┬───┬───╮\n" +
            "    │ 1 │ 2 │ 3 │\n" +
            "╭───╆━━━╈━━━╈━━━┪\n" +
            "│ 1 ┃ % ┃ % ┃ % ┃\n" +
            "├───╊━━━╋━━━╋━━━┫\n" +
            "│ 2 ┃ % ┃ % ┃ % ┃\n" +
            "├───╊━━━╋━━━╋━━━┫\n" +
            "│ 3 ┃ % ┃ % ┃ % ┃\n" +
            "╰───┺━━━┻━━━┻━━━┛```"

        fun renderGame(array: Array<TicTacToeGame.TTTState>): String {
            var out = fieldTemplate
            for (el in array) {
                out = out.replaceFirst("%", el.representation, false)
            }
            return out
        }
    }

    override suspend fun execute(context: CommandContext) {
        if (context.args.isEmpty()) {
            sendSyntax(context)
            return
        }

        val user = (retrieveMemberByArgsNMessage(context, 0, false, false) ?: return).user
        val bet = if (context.args.size == 1) {
            0
        } else {
            getLongFromArgNMessage(context, 1, 0) ?: return
        }
        if (user.isBot || user.idLong == context.authorId) return
        if (activeGame(context, context.author, user)) return

        val balanceWrapper = context.daoManager.balanceWrapper
        val bal1 = balanceWrapper.getBalance(context.authorId)
        val bal2 = balanceWrapper.getBalance(user.idLong)

        if (bet > bal1) {
            val msg = context.getTranslation("$root.user1.notenoughbalance")
                .withVariable("bal", bal1)
            sendMsg(context, msg)
            return
        } else if (bet > bal2) {
            val msg = context.getTranslation(
                "$root.user2.notenoughbalance"
            ).withVariable("user", user.asTag)
                .withVariable("bal", bal2)
            sendMsg(context, msg)
            return
        }

        val msg2 = context.getTranslation(
            "$root.challenged"
        ).withVariable("user2", user.asMention)
        sendMsg(context, msg2)

        val channel1 = context.author.openPrivateChannel().awaitOrNull()
        val channel2 = user.openPrivateChannel().awaitOrNull()

        context.container.eventWaiter.waitFor(MessageReceivedEvent::class.java, { event ->
            user.idLong == event.author.idLong &&
                event.channel.idLong == context.channelId
        }, { event ->
            val content = event.message.contentRaw
            if (content.equals("yes", true) ||
                content.equals("y", true) ||
                content.equals("accept", true)
            ) {
                val bal11 = balanceWrapper.getBalance(context.authorId)
                val bal22 = balanceWrapper.getBalance(user.idLong)

                if (bet > bal11) {
                    val msg = context.getTranslation("$root.user1.notenoughbalance")
                        .withVariable("bal", bal11)
                    sendMsg(context, msg)
                    return@waitFor
                } else if (bet > bal22) {
                    val msg = context.getTranslation(
                        "$root.user2.notenoughbalance"
                    ).withVariable("user", user.asTag)
                        .withVariable("bal", bal22)
                    sendMsg(context, msg)
                    return@waitFor
                }

                val game = TicTacToeGame(
                    context.authorId,
                    user.idLong,
                    bet,
                    Array(9) { TicTacToeGame.TTTState.EMPTY },
                    System.currentTimeMillis()
                )
                val title = "TicTacToe"
                val description = context.getTranslation(
                    "$root.dmmenu.description"
                ).withVariable("gameField", renderGame(game.game))

                val optionMenu = Embedder(context)
                    .setTitle(title)
                    .setDescription(description)


                val defaultDisabledDMsMessage = context.getTranslation("message.dmsdisabledfix")

                optionMenu.setFooter("You are " + TicTacToeGame.TTTState.O.representation + " | Please wait for your opponent.")
                val msgMenu1 = channel1?.sendMessage(optionMenu.build())?.awaitOrNull()
                if (msgMenu1 == null) {
                    val msg = defaultDisabledDMsMessage.withVariable("user", context.author.asMention)
                    sendMsg(context, msg)
                    return@waitFor
                }

                optionMenu.setFooter("You are " + TicTacToeGame.TTTState.X.representation + " | It's your turn.")
                val msgMenu2 = channel2?.sendMessage(optionMenu.build())?.awaitOrNull()
                if (msgMenu2 == null) {
                    msgMenu1.delete().queue()

                    val msg = defaultDisabledDMsMessage.withVariable("user", user.asMention)
                    sendMsg(context, msg)
                    return@waitFor
                }

                if (activeGame(context, context.author, user)) {
                    msgMenu1.delete().queue()
                    msgMenu2.delete().queue()
                    return@waitFor
                }
                activeGames.add(game)

                balanceWrapper.removeBalance(context.authorId, bet)
                balanceWrapper.removeBalance(user.idLong, bet)

                val msg = if (bet > 0) {
                    context.getTranslation(
                        "$root.started.bet"
                    )
                } else {
                    context.getTranslation(
                        "$root.started"
                    )
                }.withVariable("bet", bet)

                sendMsg(context, msg)
                return@waitFor

            } else {
                val msg = context.getTranslation(
                    "$root.request.denied"
                ).withVariable("user1", context.author.asMention)
                    .withVariable("user2", user.asTag)
                sendMsg(context, msg)
            }
        }, {
            val msg = context.getTranslation(
                "$root.request.expired"
            ).withVariable("user1", context.author.asMention)
                .withVariable("user2", user.asTag)
            sendMsg(context, msg)
        }, 60)
    }

    suspend fun activeGame(context: CommandContext, user1: User, user2: User): Boolean {
        return when {
            activeGames.any {
                it.user1 == user1.idLong || it.user2 == user1.idLong
            } -> {
                val msg = context.getTranslation(
                    "$root.user1.ingame",

                    )
                sendMsg(context, msg)
                true
            }
            activeGames.any {
                it.user1 == user2.idLong || it.user2 == user2.idLong
            } -> {
                val msg = context.getTranslation(
                    "$root.user2.ingame",
                ).withVariable("user", user2.asTag)
                sendMsg(context, msg)
                true
            }
            else -> false
        }
    }
}

data class TicTacToeGame(
    val user1: Long,
    val user2: Long,
    val bet: Long,
    var game: Array<TTTState>,
    var startTime: Long
) {
    enum class TTTState(val representation: String) {
        EMPTY(" "),
        X("X"), // user 2 (%2 == 1)
        O("○") // user 1 (%2 == 0)
    }
}