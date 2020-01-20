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
        //if (member.hasPermission(channel, Permission.MESSAGE_MANAGE) || member == guild.selfMember) return@async


        val groups = getFilterGroups(container, guildId, channelId)

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
        val userId = message.author.idLong
        if (!selfMember.hasPermission(channel, Permission.MESSAGE_MANAGE)) return


        val deniedMap = getFiltersForChannel(container, guild.idLong, channel.idLong, FilterType.DENIED)
        if (deniedMap.isEmpty()) return

        val messageContent: String = message.contentRaw

        val detected = mutableMapOf<FilterGroup, List<String>>()
        val onlyDetected = mutableListOf<String>()
        var points = 0
        val apWrapper = container.daoManager.autoPunishmentWrapper

        for ((fg, deniedList) in deniedMap) {
            for (denied in deniedList) {
                val regexJagTag = RegexJagTagParser.makeIntoPattern(denied)
                val matcher = regexJagTag.matcher(messageContent)
                if (matcher.matches()) {
                    detected[fg] = detected.getOrDefault(fg, emptyList()) + denied
                    onlyDetected.addIfNotPresent(denied)

                    points += fg.points
                    val ppMap = apWrapper.autoPunishmentCache.get(Pair(guild.idLong, userId))
                        .await()
                        .toMutableMap()
                    ppMap[fg.filterGroupName] = ppMap.getOrDefault(fg.filterGroupName, 0) + fg.points
                    apWrapper.set(guild.idLong, userId, ppMap)
                }
            }
        }

        if (detected.isNotEmpty()) {
            LogUtils.sendPPGainedMessageDMAndLC(container, message, PointsTriggerType.FILTERED_MESSAGE, onlyDetected.joinToString(), points)

            container.filteredMap[message.idLong] = onlyDetected.joinToString()
            message.delete().reason("Filter detection").queue()
        }
    }

    suspend fun filterDefault(container: Container, message: Message) {
        val guild = message.guild
        val userId = message.author.idLong
        val selfMember = guild.selfMember
        val channel = message.textChannel
        if (!selfMember.hasPermission(channel, Permission.MESSAGE_MANAGE)) return

        val deniedMap = getFiltersForChannel(container, guild.idLong, channel.idLong, FilterType.DENIED)
        val allowedMap = getFiltersForChannel(container, guild.idLong, channel.idLong, FilterType.ALLOWED)

        val messageContent: String = message.contentRaw

        val detected = mutableMapOf<FilterGroup, List<String>>()
        val onlyDetected = mutableListOf<String>()
        var points = 0
        val apWrapper = container.daoManager.autoPunishmentWrapper

        if (deniedMap.isEmpty()) return

        val detectedMap = mutableMapOf<FilterGroup, MutableList<String>>()
        if (allowedMap.isEmpty()) { //Allowed words are empty so just check for denied ones
            for ((fg, deniedList) in deniedMap) {
                for (deniedWord in deniedList) {
                    if (messageContent.contains(deniedWord, true)) {
                        detectedMap[fg] = (detectedMap.getOrDefault(fg, emptyList<String>()) + deniedWord).toMutableList()
                    }
                }
            }
        } else { //Check if the allowed words are wrappin the denied ones
            val fgDeniedPositions: MutableMap<FilterGroup, Map<Int, Int>> = HashMap()
            val fgAllowedPositions: MutableMap<FilterGroup, Map<Int, Int>> = HashMap()
            addPositions(messageContent, fgDeniedPositions, deniedMap)
            addPositions(messageContent, fgAllowedPositions, allowedMap)
            for (fg in fgDeniedPositions.keys) {
                if (fgAllowedPositions.isNotEmpty() && fgDeniedPositions.isNotEmpty()) {
                    for ((beginDeniedIndex, endDeniedIndex) in fgDeniedPositions.getOrDefault(fg, emptyMap())) {
                        for ((beginAllowedIndex, endAllowedIndex) in fgAllowedPositions.getOrDefault(fg, emptyMap())) {
                            if (beginDeniedIndex < beginAllowedIndex || endDeniedIndex > endAllowedIndex) {
                                val newDetected = messageContent.substring(beginDeniedIndex, endDeniedIndex)
                                onlyDetected.addIfNotPresent(newDetected)
                                detected[fg] = detected.getOrDefault(fg, emptyList()) + newDetected

                                points += fg.points
                                val ppMap = apWrapper.autoPunishmentCache.get(Pair(guild.idLong, userId))
                                    .await()
                                    .toMutableMap()
                                ppMap[fg.filterGroupName] = ppMap.getOrDefault(fg.filterGroupName, 0) + fg.points
                                apWrapper.set(guild.idLong, userId, ppMap)
                            }
                        }
                    }
                } else if (fgDeniedPositions.isNotEmpty()) {
                    for (beginDeniedIndex in fgDeniedPositions.getOrDefault(fg, emptyMap()).keys) {
                        val endDeniedIndex = fgDeniedPositions.getOrDefault(fg, emptyMap())[beginDeniedIndex]
                            ?: throw IllegalArgumentException("I messed up")

                        val newDetected = messageContent.substring(beginDeniedIndex, endDeniedIndex)
                        onlyDetected.addIfNotPresent(newDetected)
                        detected[fg] = detected.getOrDefault(fg, emptyList()) + newDetected

                        points += fg.points
                        val ppMap = apWrapper.autoPunishmentCache.get(Pair(guild.idLong, userId))
                            .await()
                            .toMutableMap()
                        ppMap[fg.filterGroupName] = ppMap.getOrDefault(fg.filterGroupName, 0) + fg.points
                        apWrapper.set(guild.idLong, userId, ppMap)
                    }
                }
            }
        }

        if (detected.isNotEmpty()) {
            LogUtils.sendPPGainedMessageDMAndLC(container, message, PointsTriggerType.FILTERED_MESSAGE, onlyDetected.joinToString(), points)

            container.filteredMap[message.idLong] = onlyDetected.joinToString()
            message.delete().reason("Filter detection").queue()
        }
    }

    private suspend fun getFiltersForChannel(container: Container, guildId: Long, textChannelId: Long, filterType: FilterType): Map<FilterGroup, List<String>> {
        val filterGroups = container.daoManager.filterGroupWrapper.filterGroupCache.get(guildId).await()

        filterGroups.filter { group -> group.channels.contains(textChannelId) }

        val cache = getCacheFromFilterType(container.daoManager, filterType)

        val filterGroupFilters = mutableMapOf<FilterGroup, List<String>>()

        for (filterGroup in filterGroups) {
            filterGroupFilters[filterGroup] = cache.get(Pair(guildId, filterGroup.filterGroupName)).await()
        }

        return filterGroupFilters
    }

    private fun addPositions(message: String, positions: MutableMap<FilterGroup, Map<Int, Int>>, detectionMap: Map<FilterGroup, List<String>>) {
        for ((fg, deniedList) in detectionMap) {

            for (toFind in deniedList) {
                val word = Pattern.compile(Pattern.quote(toFind.toLowerCase()))
                val match = word.matcher(message.toLowerCase())

                while (match.find()) {
                    val bb = positions
                        .getOrDefault(fg, emptyMap())
                        .toMutableMap()

                    val start = match.start()
                    val end = bb.getOrElse(start) { null }
                    val newEnd = match.end()

                    if (end != null) {
                        val actualEnd = max(end, newEnd)
                        bb[start] = actualEnd
                    }

                    bb[start] = newEnd
                    positions[fg] = bb
                }
            }
        }
    }
}