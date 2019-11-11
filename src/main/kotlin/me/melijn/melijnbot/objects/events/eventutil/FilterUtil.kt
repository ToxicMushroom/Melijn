package me.melijn.melijnbot.objects.events.eventutil

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.Container
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Message
import java.util.*
import java.util.regex.Pattern

object FilterUtil {
    suspend fun filterDefault(container: Container, message: Message) {
        val guild = message.guild
        val selfMember = guild.selfMember
        val channel = message.textChannel
        if (!selfMember.hasPermission(channel, Permission.MESSAGE_MANAGE)) return

        val filterWrapper = container.daoManager.filterWrapper
        val globalPair = Pair(guild.idLong, -1L)
        val specificPair = Pair(guild.idLong, channel.idLong)
        val globalDeniedFilters = filterWrapper.deniedFilterCache.get(globalPair).await()
        val globalAllowedFilters = filterWrapper.allowedFilterCache.get(globalPair).await()
        val specificDeniedFilters = filterWrapper.deniedFilterCache.get(specificPair).await()
        val specificAllowedFilters = filterWrapper.deniedFilterCache.get(specificPair).await()

        val deniedList = globalDeniedFilters + specificDeniedFilters
        val allowedList = globalAllowedFilters + specificAllowedFilters

        val messageContent: String = message.contentRaw
        val detectedWord = StringBuilder()
        if (deniedList.isEmpty()) return

        var ranOnce = false

        if (allowedList.isEmpty()) {
            for (deniedWord in deniedList) {
                if (messageContent.contains(deniedWord, true)) {
                    detectedWord.append(if (ranOnce) ", " else "").append(deniedWord)
                    ranOnce = true
                }
            }
        } else {
            val deniedPositions: MutableMap<Int, Int> = HashMap()
            val allowedPositions: MutableMap<Int, Int> = HashMap()
            addPositions(messageContent, deniedPositions, deniedList)
            addPositions(messageContent, allowedPositions, allowedList)
            if (allowedPositions.isNotEmpty() && deniedPositions.isNotEmpty()) {
                for (beginDenied in deniedPositions.keys) {
                    val endDenied = deniedPositions[beginDenied] ?: return
                    for (beginAllowed in allowedPositions.keys) {
                        val endAllowed = allowedPositions[beginAllowed] ?: return
                        if (beginDenied > beginAllowed && endDenied < endAllowed) continue
                        detectedWord.append(messageContent, beginDenied, endDenied)
                    }
                }
            } else if (deniedPositions.isNotEmpty()) {
                for (beginDenied in deniedPositions.keys) {
                    val endDenied = deniedPositions[beginDenied] ?: return
                    detectedWord.append(if (ranOnce) ", " else "").append(messageContent, beginDenied, endDenied)
                    ranOnce = true
                }
            }
        }
        if (detectedWord.isNotEmpty()) {
            container.filteredMap[message.idLong] = detectedWord.toString()
            message.delete().reason("Filter detection").queue()
        }
    }

    private fun addPositions(message: String, deniedPositions: MutableMap<Int, Int>, deniedList: List<String>) {
        for (toFind in deniedList) {
            val word = Pattern.compile(Pattern.quote(toFind.toLowerCase()))
            val match = word.matcher(message.toLowerCase())
            while (match.find()) {
                deniedPositions[match.start()] = match.end()
            }
        }
    }
}