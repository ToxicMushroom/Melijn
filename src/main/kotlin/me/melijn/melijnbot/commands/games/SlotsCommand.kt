package me.melijn.melijnbot.commands.games

import kotlinx.coroutines.delay
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.embed.Embedder
import me.melijn.melijnbot.internals.utils.getBalanceNMessage
import me.melijn.melijnbot.internals.utils.message.sendEmbedAwaitEL
import me.melijn.melijnbot.internals.utils.withVariable
import net.dv8tion.jda.api.Permission
import kotlin.random.Random


class SlotsCommand : AbstractCommand("command.slots") {

    init {
        id = 217
        name = "slots"
        cooldown = 3500
        discordChannelPermissions = arrayOf(Permission.MESSAGE_EMBED_LINKS)
        commandCategory = CommandCategory.GAME
    }

    companion object {
        val prizeList = listOf(
            SlotEntry("tangerine", "\uD83C\uDF4A", 3),
            SlotEntry("lemon", "\uD83C\uDF4B", 3),
            SlotEntry("grapes", "\uD83C\uDF47", 3),
            SlotEntry("watermelon", "\uD83C\uDF49", 3),
            SlotEntry("cherries", "\uD83C\uDF52", 3),
            SlotEntry("money", "\uD83D\uDCB0", 2),
            SlotEntry("gem", "\uD83D\uDC8E", 2),
            SlotEntry("atm", "\uD83C\uDFE7", 1),
            SlotEntry("seven", "7️⃣", 1),
        )
    }

    data class SlotEntry(val name: String, val unicode: String, val occurrence: Int)

    suspend fun execute(context: ICommandContext) {
        val slotEmote = context.shardManager.getEmoteById(context.container.settings.emote.slotId)
        val prizeMap = mutableMapOf<Int, String>()

        for (i in prizeList.indices) {
            for (j in 0 until prizeList[i].occurrence) {
                prizeMap[prizeMap.size] = prizeList[i].unicode
            }
        }

        val balanceWrapper = context.daoManager.balanceWrapper
        val amount = getBalanceNMessage(context, 0) ?: return

        context.initCooldown()
        balanceWrapper.removeBalance(context.authorId, amount)


        val slot1 = prizeMap[Random.nextInt(prizeMap.size)] ?: throw IllegalArgumentException("slot1 error")
        val slot2 = prizeMap[Random.nextInt(prizeMap.size)] ?: throw IllegalArgumentException("slot2 error")
        val slot3 = prizeMap[Random.nextInt(prizeMap.size)] ?: throw IllegalArgumentException("slot3 error")


        val spinningWord = context.getTranslation("word.spinning").toUpperCase()
        val stripe = "**------------------**"
        val spinning = "\n---- **$spinningWord**"
        val slots = "**| %slot1% | %slot2% | %slot3% |**"
        val spinningMsg = "$stripe\n$slots\n$stripe$spinning"

        val eb = Embedder(context)
            .setTitle("Slot | User: ${context.author.name}")
            .setDescription(
                spinningMsg
                    .withVariable("slot1", slotEmote?.asMention ?: "?")
                    .withVariable("slot2", slotEmote?.asMention ?: "?")
                    .withVariable("slot3", slotEmote?.asMention ?: "?")
            )


        val msg = try {
            sendEmbedAwaitEL(context, eb.build()).last()
        } catch (t: Throwable) {
            balanceWrapper.addBalance(context.authorId, amount)
            return
        }


        delay(1_000)
        eb.setDescription(
            spinningMsg
                .withVariable("slot1", slot1)
                .withVariable("slot2", slotEmote?.asMention ?: "?")
                .withVariable("slot3", slotEmote?.asMention ?: "?")
        )
        msg.editMessage(eb.build()).queue()


        delay(1_000)
        eb.setDescription(
            spinningMsg
                .withVariable("slot1", slot1)
                .withVariable("slot2", slot2)
                .withVariable("slot3", slotEmote?.asMention ?: "?")
        )
        msg.editMessage(eb.build()).queue()


        delay(1_000)

        val finishedBody = "$stripe\n$slots\n$stripe"
        val finishedBodyMsg = finishedBody
            .withVariable("slot1", slot1)
            .withVariable("slot2", slot2)
            .withVariable("slot3", slot3)

        var won = false
        var multiplier = 0.0
        if (slot1 == slot2 && slot2 == slot3) { // Three in a row
            won = true

            multiplier = when {
                prizeList.filter { it.occurrence == 2 }.map { it.unicode }.contains(slot1) -> {
                    25.0
                }
                prizeList.filter { it.occurrence == 1 }.map { it.unicode }.contains(slot1) -> {
                    77.0
                }
                else -> 2.0
            }

        } else if (slot1 == slot2 || slot1 == slot3 || slot2 == slot3) { // Two in a row
            won = true
            multiplier = when {
                prizeList.filter { it.occurrence == 2 }.map { it.unicode }.contains(slot1) -> {
                    2.5
                }
                prizeList.filter { it.occurrence == 1 }.map { it.unicode }.contains(slot1) -> {
                    3.5
                }
                else -> 1.0
            }
        }

        eb.setDescription(finishedBodyMsg)
        val newBalance = balanceWrapper.addBalanceAndGet(context.authorId, (amount * multiplier).toLong())

        val newBalMsg = context.getTranslation("$root.newbalance")
        if (won) {
            val wonWord = context.getTranslation("word.won").toUpperCase()
            val profit = context.getTranslation("$root.profit")
            val wonBody = ("\n---------- **$wonWord**" +
                "\n$profit" +
                "\n$newBalMsg")
                .withVariable("profit", amount * multiplier - amount)
                .withVariable("bal", newBalance)

            eb.appendDescription(wonBody)
        } else {
            val lostWord = context.getTranslation("word.lost").toUpperCase()
            val loss = context.getTranslation("$root.loss")

            val lostMsg = ("\n---------- **$lostWord**" +
                "\n$loss" +
                "\n$newBalMsg")
                .withVariable("loss", amount)
                .withVariable("bal", newBalance)

            eb.appendDescription(lostMsg)
        }

        msg.editMessage(eb.build()).queue()
    }
}