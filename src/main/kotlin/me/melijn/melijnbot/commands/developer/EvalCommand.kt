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
        private val evalImports = """
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


        suspend fun runCode(code: String, context: ICommandContext? = null): String {
            val global = context == null
            if (engine == null) return "ScriptEngine is null"
            var code1 = code
            val imports = code1.lines().takeWhile { it.startsWith("import ") || it.startsWith("\nimport ") }
            val script = code1.lines().dropWhile { imports.contains(it) }
                .joinToString("\n\t\t\t\t\t")
                .replace("return ", "return@evalTaskValueNAsync ")
            val globalVars = if (global) {
                """
                    val shardManager = MelijnBot.shardManager
                    val container = Container.instance
                """.trimIndent()
            } else ""
            val functionName = "exec"
            val functionDefinition =
                "fun $functionName(${if (global) "" else "context: ICommandContext"}): Deferred<Pair<Any?, String>> {"
            code1 = """$evalImports
                ${imports.joinToString("\n\t\t\t")}
                $functionDefinition
                    $globalVars
                    return TaskManager.evalTaskValueNAsync {
                        $script
                    }
                }""".trimIndent()
            return try {
                engine.eval(code1)
                val se = engine as KotlinJsr223JvmLocalScriptEngine

                val resp =
                    (if (global) se.invokeFunction(functionName) else se.invokeFunction(functionName, context))
                            as Deferred<Pair<Any?, String>>

                val (result, error) = resp.await()
                result?.toString() ?: "ERROR:\n```${error}```"
            } catch (t: Throwable) {
                "ERROR:\n```${t.message}```"
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
                if (podId == PodInfo.podId) {
                    sb.appendLine(runCode(code))
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
            val result = runCode(code, context)
            sendRsp(context, result)
        }
    }
}