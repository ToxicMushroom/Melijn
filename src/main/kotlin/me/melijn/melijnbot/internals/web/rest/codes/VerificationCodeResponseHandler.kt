package me.melijn.melijnbot.internals.web.rest.codes

import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import me.melijn.melijnbot.internals.web.RequestContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object VerificationCodeResponseHandler {

    val logger: Logger = LoggerFactory.getLogger(VerificationCodeResponseHandler::class.java)

    suspend fun handleVerificationCodes(context: RequestContext) {
        if (context.call.request.header("Authorization") != context.restToken) {
            context.call.respondText(status = HttpStatusCode.Forbidden) { "bruh" }
            return
        }

        val body = context.call.receiveText().toLongOrNull() ?: return
        logger.info(body.toString())

//        context.container.daoManager.unverifiedUsersWrapper.getMoment()
//
//        val userId = body.getString("user", null)?.toLongOrNull()
//        if (userId == null) {
//            context.call.respondText(status = HttpStatusCode.BadRequest) { "please provide the user as string in json body" }
//            return
//        }
    }

}