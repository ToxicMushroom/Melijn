package me.melijn.melijnbot.commands.developer

//import org.jetbrains.kotlin.cli.common.environment.setIdeaIoUseFallback
import groovy.lang.GroovyShell
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.utils.sendMsg

class EvalCommand : AbstractCommand("command.eval") {

    val groovyShell: GroovyShell

    init {
        id = 22
        name = "eval"
        aliases = arrayOf("evaluate")
        commandCategory = CommandCategory.DEVELOPER
        groovyShell = GroovyShell()
    }


    override suspend fun execute(context: CommandContext) {
        groovyShell.setProperty("context", context)

        try {
            groovyShell.evaluate(context.rawArg)
        } catch (t: Throwable) {
            sendMsg(context, "ERROR:\n```" + t.message + "```")
        }
    }


    class KotlinArg(parent: String) : AbstractCommand("$parent.kotlin") {


        init {
            name = "kotlin"
            aliases = arrayOf("k", "kt", "kts")
//            setIdeaIoUseFallback()
        }

        //        private val seManager = ScriptEngineManager()
//        private val engine: ScriptEngine = seManager.getEngineByExtension("kts")
        override suspend fun execute(context: CommandContext) {

//            val bindings = engine.createBindings()
//            val binds: List<Triple<String, Any, String>> = mutableListOf(
//                Triple("context", context, "me.melijn.melijnbot.objects.command.CommandContext"),
//                Triple("kotlinArg", this, "me.melijn.melijnbot.commands.developer.EvalCommand.KotlinArg"),
//                Triple("eb", Embedder(context), "me.melijn.melijnbot.objects.embed.Embedder")
//            )
//
//            for ((name, any, _) in binds)
//                bindings[name] = any
//
//            try {
//                var preScript = ""
//
//                for ((name, _, path) in binds)
//                    preScript += "val $name = bindings[\"$name\"] as $path\n"
//
//                val result = engine.eval(preScript + context.rawArg, bindings)
//
//                val eb = Embedder(context)
//                eb.setDescription("" +
//                    "**Eval Input**:\n```kotlin\n" + context.rawArg + "```\n" +
//                    "**Eval Output**:\n```${result?.toString()}```"
//                )
//                sendEmbed(context, eb.build())
//            } catch (t: Throwable) {
//                sendMsg(context, "Eval Error:\n```${t.message ?: "empty"}```")
//            }
//        }
            sendMsg(context, "Not implemented")
        }

//    class JavaEvalCommand(root: String) : AbstractCommand("$root.java") {
//
//        private val className = "EvalTempClass"
//
//        init {
//            name = "java"
//        }
//
//        override suspend fun execute(context: CommandContext) {
//            try {
//                evaluate(context.rawArg, context)
//            } catch (e: Exception) {
//                sendMsg(context, "```" + e.message + "```")
//            }
//        }
//
//        private fun evaluate(source: String, event: CommandContext) {
//            val compiler = CompilerFactoryFactory.getDefaultCompilerFactory().newSimpleCompiler()
//            compiler.cook(createDummyClassSource(source))
//            evaluateDummyClassMethod(event, compiler.classLoader)
//        }

//        private fun createDummyClassSource(source: String): String {
//            return "" +
//                "import me.melijn.melijnbot.objects.command.CommandContext;\n" +
//                "import java.io.*;\n" +
//                "import java.lang.*;\n" +
//                "import java.util.*;\n" +
//                "import java.util.concurrent.*;\n" +
//                "import net.dv8tion.jda.core.*;\n" +
//                "import net.dv8tion.jda.core.entities.*;\n" +
//                "import net.dv8tion.jda.core.entities.impl.*;\n" +
//                "import net.dv8tion.jda.core.managers.*;\n" +
//                "import net.dv8tion.jda.core.managers.impl.*;\n" +
//                "import net.dv8tion.jda.core.utils.*;\n" +
//                "import java.util.regex.*;\n" +
//                "import java.awt.*;\n" +
//                "class " + className + " {\n" +
//                "   public static void eval(final CommandContext context) {\n" +
//                "       " + source + "\n" +
//                "   }\n" +
//                "}\n"
//        }
//
//        private fun evaluateDummyClassMethod(context: CommandContext, classLoader: ClassLoader) {
//            val dummy = classLoader.loadClass(className)
//            val eval = dummy.getDeclaredMethod("eval", CommandContext::class.java)
//            eval.isAccessible = true
//            eval.invoke(null, context)
//        }
    }
}