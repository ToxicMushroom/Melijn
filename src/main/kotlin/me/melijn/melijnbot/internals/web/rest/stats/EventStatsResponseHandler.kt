package me.melijn.melijnbot.internals.web.rest.stats

import me.melijn.melijnbot.internals.events.EventManager
import me.melijn.melijnbot.internals.web.RequestContext
import me.melijn.melijnbot.internals.web.WebUtils.respondJson
import me.melijn.melijnbot.objectMapper

object EventStatsResponseHandler {

    var lastRequest = System.currentTimeMillis()

    suspend fun handleEventStatsResponse(context: RequestContext) {
        val dataObject = computeBaseObject()
            .put("events", objectMapper.writeValueAsString(EventManager.eventCountMap))
            .put("lastPoint", lastRequest)
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