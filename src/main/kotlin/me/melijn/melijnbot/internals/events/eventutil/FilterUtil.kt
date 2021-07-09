package me.melijn.melijnbot.internals.events.eventutil

import me.melijn.melijnbot.Container
import me.melijn.melijnbot.database.filter.FilterGroup
import me.melijn.melijnbot.enums.FilterMode
import me.melijn.melijnbot.enums.FilterType
import me.melijn.melijnbot.enums.PointsTriggerType
import me.melijn.melijnbot.internals.jagtag.RegexJagTagParser
import me.melijn.melijnbot.internals.threading.TaskManager
import me.melijn.melijnbot.internals.utils.LogUtils
import me.melijn.melijnbot.internals.utils.PPUtils
import me.melijn.melijnbot.internals.utils.addIfNotPresent
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Message
import java.lang.Integer.max
import java.util.regex.Pattern

object FilterUtil {

    suspend fun handleFilter(container: Container, message: Message) =
        TaskManager.async(message.author, message.channel) {
            if (message.author.isBot) return@async
            val guild = message.guild
            val member = message.member ?: return@async
            val channel = message.textChannel
            if (member.hasPermission(channel, Permission.MESSAGE_MANAGE)) return@async
            if (!guild.selfMember.hasPermission(channel, Permission.MESSAGE_MANAGE)) return@async

            val guildId = guild.idLong
            val channelId = channel.idLong

            // Filter groups for this channel
            val groups = getFilterGroups(container, guildId, channelId)

            var points = 0 // Points total of this message
            val filterGroupTriggerInfoMap =
                mutableMapOf<FilterGroup, Map<String, List<String>>>() // Map of filtergroup -> Map of <infotype -> info>
            val onlyTriggerInfoMap =
                mutableMapOf<String, List<String>>() // Map of <infotype -> info> not bound by filtergroup

            // The extra points map for a user (guildId, member) -> Map<List<punishGroupName>, points>
            val extraPPMap = mutableMapOf<List<String>, Int>()

            // If a will be set to true by a deleting filterGroup if needed ofc
            var shouldDelete = false

            // Loop through the filter groups
            for (fg in groups) {
                if (!fg.state) continue
                // Detected stuff returned from the case from mode of the filtergroup
                val map = mutableMapOf<String, List<String>>()
                var extraPoints = 0
                when (fg.mode) {
                    FilterMode.MUST_CONTAIN_ANY_ALLOWED -> {
                        map["contain.any"] = mustContainAny(container, message, fg)
                        if (map.getValue("contain.any").isNotEmpty()) {
                            extraPoints = fg.points
                        }
                    }
                    FilterMode.MUST_CONTAIN_ALL_ALLOWED -> {
                        map["contain.all"] = mustContainAll(container, message, fg)
                        if (map.getValue("contain.all").isNotEmpty()) {
                            extraPoints = fg.points
                        }
                    }
                    FilterMode.MUST_MATCH_ALLOWED_FORMAT -> {
                        map["must.match"] = filterMatch(container, message, fg)
                        map["mustnot.contain"] = filterNoWrap(container, message, fg)
                        if (map.getValue("mustnot.contain").isNotEmpty()) {
                            extraPoints = fg.points * map.getValue("mustnot.contain").size
                        }
                        if (map.getValue("must.match").isNotEmpty()) {
                            extraPoints = fg.points
                        }
                    }
                    FilterMode.NO_WRAP -> {
                        map["mustnot.contain"] = filterNoWrap(container, message, fg)
                        if (map.getValue("mustnot.contain").isNotEmpty()) {
                            extraPoints = fg.points * map.getValue("mustnot.contain").size
                        }
                    }
                    FilterMode.DEFAULT -> {
                        map["mustnot.contain"] = filterDefault(container, message, fg)
                        if (map.getValue("mustnot.contain").isNotEmpty()) {
                            extraPoints = fg.points * map.getValue("mustnot.contain").size
                        }
                    }
                    FilterMode.NO_MODE, FilterMode.DISABLED -> {
                    }
                }
                if (fg.deleteHit && map.isNotEmpty()) {
                    shouldDelete = true
                }
                filterGroupTriggerInfoMap[fg] = map // Put info in a map bound to its filter group for later use
                for ((key, value) in map) { // Merge the total info with new info
                    if (value.isEmpty()) continue
                    val currentInfo = onlyTriggerInfoMap.getOrElse(key) { emptyList() }
                    onlyTriggerInfoMap[key] = currentInfo + value
                }

                points += extraPoints // add extra points to points total of this filter check
                extraPPMap[fg.punishGroupNames] = extraPoints // save extra points to the ppMap
            }

            // If something is detected
            if (onlyTriggerInfoMap.isNotEmpty()) {
                container.filteredMap[message.idLong] =
                    onlyTriggerInfoMap // Store detected and notMatched reason in the filteredMap for logging the deletion reason

                if (shouldDelete)
                    message.delete().reason("Filter detection").queue()  // delete the message

                LogUtils.sendPPGainedMessageDMAndLC(
                    container,
                    message,
                    PointsTriggerType.FILTERED_MESSAGE,
                    onlyTriggerInfoMap,
                    points
                ) // send a dm and log the violation
                PPUtils.updatePP(
                    member,
                    extraPPMap,
                    container,
                    PointsTriggerType.FILTERED_MESSAGE
                ) // Update the punishment points of the user
            }
        }

