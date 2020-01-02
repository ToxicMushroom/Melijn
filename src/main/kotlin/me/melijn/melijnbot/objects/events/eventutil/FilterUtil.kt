package me.melijn.melijnbot.objects.events.eventutil

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.Container
import me.melijn.melijnbot.commands.administration.getCacheFromFilterType
import me.melijn.melijnbot.enums.FilterMode
import me.melijn.melijnbot.enums.FilterType
import me.melijn.melijnbot.objects.jagtag.RegexJagTagParser
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Message
import java.util.*
import java.util.regex.Pattern

object FilterUtil {

    suspend fun handleFilter(container: Container, message: Message) = container.taskManager.async {
        val guild = message.guild
        val channel = message.textChannel
        val guildId = guild.idLong
        val channelId = channel.idLong
        val member = message.member ?: return@async
        val daoManager = container.daoManager
        if (member.hasPermission(channel, Permission.MESSAGE_MANAGE) || member == guild.selfMember) return@async

        val channelFilterMode = daoManager.filterModeWrapper.filterWrappingModeCache.get(Pair(guildId, channelId)).await()
        val effectiveMode: FilterMode = if (channelFilterMode == FilterMode.NO_MODE) {
            val guildFilterMode = daoManager.filterModeWrapper.filterWrappingModeCache.get(Pair(guildId, null)).await()
            if (guildFilterMode == FilterMode.NO_MODE) {
                FilterMode.DEFAULT
            } else {
                guildFilterMode
            }
        } else channelFilterMode

        when (effectiveMode) {
            FilterMode.MUST_MATCH_ALLOWED_FORMAT -> {
                //filterMatch(container, message)
            }
            FilterMode.MUST_MATCH_ALLOWED_FORMAT_EXCLUDE_FILTER -> {
                // filterMatchNoDenied(container, message)
            }
            FilterMode.NO_WRAP -> {
                filterNoWrap(container, message)
            }
            FilterMode.DEFAULT -> {
                filterDefault(container, message)
            }
            FilterMode.NO_MODE -> throw IllegalArgumentException("Should be DEFAULT rn")
            FilterMode.DISABLED -> return@async
        }
    }

    private suspend fun filterNoWrap(container: Container, message: Message) {
        val guild = message.guild
        val selfMember = guild.selfMember
        val channel = message.textChannel
        if (!selfMember.hasPermission(channel, Permission.MESSAGE_MANAGE)) return


        val deniedList = getFiltersForChannel(container, guild.idLong, channel.idLong, FilterType.DENIED)
        if (deniedList.isEmpty()) return

        val messageContent: String = message.contentRaw


        val detectedWord = deniedList
            .filter { denied ->
                val regexJagTag = RegexJagTagParser.makeIntoPattern(denied)
                val matcher = regexJagTag.matcher(messageContent)
                matcher.matches()
            }
            .joinToString()

        if (detectedWord.isNotEmpty()) {
            container.filteredMap[message.idLong] = detectedWord
            message.delete().reason("Filter detection").queue()
        }
    }

    suspend fun filterDefault(container: Container, message: Message) {
        val guild = message.guild
        val selfMember = guild.selfMember
        val channel = message.textChannel
        if (!selfMember.hasPermission(channel, Permission.MESSAGE_MANAGE)) return

        val deniedList = getFiltersForChannel(container, guild.idLong, channel.idLong, FilterType.DENIED)
        val allowedList = getFiltersForChannel(container, guild.idLong, channel.idLong, FilterType.ALLOWED)

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
                        if (beginDenied < beginAllowed || endDenied > endAllowed) {
                            detectedWord.append(messageContent, beginDenied, endDenied)
                        }
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

    suspend fun getFiltersForChannel(container: Container, guildId: Long, textChannelId: Long, filterType: FilterType): List<String> {
        val filterGroups = container.daoManager.filterGroupWrapper.filterGroupCache.get(guildId).await()
        filterGroups.filter { group -> group.channels.contains(textChannelId) }
        val cache = getCacheFromFilterType(container.daoManager, filterType)
        val filters = mutableListOf<String>()
        for ((filterGroupName) in filterGroups) {
            filters.addAll(cache.get(Pair(guildId, filterGroupName)).await())
        }
        return filters
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