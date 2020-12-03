package me.melijn.melijnbot.commands.developer

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext
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

    val engine: ScriptEngine? = ScriptEngineManager().getEngineByName("kotlin")

    override suspend fun execute(context: CommandContext) {
        requireNotNull(engine)
        var code = context.rawArg.removePrefix("```kt").removePrefix("```").removeSuffix("```")
        val imports = code.lines().takeWhile { it.startsWith("import ") || it.startsWith("\nimport ") }
        code = """
			import me.melijn.melijnbot.internals.utils.*
			import me.melijn.melijnbot.internals.threading.*
			import me.melijn.melijnbot.internals.command.*
            import me.melijn.melijnbot.internals.utils.message.sendRsp
			import me.melijn.melijnbot.internals.*
			import me.melijn.melijnbot.MelijnBot
			import java.awt.image.BufferedImage
			import java.io.File
			import javax.imageio.ImageIO
			import kotlinx.coroutines.*
			${imports.joinToString("\n\t\t\t")}
			fun exec(context: CommandContext) {
                TaskManager.async {
				    ${code.lines().dropWhile { it.startsWith("import ") || it.startsWith("\nimport ") }.joinToString("\n\t\t\t\t\t")}
                }
            }""".trimIndent()


        try {
            engine.eval(code)
            val se = engine as KotlinJsr223JvmLocalScriptEngine
            se.invokeFunction("exec", context)
            sendRsp(context, "Invoked")

        } catch (t: Throwable) {
            sendRsp(context, "ERROR:\n```${t.message}```")
        }
    }
}