package me.melijn.melijnbot.internals.events.eventutil

import me.melijn.melijnbot.Container
import me.melijn.melijnbot.internals.models.SpammingUser
import net.dv8tion.jda.api.MessageBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent

object SpammingUtil {

    val spamMap = mutableMapOf<Long, List<SpammingUser>>()

    fun handleSpam(container: Container, event: GuildMessageReceivedEvent) {
        if (event.author.isBot) return
        val member = event.member ?: return
        val channel = event.channel
        if (member.hasPermission(channel, Permission.MESSAGE_MANAGE)) return

        val spamList = spamMap.getOrDefault(event.guild.idLong, emptyList()).toMutableList()
        val cTime = System.currentTimeMillis()
        val possibleSpammer = spamList.firstOrNull { (userId) -> userId == member.idLong }
            ?: SpammingUser(member.idLong, cTime, 0, 0b00)

        val shortLimit = 0.9 // messages per second for 4 seconds
        val medLimit = 0.75 // messages per second for 20 seconds

        val timeDif = (cTime - possibleSpammer.startTime)
        if ((possibleSpammer.count / timeDif) >= shortLimit && (timeDif >= 4_000)) {
            val shortSpamWarn = 0b1
            if ((possibleSpammer.responses and shortSpamWarn) == 0) {
                val warnMessage = MessageBuilder().apply {
                    setContent("${event.author.asMention} don't spam please")
                    mentionUsers(event.author.idLong)
                }.build()
                event.channel.sendMessage(warnMessage).queue()
                possibleSpammer.responses = possibleSpammer.responses or shortSpamWarn
            }
            event.message.delete().reason("spamming").queue()
        } else if ((possibleSpammer.count / timeDif) >= medLimit && (timeDif >= 20_000)) {
            event.member?.ban(1, "spamming")?.queue()
        } else {
            // no threshold violation yet
        }
        possibleSpammer.count++

    }

}