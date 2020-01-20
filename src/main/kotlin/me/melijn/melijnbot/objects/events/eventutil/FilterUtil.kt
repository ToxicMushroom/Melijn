package me.melijn.melijnbot.objects.events.eventutil

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.Container
import me.melijn.melijnbot.commands.administration.getCacheFromFilterType
import me.melijn.melijnbot.database.filter.FilterGroup
import me.melijn.melijnbot.enums.FilterMode
import me.melijn.melijnbot.enums.FilterType
import me.melijn.melijnbot.enums.PointsTriggerType
import me.melijn.melijnbot.objects.jagtag.RegexJagTagParser
import me.melijn.melijnbot.objects.utils.LogUtils
import me.melijn.melijnbot.objects.utils.addIfNotPresent
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Message
import java.lang.Integer.max
import java.util.regex.Pattern

object FilterUtil {

    suspend fun handleFilter(container: Container, message: Message) = container.taskManager.async {
        val guild = message.guild
        val channel = message.textChannel
        val guildId = guild.idLong
        val channelId = channel.idLong
        val member = message.member ?: return@async
        val daoManager = container.daoManager
        if (!member.hasPermission(channel, Permission.MESSAGE_MANAGE) || member == guild.selfMember) return@async


        val groups = getFilterGroups(container, guildId, channelId)

        var points = 0
        val detectedMap = mutableMapOf<FilterGroup, List<String>>()
        val onlyDetected = mutableListOf<String>()
        val apWrapper = daoManager.autoPunishmentWrapper

        val ppMap = apWrapper.autoPunishmentCache.get(Pair(guild.idLong, member.idLong))
            .await()
            .toMutableMap()

        for (fg in groups) {
            val detected: List<String> = when (fg.mode) {
//                FilterMode.MUST_MATCH_ALLOWED_FORMAT -> {
//                    emptyList()
//                    //filterMatch(container, message)
//                }
//                FilterMode.MUST_MATCH_ALLOWED_FORMAT_EXCLUDE_FILTER -> {
//                    emptyList()
//                    // filterMatchNoDenied(container, message)
//                }
                FilterMode.NO_WRAP -> {
                    filterNoWrap(container, message, fg)
                }
                FilterMode.DEFAULT -> {
                    filterDefault(container, message, fg)
                }
                FilterMode.NO_MODE -> emptyList()
                FilterMode.DISABLED -> emptyList()
            }
            detectedMap[fg] = detected
            onlyDetected.addAll(detected)

            val extraPoints = (detected.size * fg.points)
            points += extraPoints
            ppMap[fg.filterGroupName] = ppMap.getOrDefault(fg.filterGroupName, 0) + extraPoints
        }

        apWrapper.set(guild.idLong, member.idLong, ppMap)
        if (onlyDetected.isNotEmpty()) {
            LogUtils.sendPPGainedMessageDMAndLC(container, message, PointsTriggerType.FILTERED_MESSAGE, onlyDetected.joinToString(), points)

            container.filteredMap[message.idLong] = onlyDetected.joinToString()
            message.delete().reason("Filter detection").queue()
        }
    }

    private suspend fun getFilterGroups(container: Container, guildId: Long, channelId: Long): List<FilterGroup> {
        return container.daoManager.filterGroupWrapper.filterGroupCache.get(guildId)
            .await()
            .filter { group ->
                group.channels.contains(channelId) || group.channels.isEmpty()
            }
    }

    private suspend fun filterNoWrap(container: Container, message: Message, fg: FilterGroup): List<String> {
        val guild = message.guild

        val deniedList = getFiltersForGroup(container, guild.idLong, fg, FilterType.DENIED)
        if (deniedList.isEmpty())
            return emptyList()

        val messageContent: String = message.contentRaw
        val detected = mutableListOf<String>()

        for (denied in deniedList) {
            val regexJagTag = RegexJagTagParser.makeIntoPattern(denied)
            val matcher = regexJagTag.matcher(messageContent)
            if (matcher.matches()) {
                detected.addIfNotPresent(denied)
            }
        }

        return detected
    }

    private suspend fun filterDefault(container: Container, message: Message, fg: FilterGroup): List<String> {
        val guild = message.guild

        val deniedList = getFiltersForGroup(container, guild.idLong, fg, FilterType.DENIED)
        val allowedList = getFiltersForGroup(container, guild.idLong, fg, FilterType.ALLOWED)

        val messageContent: String = message.contentRaw

        val detected = mutableListOf<String>()
        if (deniedList.isEmpty()) return emptyList()

        if (allowedList.isEmpty()) { //Allowed words are empty so just check for denied ones
            for (deniedWord in deniedList) {
                if (messageContent.contains(deniedWord, true)) {
                    detected.addIfNotPresent(deniedWord)
                }
            }
        } else { //Check if the allowed words are wrappin the denied ones
            val fgDeniedPositions = HashMap<Int, Int>()
            val fgAllowedPositions = HashMap<Int, Int>()
            addPositions(messageContent, fgDeniedPositions, deniedList)
            addPositions(messageContent, fgAllowedPositions, allowedList)

            if (fgAllowedPositions.isNotEmpty() && fgDeniedPositions.isNotEmpty()) {
                for ((beginDeniedIndex, endDeniedIndex) in fgDeniedPositions) {
                    for ((beginAllowedIndex, endAllowedIndex) in fgAllowedPositions) {
                        if (beginDeniedIndex < beginAllowedIndex || endDeniedIndex > endAllowedIndex) {
                            val newDetected = messageContent.substring(beginDeniedIndex, endDeniedIndex)
                            detected.addIfNotPresent(newDetected)
                        }
                    }
                }
            } else if (fgDeniedPositions.isNotEmpty()) {
                for (beginDeniedIndex in fgDeniedPositions.keys) {
                    val endDeniedIndex = fgDeniedPositions[beginDeniedIndex]
                        ?: throw IllegalArgumentException("I messed up")

                    val newDetected = messageContent.substring(beginDeniedIndex, endDeniedIndex)
                    detected.addIfNotPresent(newDetected)
                }
            }
        }

        return detected
    }

    private suspend fun getFiltersForGroup(container: Container, guildId: Long, fg: FilterGroup, type: FilterType): List<String> {
        return getCacheFromFilterType(container.daoManager, type)
            .get(Pair(guildId, fg.filterGroupName))
            .await()

    }

    private fun addPositions(message: String, positions: MutableMap<Int, Int>, detectionList: List<String>) {
        for (toFind in detectionList) {
            val word = Pattern.compile(Pattern.quote(toFind.toLowerCase()))
            val match = word.matcher(message.toLowerCase())

            while (match.find()) {
                val start = match.start()
                val end = positions.getOrElse(start) {
                    null
                }
                val newEnd = match.end()

                if (end != null) {
                    val actualEnd = max(end, newEnd)
                    positions[start] = actualEnd
                }

                positions[start] = newEnd
            }
        }
    }
}