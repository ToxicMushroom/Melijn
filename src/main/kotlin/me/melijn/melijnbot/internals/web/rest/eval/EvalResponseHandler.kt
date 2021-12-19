package me.melijn.melijnbot.internals.web.rest.eval

import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import me.melijn.melijnbot.commands.developer.EvalCommand
import me.melijn.melijnbot.internals.web.RequestContext

object EvalResponseHandler {

    suspend fun handle(context: RequestContext) {
        if (context.call.request.header("Authorization") != context.restToken) {
            context.call.respondText(status = HttpStatusCode.Forbidden) { "Invalid token\n" }
            return
        }

        val code = context.call.receiveText()
        context.call.respondText(status = HttpStatusCode.OK) {
            EvalCommand.evaluateGlobal(code)
        }


    }
}