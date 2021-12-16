package me.melijn.melijnbot.internals.web.rest.stats

import me.melijn.melijnbot.internals.services.ratelimits.RatelimitService
import me.melijn.melijnbot.internals.web.RequestContext
import me.melijn.melijnbot.internals.web.WebUtils.respondJson
import me.melijn.melijnbot.objectMapper
import net.dv8tion.jda.api.utils.data.DataObject

object RatelimitInfoHandler {

    suspend fun handleStatsResponse(context: RequestContext) {
        val dataObject = DataObject.empty()
            .put("errorCounts", objectMapper.writeValueAsString(RatelimitService.lastMinuteErrorCounts))
            .put("pathErrorCounts", objectMapper.writeValueAsString(RatelimitService.lastMinuteErrorCounts))

        context.call.respondJson(dataObject)
    }
}