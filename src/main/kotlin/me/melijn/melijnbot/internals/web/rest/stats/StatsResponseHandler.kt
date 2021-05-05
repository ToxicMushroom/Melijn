package me.melijn.melijnbot.internals.web.rest.stats

import me.melijn.melijnbot.internals.events.EventManager
import me.melijn.melijnbot.internals.web.RequestContext
import me.melijn.melijnbot.internals.web.WebUtils.respondJson
import me.melijn.melijnbot.objectMapper

object StatsResponseHandler {

    var lastRequest = System.currentTimeMillis() - 60_000

    @Deprecated("Marked for removal", replaceWith = ReplaceWith("EventStatsResponseHandler#handleEventStatsResponse"))
    suspend fun handleStatsResponse(context: RequestContext) {
        val dataObject = computeBaseObject()
            .put("events", objectMapper.writeValueAsString(EventManager.eventCountMap))
            .put("lastPoint", lastRequest)
            .put("shards", computePublicStatsObject(context))
        resetEventCounter()
        context.call.respondJson(dataObject)
        lastRequest = System.currentTimeMillis()
    }

    private fun resetEventCounter() {
        for (event in EventManager.eventCountMap.keys) {
            EventManager.eventCountMap[event] = 0
        }
    }
}