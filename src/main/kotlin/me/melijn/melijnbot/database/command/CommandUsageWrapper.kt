package me.melijn.melijnbot.database.command

import me.melijn.melijnbot.Container
import me.melijn.melijnbot.internals.command.AbstractCommand
import java.util.*
import java.util.Map.Entry.comparingByValue
import kotlin.collections.Map.Entry
import kotlin.math.max

class CommandUsageWrapper(private val commandUsageDao: CommandUsageDao) {

    suspend fun addUse(commandId: Int) {
        commandUsageDao.addUse(commandId)
    }

    private suspend fun getUsageWithinPeriod(from: Long, until: Long): LinkedHashMap<Int, Long> {
        return commandUsageDao.getUsageWithinPeriod(from, until)
    }

    suspend fun getTopUsageWithinPeriod(from: Long, until: Long, top: Int): MutableMap<AbstractCommand, Long> {
        val map = getUsageWithinPeriod(from, until)
        val sortedAndTopped = sortAndTopOfUsageMap(map, top)
        return mapKeysToAbstractCommand(sortedAndTopped)
    }

    private fun mapKeysToAbstractCommand(map: MutableMap<Int, Long>): MutableMap<AbstractCommand, Long> {
        val newMutableMap = mutableMapOf<AbstractCommand, Long>()
        for ((id, usageCount) in map) {
            val cmd = Container.instance.commandMap[id] ?: continue
            newMutableMap[cmd] = usageCount
        }
        return newMutableMap
    }

    private fun mapKeysToAbstractCommand(list: List<Entry<Int, Long>>): MutableMap<AbstractCommand, Long> {
        val newMutableMap = mutableMapOf<AbstractCommand, Long>()
        for ((id, usageCount) in list) {
            val cmd = Container.instance.commandMap[id] ?: continue
            newMutableMap[cmd] = usageCount
        }
        return newMutableMap
    }

    private fun sortAndTopOfUsageMap(map: LinkedHashMap<Int, Long>, top: Int): MutableMap<Int, Long> {
        val sorted = sortUsage(map)
        val max = max(if (top == -1) map.size else top, 1)
        val limitedMap = mutableMapOf<Int, Long>()
        for ((i, v) in sorted) {
            if (limitedMap.size >= max) break
            limitedMap[i] = v
        }

        return limitedMap
    }

    suspend fun getFilteredUsageWithinPeriod(from: Long, until: Long, cmdList: List<Int>): MutableMap<AbstractCommand, Long> {
        val map = getUsageWithinPeriod(from, until).toMutableMap()
        val filtered = filterUsage(map, cmdList)
        val sorted = sortUsage(filtered)
        return mapKeysToAbstractCommand(sorted)
    }

    private fun filterUsage(map: MutableMap<Int, Long>, cmdList: List<Int>): MutableMap<Int, Long> {
        return map.filter { cmd -> cmdList.contains(cmd.key) }.toMutableMap()
    }

    private fun sortUsage(map: MutableMap<Int, Long>): List<Entry<Int, Long>> {
        val entries = map.entries.toList()
        return entries.sortedWith(comparingByValue()).reversed()
    }
}