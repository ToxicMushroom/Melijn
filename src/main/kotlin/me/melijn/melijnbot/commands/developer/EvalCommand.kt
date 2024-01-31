package me.melijn.melijnbot.commands.developer

import io.ktor.client.request.*
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.models.PodInfo
import me.melijn.melijnbot.internals.script.evalCode
import me.melijn.melijnbot.internals.script.evalCodeNoContext
import me.melijn.melijnbot.internals.utils.escapeMarkdown
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.message.sendRspAwaitEL
import me.melijn.melijnbot.internals.utils.message.sendRspCodeBlock
import me.melijn.melijnbot.internals.utils.replace
import kotlin.script.experimental.api.valueOr


class EvalCommand : AbstractCommand("command.eval") {

    init {
        id = 22
        name = "eval"
        aliases = arrayOf("evaluate", "globalEval")
        commandCategory = CommandCategory.DEVELOPER
    }

    companion object {
        var paginateGE = true
        private val evalImports = """
                import me.melijn.melijnbot.internals.utils.*
                import me.melijn.melijnbot.internals.threading.*
                import me.melijn.melijnbot.internals.command.*
                import me.melijn.melijnbot.internals.utils.message.sendRsp
                import me.melijn.melijnbot.internals.*
                import me.melijn.melijnbot.MelijnBot
                import me.melijn.melijnbot.Container
                import net.dv8tion.jda.api.entities.*
                import kotlinx.coroutines.Deferred
                import kotlinx.coroutines.*""".trimIndent()


        fun runCode(code: String, receiver: ICommandContext? = null, vararg props: Pair<String, Any?>): String {
            val codeWithImports = evalImports + "\n" + code
            return try {
                val results = if (receiver != null)
                    evalCode(codeWithImports, receiver, props.toMap())
                else evalCodeNoContext(codeWithImports, props.toMap())
                return results.also {
                    println(it.reports.joinToString("\n") {
                        it.exception?.printStackTrace()
                        it.message
                    })
                }.valueOr {
                    return "ERROR:\n```${it.reports.joinToString("\n") {
                        it.exception?.printStackTrace()
                        it.message
                    }}```"
                }.returnValue.toString()
            } catch (t: Throwable) {
                t.printStackTrace()
                "ERROR:\n```${t.message}```"
            }
        }
    }


    override suspend fun execute(context: ICommandContext) {
        val code = context.rawArg.removePrefix("```kt\n").removePrefix("```").removeSuffix("```").trim()
        if (context.commandParts[1].equals("globaleval", true)) {
            val msg = sendRspAwaitEL(context, "processing..").first()
            val sb = StringBuilder()
            for (podId in 0 until PodInfo.podCount) {
                sb.append("[Pod-${podId}]: ")
                if (podId == PodInfo.podId) {
                    sb.appendLine(runCode(code))
                } else {
                    val hostPattern = context.container.settings.botInfo.hostPattern
                    val url = hostPattern.replace("{podId}", podId) + "/eval"
                    val response = try {
                        context.webManager.httpClient.post(url) {
                            header("Authorization", context.container.settings.restServer.token)
                            body = code
                        }
                    } catch (t: Throwable) {
                        "err: ${t.message}"
                    }
                    sb.appendLine(response)
                }
            }
            msg.delete().queue()
            sendRspCodeBlock(context, "```INI\n${sb.toString().escapeMarkdown()}```", "INI", paginateGE)
        } else {
            val result = runCode(code, context)
            sendRsp(context, result)
        }
    }
}