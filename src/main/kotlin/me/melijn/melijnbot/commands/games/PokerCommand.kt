package me.melijn.melijnbot.commands.games

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.embed.Embedder
import me.melijn.melijnbot.internals.utils.getBalanceNMessage
import me.melijn.melijnbot.internals.utils.message.sendEmbedRsp
import me.melijn.melijnbot.internals.utils.message.sendEmbedRspAwaitEL
import me.melijn.melijnbot.internals.utils.message.sendSyntax
import me.melijn.melijnbot.internals.utils.withVariable
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import kotlin.random.Random


class PokerCommand : AbstractCommand("command.poker") {

    init {
        id = 218
        name = "poker"
        cooldown = 4000
        commandCategory = CommandCategory.GAME
    }

    companion object {
        val ongoingPoker = mutableListOf<PokerGame>()
    }

    data class PokerGame(
        val userId: Long,
        val bet: Long,
        val firstHand: String,
        val channelId: Long,
        val msgId: Long,
        val moment: Long,
        var reactions: List<Int>
    )

    private val ranks = listOf("2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "R", "A")
    private val types = listOf('♠', '♣', '♥', '♦')

    override suspend fun execute(context: CommandContext) {
        if (context.args.isEmpty()) {
            sendSyntax(context)
            return
        }

        val balanceWrapper = context.daoManager.balanceWrapper
        val amount = getBalanceNMessage(context, 0) ?: return

        context.initCooldown()
        balanceWrapper.removeBalance(context.authorId, amount)

        try {
            val allCards = mutableListOf<String>()
            for (type in types) {
                for (rank in ranks) {
                    allCards.add("$rank$type")
                }
            }

            val eb = Embedder(context)


            var hand = ""
            for (i in 0 until 5) {
                val cardPosition = Random.nextInt(allCards.size)
                val card = allCards[cardPosition]
                allCards.removeAt(cardPosition)
                hand += "$card, "
            }
            hand.removeSuffix(", ")

            eb.addField(
                context.getTranslation("$root.title1").withVariable(
                    "user",
                    context.member.effectiveName
                ), hand, false
            )

            eb.addField(
                context.getTranslation("$root.actions"),
                context.getTranslation(
                    "$root.actionsvalue"
                ).withVariable("prefix", context.usedPrefix),
                false
            )

            eb.setFooter(
                context.getTranslation(
                    "$root.helpfooter"
                )
            )

            val msg = sendEmbedRspAwaitEL(context, eb.build()).first()
            msg.addReaction("1️⃣").queue()
            msg.addReaction("2️⃣").queue()
            msg.addReaction("3️⃣").queue()
            msg.addReaction("4️⃣").queue()
            msg.addReaction("5️⃣").queue()

            ongoingPoker.add(
                PokerGame(
                    context.authorId,
                    amount,
                    hand,
                    msg.channel.idLong,
                    msg.idLong,
                    System.currentTimeMillis(),
                    emptyList()
                )
            )

            context.container.eventWaiter.waitFor(MessageReceivedEvent::class.java, { event ->
                event.message.contentRaw == "${context.prefix}draw" &&
                    ongoingPoker.any { it.userId == event.author.idLong && it.channelId == event.channel.idLong }
            }, func@{ event ->
                val pokerGame = ongoingPoker.first { it.userId == event.author.idLong }

                val lockedCards = pokerGame.firstHand.split(", ").withIndex().filter {
                    pokerGame.reactions.contains(it.index + 1)
                }

                val nexCardPool = mutableListOf<String>()
                for (type in types) {
                    for (rank in ranks) {
                        if (!lockedCards.map { it.value }.contains("$rank$type")) {
                            nexCardPool.add("$rank$type")
                        }
                    }
                }

                val finalCards = mutableListOf<String>()
                for (i in 0 until 5) {
                    if (lockedCards.any { it.index == i }) {
                        finalCards.add(lockedCards.first { it.index == i }.value)
                    } else {
                        val cardPosition = Random.nextInt(allCards.size)
                        val card = allCards[cardPosition]
                        allCards.removeAt(cardPosition)
                        finalCards.add(card)
                    }
                }

                val ebb = Embedder(context)
                    .setTitle(context.getTranslation("$root.finalhandtitle"))
                    .setDescription(finalCards.joinToString(", "))
                sendEmbedRsp(context, ebb.build())


                val pokerHand = findPokerHand(finalCards)

                val bal = if (pokerHand != null) {
                    balanceWrapper.addBalanceAndGet(context.authorId, (pokerGame.bet * pokerHand.multiplier).toLong())
                } else {
                    balanceWrapper.getBalance(context.authorId)
                }


                if (pokerHand != null) {

                    val ebb1 = Embedder(context)
                        .addField(
                            pokerHand.name,
                            context.getTranslation("$root.won")
                                .withVariable("amount", pokerGame.bet * pokerHand.multiplier),
                            false
                        )
                        .addField(
                            context.getTranslation("$root.mel"),
                            context.getTranslation("$root.newbalance")
                                .withVariable("amount", bal),
                            false
                        )

                    sendEmbedRsp(context, ebb1.build())
                } else {
                    val ebb1 = Embedder(context)
                        .addField(
                            context.getTranslation("$root.losttitle"),
                            context.getTranslation("$root.lost")
                                .withVariable("amount", pokerGame.bet),
                            false
                        )
                        .addField(
                            context.getTranslation("$root.mel"),
                            context.getTranslation("$root.newbalance")
                                .withVariable("amount", bal),
                            false
                        )
                    sendEmbedRsp(context, ebb1.build())
                }

                ongoingPoker.removeIf { it.userId == context.authorId }
            }, {
                ongoingPoker.removeIf { it.userId == context.authorId }
            }, 120)

        } catch (t: Throwable) {
            balanceWrapper.addBalance(context.authorId, amount)
        }
    }

    private fun findPokerHand(finalCards: MutableList<String>): PokerHand? {
        // Royal flush
        val ctypes = finalCards.map { it.last() }
        val cranks = finalCards.map { it.dropLast(1) }

        val ranksOccurrenceMap = mutableMapOf<Int, Int>()

        for (card in finalCards) {
            val r = card.dropLast(1)
            ranksOccurrenceMap[ranks.indexOf(r)] = (ranksOccurrenceMap[ranks.indexOf(r)] ?: 0) + 1
        }

        val type1 = ctypes[0]

        val positions = cranks.map { ranks.indexOf(it) }.sorted()

        var flush = false
        if (ctypes.all { it == type1 }) { // flush check (all same type)
            if (
                cranks.contains("A") &&
                cranks.contains("K") &&
                cranks.contains("Q") &&
                cranks.contains("J") &&
                cranks.contains("10")
            ) { // Royal flush check
                return PokerHand("Royal Flush", 100f)
            }

            var lastRank = positions[0]
            var indices = 1 until 5
            if (lastRank == 0 && positions[4] == 12) { // if lastRank card is 2 and hand contains 1 then we can use A as a 1
                lastRank = positions[4]
                indices = 0 until 4
            }

            var straightFlush = true
            for (i in indices) {
                if (i == (lastRank + 1)) {
                    lastRank = positions[i]
                } else {
                    straightFlush = false
                }
            }

            if (straightFlush) {
                return PokerHand("Straight Flush", 50f)
            }

            flush = true
        }

        if (ranksOccurrenceMap.size <= 2) {
            if (ranksOccurrenceMap.values.sorted()[0] == 4) { // 4 of a  kind
                return PokerHand("Four of a Kind", 7f)
            } else if (ranksOccurrenceMap.values.sorted()[0] == 3) { // Full house
                return PokerHand("Full House", 5f)
            }
        }

        if (flush) {
            return PokerHand("Flush", 4f)
        }

        // Straight check
        var lastRank = positions[0]
        var indices = 1 until 5
        if (lastRank == 0 && positions[4] == 12) { // if lastRank card is 2 and hand contains 1 then we can use A as a 1
            lastRank = positions[4]
            indices = 0 until 4
        }

        var straight = true
        for (i in indices) {
            if (i == (lastRank + 1)) {
                lastRank = positions[i]
            } else {
                straight = false
            }
        }

        if (straight) {
            return PokerHand("Straight", 50f)
        }

        if (ranksOccurrenceMap.size == 3 && ranksOccurrenceMap.values.sorted()[0] == 2 && ranksOccurrenceMap.values.sorted()[1] == 2) { // Two Pair
            return PokerHand("Two Pair", 2f)
        }

        return null
    }

    data class PokerHand(
        val name: String,
        val multiplier: Float
    )
}

