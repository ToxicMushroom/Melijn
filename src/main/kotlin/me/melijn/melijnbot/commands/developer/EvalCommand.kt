package me.melijn.melijnbot.commands.developer

import io.ktor.client.request.*
import kotlinx.coroutines.Deferred
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.models.PodInfo
import me.melijn.melijnbot.internals.utils.escapeMarkdown
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.message.sendRspCodeBlock
import me.melijn.melijnbot.internals.utils.replace
import org.jetbrains.kotlin.script.jsr223.KotlinJsr223JvmLocalScriptEngine
import javax.script.ScriptEngine
import javax.script.ScriptEngineManager

class EvalCommand : AbstractCommand("command.eval") {

    init {
        id = 22
        name = "eval"
        aliases = arrayOf("evaluate", "globalEval")
        commandCategory = CommandCategory.DEVELOPER
    }

    companion object {
        var paginateGE = true
        val engine: ScriptEngine? = ScriptEngineManager().getEngineByName("kotlin")
        val evalImports = """
                import me.melijn.melijnbot.internals.utils.*
                import me.melijn.melijnbot.internals.threading.*
                import me.melijn.melijnbot.internals.command.*
                import me.melijn.melijnbot.internals.utils.message.sendRsp
                import me.melijn.melijnbot.internals.*
                import me.melijn.melijnbot.MelijnBot
                import me.melijn.melijnbot.Container
                import net.dv8tion.jda.api.entities.*
                import java.awt.image.BufferedImage
                import kotlinx.coroutines.Deferred
                import java.io.File
                import javax.imageio.ImageIO
                import kotlinx.coroutines.*""".trimIndent()

        suspend fun evaluateGlobal(code: String): String {
            if (engine == null) return "ScriptEngine is null"

            var code1 = code
            val imports = code1.lines().takeWhile { it.startsWith("import ") || it.startsWith("\nimport ") }

            code1 = """$evalImports
                ${imports.joinToString("\n\t\t\t")}
                fun exec(): Deferred<Any?> {
                    val shardManager = MelijnBot.shardManager
                    val container = Container.instance
                    return TaskManager.evalTaskValueNAsync {
                        ${
                code1.lines().dropWhile { imports.contains(it) }
                    .joinToString("\n\t\t\t\t\t")
                    .replace("return ", "return@evalTaskValueNAsync ")
            }
                    }
                }""".trimIndent()

            try {
                engine.eval(code1)
                val se = engine as KotlinJsr223JvmLocalScriptEngine
                val resp: Deferred<Any?> = se.invokeFunction("exec") as Deferred<Any?>
                return resp.await().toString()

            } catch (t: Throwable) {
                return "ERROR:\n```${t.message}```"
            }
        }
    }


    override suspend fun execute(context: ICommandContext) {
        requireNotNull(engine)
        val code = context.rawArg.removePrefix("```kt\n").removePrefix("```").removeSuffix("```").trim()
        if (context.commandParts[1].equals("globaleval", true)) {
            val sb = StringBuilder()
            for (podId in 0 until PodInfo.podCount) {
                sb.append("[Pod-${podId}]: ")
                if (podId == PodInfo.podId){
                    sb.appendLine(evaluateGlobal(code))
                } else {
                    val hostPattern = context.container.settings.botInfo.hostPattern
                    val url = hostPattern.replace("{podId}", podId) + "/eval"
                    val response = context.webManager.httpClient.post<String>(url) {
                        header("Authorization", context.container.settings.restServer.token)
                        body = code
                    }
                    sb.appendLine(response)
                }
            }
            sendRspCodeBlock(context, "```INI\n${sb.toString().escapeMarkdown()}```", "INI", paginateGE)
        } else {
            val result = evaluateCommand(context, code)
            sendRsp(context, result)
        }
    }

    private suspend fun evaluateCommand(
        context: ICommandContext,
        code: String
    ): String {
        if (engine == null) return "ScriptEngine is null"
        var code1 = code
        val imports = code1.lines().takeWhile { it.startsWith("import ") || it.startsWith("\nimport ") }
        code1 = """$evalImports
                ${imports.joinToString("\n\t\t\t")}
                fun exec(context: ICommandContext): Deferred<Any?> {
                    return TaskManager.evalTaskValueNAsync {
                        ${
            code1.lines().dropWhile { imports.contains(it) }
                .joinToString("\n\t\t\t\t\t")
                .replace("return ", "return@evalTaskValueNAsync ")
        }
                    }
                }""".trimIndent()

        try {
            engine.eval(code1)
            val se = engine as KotlinJsr223JvmLocalScriptEngine
            val resp: Deferred<Any?> = se.invokeFunction("exec", context) as Deferred<Any?>
            return resp.await().toString()

        } catch (t: Throwable) {
            return "ERROR:\n```${t.message}```"
        }
    }
}