package me.melijn.melijnbot.commands.games

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.command.RunCondition
import me.melijn.melijnbot.internals.models.EmbedEditor
import me.melijn.melijnbot.internals.utils.getLongFromArgNMessage
import me.melijn.melijnbot.internals.utils.message.sendMsg
import me.melijn.melijnbot.internals.utils.message.sendOnShard0
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.message.sendSyntax
import me.melijn.melijnbot.internals.utils.retrieveMemberByArgsNMessage
import me.melijn.melijnbot.internals.utils.withVariable
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.internal.entities.UserImpl

class TicTacToeCommand : AbstractCommand("command.tictactoe") {

    init {
        id = 236
        name = "ticTacToe"
        aliases = arrayOf("ttt")
        runConditions = arrayOf(RunCondition.GUILD)
        commandCategory = CommandCategory.GAME
    }

    companion object {
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

    override suspend fun execute(context: ICommandContext) {
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

        val user1 = context.author as UserImpl
        val user2 = user as UserImpl

        context.container.eventWaiter.waitFor(GuildMessageReceivedEvent::class.java, { event ->
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

                balanceWrapper.removeBalance(context.authorId, bet)
                balanceWrapper.removeBalance(user.idLong, bet)


                val game = TicTacToeGame(
                    context.authorId,
                    user.idLong,
                    bet,
                    Array(9) { TicTacToeGame.TTTState.EMPTY },
                    System.currentTimeMillis(),
                    System.currentTimeMillis()
                )

                val title = "TicTacToe"
                val description = context.getTranslation(
                    "$root.dmmenu.description"
                ).withVariable("gameField", renderGame(game.gameState))

                val optionMenu = EmbedEditor()
                    .setTitle(title)
                    .setDescription(description)


                val defaultDisabledDMsMessage = context.getTranslation("message.dmsdisabledfix")

                optionMenu.setFooter("You are " + TicTacToeGame.TTTState.O.representation + " | Please wait for your opponent.")
                val success = sendOnShard0(context, user1, optionMenu, "TTT")
                if (!success) {
                    val msg = defaultDisabledDMsMessage.withVariable("user", context.author.asMention)
                    sendMsg(context, msg)
                    balanceWrapper.addBalance(context.authorId, bet)
                    balanceWrapper.addBalance(user.idLong, bet)
                    return@waitFor
                }

                optionMenu.setFooter("You are " + TicTacToeGame.TTTState.X.representation + " | It's your turn.")
                val success2 = sendOnShard0(context, user2, optionMenu, "TTT")
                if (!success2) {
                    val msg = defaultDisabledDMsMessage.withVariable("user", user.asMention)
                    sendMsg(context, msg)
                    balanceWrapper.addBalance(context.authorId, bet)
                    balanceWrapper.addBalance(user.idLong, bet)
                    return@waitFor
                }

                if (activeGame(context, context.author, user)) {
                    balanceWrapper.addBalance(context.authorId, bet)
                    balanceWrapper.addBalance(user.idLong, bet)
                    return@waitFor
                }
                context.daoManager.tttWrapper.addGame(game)

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

    private suspend fun activeGame(context: ICommandContext, user1: User, user2: User): Boolean {
        val rpsWrapper = context.daoManager.tttWrapper
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

data class TicTacToeGame(
    val user1: Long,
    val user2: Long,
    val bet: Long,
    var gameState: Array<TTTState>,
    var lastUpdate: Long,
    var startTime: Long
) {
    enum class TTTState(val representation: String) {
        EMPTY(" "),
        X("X"), // user 2 (%2 == 1)
        O("○") // user 1 (%2 == 0)
    }
}