    private suspend fun mustContainAll(container: Container, message: Message, fg: FilterGroup): List<String> {
        val guild = message.guild

        val allMatchList = getFiltersForGroup(container, guild.idLong, fg, FilterType.ALLOWED).toMutableList()
        val noMatch = mutableListOf<String>()

        allMatchList.forEach {
            if (!message.contentRaw.contains(it)) {
                noMatch.add(it)
            }
        }

        return noMatch
    }

    private suspend fun mustContainAny(container: Container, message: Message, fg: FilterGroup): List<String> {
        val guild = message.guild

        val anyMatchList = getFiltersForGroup(container, guild.idLong, fg, FilterType.ALLOWED)

        return if (anyMatchList.any { message.contentRaw.contains(it) }) {
            emptyList()
        } else {
            anyMatchList
        }
    }

    // Returns failed mustMatch patterns
    private suspend fun filterMatch(container: Container, message: Message, fg: FilterGroup): List<String> {
        val guild = message.guild

        val mustMatchList = getFiltersForGroup(container, guild.idLong, fg, FilterType.ALLOWED)

        val noMatch = mutableListOf<String>()
        for (mustMatch in mustMatchList) {
            if (!RegexJagTagParser.makeIntoPattern(mustMatch).matcher(message.contentRaw).matches()) {
                noMatch.add(mustMatch)
            }
        }

        return noMatch
    }

    private suspend fun getFilterGroups(container: Container, guildId: Long, channelId: Long): List<FilterGroup> {
        return container.daoManager.filterGroupWrapper.getGroups(guildId)
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

        if (allowedList.isEmpty()) { // Allowed words are empty so just check for denied ones
            for (deniedWord in deniedList) {
                if (messageContent.contains(deniedWord, true)) {
                    detected.addIfNotPresent(deniedWord)
                }
            }
        } else { // Check if the allowed words are wrapping the denied ones
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

    private suspend fun getFiltersForGroup(
        container: Container,
        guildId: Long,
        fg: FilterGroup,
        type: FilterType
    ): List<String> {
        return container.daoManager.filterWrapper.getFilters(guildId, fg.filterGroupName, type)
    }

    private fun addPositions(message: String, positions: MutableMap<Int, Int>, detectionList: List<String>) {
        for (toFind in detectionList) {
            val word = Pattern.compile(Pattern.quote(toFind.lowercase()))
            val match = word.matcher(message.lowercase())

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