package me.melijn.melijnbot.commands.developer

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.utils.sendMsg
import me.melijn.melijnbot.objects.utils.sendSyntax
import org.codehaus.commons.compiler.CompilerFactoryFactory
//import org.jetbrains.kotlin.cli.common.environment.setIdeaIoUseFallback
//import org.jetbrains.kotlin.script.jsr223.KotlinJsr223JvmLocalScriptEngineFactory


class EvalCommand : AbstractCommand("command.eval") {

    init {
        id = 22
        name = "eval"
        aliases = arrayOf("evaluate")
        children = arrayOf(JavaEvalCommand(root))
        commandCategory = CommandCategory.DEVELOPER

    }

    override suspend fun execute(context: CommandContext) {
        sendSyntax(context, syntax)
    }

//    class KotlinEvalCommand(root: String) : AbstractCommand("$root.kotlin") {
//        init {
//            name = "kotlin"
//            setIdeaIoUseFallback()
//        }
//
//        override suspend fun execute(context: CommandContext) {
//            val engine = KotlinJsr223JvmLocalScriptEngineFactory().scriptEngine
//            context.reply(engine.eval("val x = 3"))
//        }
//    }

    class JavaEvalCommand(root: String) : AbstractCommand("$root.java") {

        private val className = "EvalTempClass"

        init {
            name = "java"
        }

        override suspend fun execute(context: CommandContext) {
            try {
                evaluate(context.rawArg, context)
            } catch (e: Exception) {
                sendMsg(context, "```" + e.message + "```")
            }
        }

        private fun evaluate(source: String, event: CommandContext) {
            val compiler = CompilerFactoryFactory.getDefaultCompilerFactory().newSimpleCompiler()
            compiler.cook(createDummyClassSource(source))
            evaluateDummyClassMethod(event, compiler.classLoader)
        }

        private fun createDummyClassSource(source: String): String {
            return "import me.melijn.melijnbot.objects.command.CommandContext;\n" +
                    "import java.io.*;\n" +
                    "import java.lang.*;\n" +
                    "import java.util.*;\n" +
                    "import java.util.concurrent.*;\n" +
                    "import net.dv8tion.jda.core.*;\n" +
                    "import net.dv8tion.jda.core.entities.*;\n" +
                    "import net.dv8tion.jda.core.entities.impl.*;\n" +
                    "import net.dv8tion.jda.core.managers.*;\n" +
                    "import net.dv8tion.jda.core.managers.impl.*;\n" +
                    "import net.dv8tion.jda.core.utils.*;\n" +
                    "import java.util.regex.*;\n" +
                    "import java.awt.*;\n" +
                    "class " + className + " {\n" +
                    "   public static void eval(final CommandContext context) {\n" +
                    "       " + source + "\n" +
                    "   }\n" +
                    "}\n"
        }

        private fun evaluateDummyClassMethod(context: CommandContext, classLoader: ClassLoader) {
            val dummy = classLoader.loadClass(className)
            val eval = dummy.getDeclaredMethod("eval", CommandContext::class.java)
            eval.isAccessible = true
            eval.invoke(null, context)
        }
    }
}