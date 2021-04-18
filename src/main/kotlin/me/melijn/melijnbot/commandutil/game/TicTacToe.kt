package me.melijn.melijnbot.commandutil.game

import me.melijn.melijnbot.commands.games.TicTacToeCommand
import me.melijn.melijnbot.commands.games.TicTacToeGame
import me.melijn.melijnbot.database.DaoManager
import me.melijn.melijnbot.internals.embed.Embedder
import me.melijn.melijnbot.internals.translation.getLanguage
import me.melijn.melijnbot.internals.translation.i18n
import me.melijn.melijnbot.internals.utils.awaitOrNull
import me.melijn.melijnbot.internals.utils.withVariable
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.sharding.ShardManager

object TicTacToe {

    suspend fun sendNewMenu(jda: ShardManager, daoManager: DaoManager, game: TicTacToeGame) {
        val userId1 = game.user1
        val userId2 = game.user2

        val user1 = jda.retrieveUserById(userId1).awaitOrNull()
        val user2 = jda.retrieveUserById(userId2).awaitOrNull()

        val pc1 = user1?.openPrivateChannel()?.awaitOrNull()
        val pc2 = user2?.openPrivateChannel()?.awaitOrNull()

        if (pc1 == null || pc2 == null) {
            if (pc1 == null && pc2 == null) {
                daoManager.balanceWrapper.addBalance(userId1, game.bet)
                daoManager.balanceWrapper.addBalance(userId2, game.bet)
            } else if (pc1 == null) {
                daoManager.balanceWrapper.addBalance(userId2, game.bet * 2)
                pc2?.sendMessage("Your opponent closed their dms, thus you are the winner.")?.queue()
            } else {
                daoManager.balanceWrapper.addBalance(userId1, game.bet * 2)
                pc1.sendMessage("Your opponent closed their dms, thus you are the winner.").queue()
            }
            return
        }

        val gameField = game.gameState
        val won1 = checkWon(gameField, TicTacToeGame.TTTState.O)
        val won2 = checkWon(gameField, TicTacToeGame.TTTState.X)
        val draw = gameField.none { it == TicTacToeGame.TTTState.EMPTY } && !won1 && !won2
        val lang1 = getLanguage(daoManager, userId1)
        val lang2 = getLanguage(daoManager, userId2)

        val msg1: MessageEmbed
        val msg2: MessageEmbed

        val wonMsg = Embedder(daoManager, -1, userId1)
            .setTitle("Won Tic-Tac-Toe!")
            .setDescription(
                "%gameField%\nYou won **%bet%** mel.".withVariable(
                    "bet",
                    game.bet
                ).withVariable("gameField", TicTacToeCommand.renderGame(gameField))
            )
            .build()
        val lostMsg = Embedder(daoManager, -1, userId1)
            .setTitle("Lost Tic-Tac-Toe!")
            .setDescription(
                "%gameField%\nYou lost **%bet%** mel.".withVariable(
                    "bet",
                    game.bet
                ).withVariable("gameField", TicTacToeCommand.renderGame(gameField))
            )
            .build()
        val drawMsg = Embedder(daoManager, -1, userId1)
            .setTitle("It's a draw!")
            .setDescription(
                "%gameField%\nYou neither won or lost."
                    .withVariable("gameField", TicTacToeCommand.renderGame(gameField))
            )
            .build()

        when {
            won1 -> {
                msg1 = wonMsg
                msg2 = lostMsg
                daoManager.balanceWrapper.addBalance(userId1, game.bet * 2)
                daoManager.tttWrapper.removeGame(game)
            }
            won2 -> {
                msg2 = wonMsg
                msg1 = lostMsg
                daoManager.balanceWrapper.addBalance(userId2, game.bet * 2)
                daoManager.tttWrapper.removeGame(game)
            }
            draw -> {
                msg1 = drawMsg
                msg2 = drawMsg
                daoManager.balanceWrapper.addBalance(userId1, game.bet)
                daoManager.balanceWrapper.addBalance(userId2, game.bet)
                daoManager.tttWrapper.removeGame(game)
            }
            else -> {
                val title = "Tic-Tac-Toe"
                val description = i18n.getTranslation(
                    lang1,
                    "command.tictactoe.dmmenu.description" // TODO: add to en_translations
                ).withVariable("gameField", TicTacToeCommand.renderGame(gameField))
                val baseMessage = Embedder(daoManager, -1, userId1)
                    .setTitle(title)
                    .setDescription(description)
                val yourAre1 = i18n.getTranslation(lang1, "message.ttt.yourare")
                val pleaseWait1 = i18n.getTranslation(lang1, "message.ttt.pleasewait")
                val yourTurn1 = i18n.getTranslation(lang1, "message.ttt.yourturn")
                val yourAre2 = i18n.getTranslation(lang2, "message.ttt.yourare")
                val pleaseWait2 = i18n.getTranslation(lang2, "message.ttt.pleasewait")
                val yourTurn2 = i18n.getTranslation(lang2, "message.ttt.yourturn")
                if (isTurnUserOne(gameField)) {
                    baseMessage.setFooter(
                        yourAre1.withVariable(
                            "shape",
                            TicTacToeGame.TTTState.O.representation
                        ) + " $pleaseWait1"
                    )
                    msg1 = baseMessage.build()
                    baseMessage.setFooter(
                        yourAre2.withVariable(
                            "shape",
                            TicTacToeGame.TTTState.X.representation
                        ) + " $yourTurn2"
                    )
                    msg2 = baseMessage.build()
                } else {
                    baseMessage.setFooter(
                        yourAre1.withVariable(
                            "shape",
                            TicTacToeGame.TTTState.O.representation
                        ) + " $yourTurn1"
                    )
                    msg1 = baseMessage.build()
                    baseMessage.setFooter(
                        yourAre2.withVariable(
                            "shape",
                            TicTacToeGame.TTTState.X.representation
                        ) + " $pleaseWait2"
                    )
                    msg2 = baseMessage.build()
                }
            }
        }

        pc1.sendMessage(msg1).queue()
        pc2.sendMessage(msg2).queue()
    }

    fun isTurnUserOne(game: Array<TicTacToeGame.TTTState>): Boolean {
        return game.count { state ->
            state == TicTacToeGame.TTTState.EMPTY
        } % 2 == 0
    }

    private fun checkWon(game: Array<TicTacToeGame.TTTState>, state: TicTacToeGame.TTTState): Boolean {
        val horizontal = checkHorizontal(game, state)
        val vertical = checkVertical(game, state)
        val diagonal = checkDiagonal(game, state)
        return horizontal || diagonal || vertical
    }

    private fun checkHorizontal(game: Array<TicTacToeGame.TTTState>, state: TicTacToeGame.TTTState): Boolean {
        for (i in 0 until 3) {
            if (game[i * 3] == state && game[i * 3 + 1] == state && game[i * 3 + 2] == state) {
                return true
            }
        }
        return false
    }

    private fun checkVertical(game: Array<TicTacToeGame.TTTState>, state: TicTacToeGame.TTTState): Boolean {
        for (i in 0 until 3) {
            if (game[i] == state && game[i + 3] == state && game[i + 6] == state) {
                return true
            }
        }
        return false
    }

    private fun checkDiagonal(game: Array<TicTacToeGame.TTTState>, state: TicTacToeGame.TTTState): Boolean {
        if (game[0] == state && game[4] == state && game[8] == state) {
            return true
        }

        if (game[2] == state && game[4] == state && game[6] == state) {
            return true
        }

        return false
    }

}