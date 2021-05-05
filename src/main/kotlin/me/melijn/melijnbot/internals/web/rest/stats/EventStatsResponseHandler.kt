package me.melijn.melijnbot.internals.web.rest.stats

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.events.EventManager
import me.melijn.melijnbot.internals.web.RequestContext
import me.melijn.melijnbot.internals.web.WebUtils.respondJson
import me.melijn.melijnbot.objectMapper
import net.dv8tion.jda.api.utils.data.DataObject
import java.util.*

object EventStatsResponseHandler {

    var lastRequest = System.currentTimeMillis() - 60_000

    suspend fun handleEventStatsResponse(context: RequestContext) {
        val cmdUsesMap = mutableMapOf<Int, Int>()
        val entityUsesMap =
            mutableMapOf<Long, Int>() // the keys in this map can actually also be userIds for dm-commands
        AbstractCommand.commandUsageList.forEach {
            cmdUsesMap[it.commandId] = (cmdUsesMap[it.commandId] ?: 0) + 1
            entityUsesMap[it.guildId] = (entityUsesMap[it.guildId] ?: 0) + 1
        }

        val highestGuilds = LinkedList<Pair<Long, Int>>()
        entityUsesMap.forEach { (id, uses) ->
            if (highestGuilds.isEmpty()) highestGuilds.add(id to uses)
            else for (index in 0 until highestGuilds.size) {
                val el = highestGuilds[index]
                if (el.second < uses) {
                    highestGuilds.add(id to uses)
                    if (highestGuilds.size > 10) highestGuilds.removeLast()
                    break
                }
            }
        }

        val dataObject = DataObject.empty()
            .put("events", objectMapper.writeValueAsString(EventManager.eventCountMap))
            .put("commandUses", objectMapper.writeValueAsString(cmdUsesMap))
            .put("highestGuilds", objectMapper.writeValueAsString(highestGuilds))
            .put("lastPoint", lastRequest)
        resetEventCounter()
        resetCommands()
        context.call.respondJson(dataObject)
        lastRequest = System.currentTimeMillis()
    }

    private suspend fun resetCommands() {
        AbstractCommand.commandUsageList.clear()
    }

    private fun resetEventCounter() {
        EventManager.eventCountMap.clear()
    }
}