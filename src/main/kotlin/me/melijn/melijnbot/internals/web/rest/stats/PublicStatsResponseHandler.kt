package me.melijn.melijnbot.internals.web.rest.stats

import me.melijn.melijnbot.internals.web.RequestContext
import me.melijn.melijnbot.internals.web.WebUtils.respondJson

object PublicStatsResponseHandler {

    suspend fun handlePublicStatsResponse(context: RequestContext) {
        val dataObject = computeBaseObject()
            .put("shards", computePublicStatsObject(context))
        context.call.respondJson(dataObject)
    }
}