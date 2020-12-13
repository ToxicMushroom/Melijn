package me.melijn.melijnbot.internals.utils

import me.melijn.melijnbot.commands.games.RockPaperScissorsGame
import me.melijn.melijnbot.database.DaoManager
import me.melijn.melijnbot.internals.translation.getLanguage
import me.melijn.melijnbot.internals.translation.i18n
import net.dv8tion.jda.api.JDA


object RockPaperScissors {

    suspend fun checkForContinue(jda: JDA, daoManager: DaoManager, iter: MutableListIterator<RockPaperScissorsGame>) {
        val rps = iter.next()
        val choice1 = rps.choice1
        val choice2 = rps.choice2
        if (choice1 == null || choice2 == null) return
        iter.remove()

        val userId1 = rps.user1
        val user1 = jda.retrieveUserById(userId1).awaitOrNull()
        val userId2 = rps.user2
        val user2 = jda.retrieveUserById(userId2).awaitOrNull()
        val dm1 = user1?.openPrivateChannel()?.awaitOrNull()
        val dm2 = user2?.openPrivateChannel()?.awaitOrNull()
        val msg1: String
        val msg2: String

        val lang1 = getLanguage(daoManager, userId1)
        val lang2 = getLanguage(daoManager, userId2)

        when {
            choice1 == choice2 -> { // tie
                msg1 = i18n.getTranslation(
                    lang1,
                    "message.rps.user.tie"
                )
                    .withVariable("user", user2?.asTag ?: return)
                    .withVariable("bet", rps.bet)
                msg2 = i18n.getTranslation(
                    lang2,
                    "message.rps.user.tie"
                )
                    .withVariable("user", user1?.asTag ?: return)
                    .withVariable("bet", rps.bet)

                daoManager.balanceWrapper.addBalance(userId1, rps.bet)
                daoManager.balanceWrapper.addBalance(userId2, rps.bet)
            }
            choice1.beats() == choice2 -> { // user1 won
                msg1 = i18n.getTranslation(
                    lang1,
                    "message.rps.user.won"
                )
                    .withVariable("user", user2?.asTag ?: return)
                    .withVariable("bet", rps.bet)
                msg2 = i18n.getTranslation(
                    lang2,
                    "message.rps.user.lost"
                )
                    .withVariable("user", user1?.asTag ?: return)
                    .withVariable("bet", rps.bet)

                daoManager.balanceWrapper.addBalance(userId1, rps.bet * 2)
            }
            else -> { // user2 won
                msg1 = i18n.getTranslation(
                    lang1,
                    "message.rps.user.lost"
                )
                    .withVariable("user", user2?.asTag ?: return)
                    .withVariable("bet", rps.bet)
                msg2 = i18n.getTranslation(
                    lang2,
                    "message.rps.user.won"
                )
                    .withVariable("user", user1?.asTag ?: return)
                    .withVariable("bet", rps.bet)

                daoManager.balanceWrapper.addBalance(userId2, rps.bet * 2)
            }
        }

        dm1?.sendMessage(msg1)?.queue()
        dm2?.sendMessage(msg2)?.queue()
    }
}