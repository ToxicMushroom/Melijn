package me.melijn.melijnbot.commands.developer

import kotlinx.coroutines.Deferred
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.utils.message.sendRsp
import org.jetbrains.kotlin.script.jsr223.KotlinJsr223JvmLocalScriptEngine
import javax.script.ScriptEngine
import javax.script.ScriptEngineManager

class EvalCommand : AbstractCommand("command.eval") {

    init {
        id = 22
        name = "eval"
        aliases = arrayOf("evaluate")
        commandCategory = CommandCategory.DEVELOPER
    }

    private val engine: ScriptEngine? = ScriptEngineManager().getEngineByName("kotlin")

    override suspend fun execute(context: ICommandContext) {
        requireNotNull(engine)
        var code = context.rawArg.removePrefix("```kt\n").removePrefix("```").removeSuffix("```").trim()
        val imports = code.lines().takeWhile { it.startsWith("import ") || it.startsWith("\nimport ") }
        code = """
			import me.melijn.melijnbot.internals.utils.*
			import me.melijn.melijnbot.internals.threading.*
			import me.melijn.melijnbot.internals.command.*
            import me.melijn.melijnbot.internals.utils.message.sendRsp
			import me.melijn.melijnbot.internals.*
			import me.melijn.melijnbot.MelijnBot
			import java.awt.image.BufferedImage
            import kotlinx.coroutines.Deferred
			import java.io.File
			import javax.imageio.ImageIO
			import kotlinx.coroutines.*
			${imports.joinToString("\n\t\t\t")}
			fun exec(context: ICommandContext): Deferred<Any?> {
                return TaskManager.evalTaskValueNAsync {
				    ${
            code.lines().dropWhile { imports.contains(it) }
                .joinToString("\n\t\t\t\t\t")
        }
                }
            }""".trimIndent()



        try {
            engine.eval(code)
            val se = engine as KotlinJsr223JvmLocalScriptEngine
            val resp: Deferred<Any?> = se.invokeFunction("exec", context) as Deferred<Any?>
            sendRsp(context, resp.await().toString())

        } catch (t: Throwable) {
            sendRsp(context, "ERROR:\n```${t.message}```")
        }
    }
}