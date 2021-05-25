package me.melijn.melijnbot.internals.web.rest.commands

import io.ktor.http.*
import io.ktor.response.*
import me.melijn.melijnbot.internals.web.RequestContext
import me.melijn.melijnbot.objectMapper

object CommandMapResponseHandler {

    var cachedMap = emptyMap<Int, String>()
    suspend fun handleCommandMapResponse(context: RequestContext) {
        if (cachedMap.isNotEmpty()) {
            context.call.respondText(objectMapper.writeValueAsString(cachedMap), contentType = ContentType.Application.Json)
        } else {
            val map = context.container.commandSet.associate { it.id to it.name }
            cachedMap = map
            context.call.respondText(objectMapper.writeValueAsString(cachedMap), contentType = ContentType.Application.Json)
        }
    }
}