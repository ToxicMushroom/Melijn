package me.melijn.melijnbot.internals.command

import dev.minn.jda.ktx.SLF4J
import me.melijn.melijnbot.internals.utils.capitalize
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData
import org.xml.sax.Attributes
import org.xml.sax.SAXException
import org.xml.sax.helpers.DefaultHandler
import java.io.InputStream
import java.util.*
import javax.xml.parsers.SAXParserFactory
import kotlin.reflect.KClass
import kotlin.reflect.KParameter.Kind.*
import kotlin.reflect.full.*
import kotlin.reflect.jvm.jvmErasure

class XMLCommandLoader private constructor() : DefaultHandler() {

    val commandCount: Int
        get() = commandStack.peek().subCommands.size

    private val commandStack = Stack<XMLCommand>().also {
        it.push(XMLCommand("", "", ""))
    }

    private val categoryStack = Stack<String>()
    private var data: String? = null

    override fun startElement(uri: String, localName: String, qName: String, attributes: Attributes) {
        fun missing(elementName: String, attributeName: String) =
            SAXException("$elementName element must have '$attributeName' attribute")

        when (qName) {
            "command" -> {
                val name = attributes.getValue("name") ?: throw missing("command", "name")
                val comment = attributes.getValue("comment") ?: throw missing("arg", "comment")
                val command = XMLCommand(name, comment, this.categoryStack.joinToString("."))

                commandStack.push(command)
            }
            "arg" -> {
                val name = attributes.getValue("name") ?: throw missing("arg", "name")
                val type = attributes.getValue("type") ?: throw missing("arg", "type")
                val comment = attributes.getValue("comment") ?: throw missing("arg", "comment")
                val required = attributes.getValue("required").toBoolean()
                val command = commandStack.peek()

                command.arguments += XMLArg(name, OptionType.valueOf(type.uppercase()), comment, required)
            }
            "category" -> {
                val name = attributes.getValue("name") ?: throw missing("category", "name")
                categoryStack.push(name)
            }
        }
    }

    override fun endElement(uri: String?, localName: String?, qName: String?) {
        when (qName) {
            "command" -> {
                val finishedCommand = commandStack.pop()

                // Sanity check: subcommands and arguments do not mix
                if (finishedCommand.arguments.isNotEmpty() && finishedCommand.subCommands.isNotEmpty())
                    throw SAXException("Command '${finishedCommand.name}' may not have both subcommands and arguments")

                commandStack.peek().subCommands += finishedCommand
            }
            "category" -> categoryStack.pop()
            "choice" -> (commandStack.peek().arguments.lastOrNull()
                ?: throw SAXException("<choice> block must appear within an <arg> block"))
                .choices.add(this.data ?: throw SAXException("choice block must contain a choice"))
        }
    }

    override fun characters(ch: CharArray?, start: Int, length: Int) {
        this.data = String(ch!!, start, length)
    }

    inline fun <reified T : CommandOptions> createExecutor(packageName: String): CommandExecutor<T, CommandFailure> =
        createExecutor(packageName, T::class)

    fun <T : CommandOptions> createExecutor(
        packageName: String,
        contextClazz: KClass<T>
    ): CommandExecutor<T, CommandFailure> {
        val commandMap = HashMap<String, CommandExecutor<T, CommandFailure>>()

        commandStack.peek().subCommands.forEach { command ->
            val name = command.name

            // The class names to try
            val base = if (command.category.isEmpty()) {
                "$packageName.${name.capitalize()}"
            } else {
                "$packageName.${command.category}.${name.capitalize()}"
            }
            val toTry = arrayOf(
                "${base}Command",
                "${base}SlashCommand",
                base
            )

            // Try to find the class, otherwise throw a (hopefully helpful) error
            val clazz = toTry.asSequence().map {
                try {
                    Class.forName(it)
                } catch (e: ClassNotFoundException) {
                    null
                }
            }.filterNotNull().firstOrNull()?.kotlin
                ?: throw RuntimeException(
                    "Could not find implementation class for command '$name'. " +
                            "Tried ${toTry.joinToString(" and ") { "'$it'" }}."
                )

            // Get the instance, fail if clazz is not an `object`
            val instance = clazz.objectInstance
                ?: throw RuntimeException("${clazz.qualifiedName} is not an object declaration")

            val executor = command.createExecutor(instance, clazz, contextClazz)
            commandMap[command.name] = executor
        }

        return object : CommandExecutor<T, CommandFailure> {
            override suspend fun execute(context: T): Result<CommandFailure> {
                return (commandMap[context.name] ?: return Result.failure(CommandFailure.DOES_NOT_EXIST))
                    .execute(context)
            }
        }
    }

    fun buildCommands(): List<CommandData> = this.commandStack.peek().subCommands.map {
        it.createCommandData()
    }

    companion object {
        fun load(stream: InputStream): XMLCommandLoader {
            val loader = XMLCommandLoader()
            SAXParserFactory.newInstance().newSAXParser().parse(stream, loader)

            return loader
        }
    }

    private data class XMLCommand(
        val name: String,
        val comment: String,
        val category: String,
        val subCommands: MutableList<XMLCommand> = mutableListOf(),
        val arguments: MutableList<XMLArg> = mutableListOf(),
    ) {
        private val logger by SLF4J

        fun <T : CommandOptions> createExecutor(
            instance: Any,
            clazz: KClass<*>,
            contextClazz: KClass<T>
        ): CommandExecutor<T, CommandFailure> {
            return if (subCommands.isNotEmpty()) {
                createSubcommandsExecutor(clazz, contextClazz)
            } else {
                createArgumentedExecutor(clazz, contextClazz, instance)
            }
        }

        private fun <T : CommandOptions> createSubcommandsExecutor(
            clazz: KClass<*>,
            contextClazz: KClass<T>
        ): CommandExecutor<T, CommandFailure> {
            val commandMap =
                subCommands.associateWith {
                    val name = it.name
                    val inner = clazz.nestedClasses.find { nested ->
                        nested.simpleName?.lowercase() == name.lowercase().replace("_", "")
                    }
                        ?: throw RuntimeException("Subcommand '$name' has no related inner class in $clazz")
                    // Get the instance, fail if clazz is not an `object`
                    val instance = inner.objectInstance
                        ?: throw RuntimeException("${clazz.qualifiedName} is not an object declaration")

                    it.createExecutor(instance, inner, contextClazz)
                }

            return object : CommandExecutor<T, CommandFailure> {
                override suspend fun execute(context: T): Result<CommandFailure> {
                    for ((command, executor) in commandMap.entries) {
                        val name = (if (command.subCommands.isNotEmpty()) context.subGroupName else context.subName)
                            ?: throw RuntimeException("Subcommand executor on context that is not a subcommand (group)")
                        if (command.name == name) {
                            executor.execute(context)
                            return Result.success()
                        }
                    }

                    return Result.failure(CommandFailure.DOES_NOT_EXIST)
                }
            }
        }

        private fun <T : CommandOptions> createArgumentedExecutor(
            clazz: KClass<*>,
            contextClazz: KClass<T>,
            instance: Any
        ): CommandExecutor<T, CommandFailure> {
            val executeFun = clazz.members.find { it.name == "execute" }
                ?: throw RuntimeException("${clazz.qualifiedName} has no `execute` function")
            if (!executeFun.isSuspend) throw RuntimeException("'$executeFun' must be suspend")

            val recParam = executeFun.extensionReceiverParameter?.also {
                if (it.type.jvmErasure != contextClazz)
                    throw RuntimeException("$it must be of type ${contextClazz.qualifiedName!!}.")
            }

            val valueParameters = executeFun.valueParameters
            val specialParams = executeFun.parameters.minus(valueParameters)

            val gottenSignature by lazy {
                valueParameters.joinToString(", ") { it.type.jvmErasure.qualifiedName ?: "UNKNOWN" }
            }
            val wantedSignature by lazy {
                (listOf(contextClazz.qualifiedName!!) + arguments.map {
                    "${it.name}: ${
                        it.type.name.lowercase().capitalize()
                    }"
                })
                    .joinToString(", ")
            }

            // First off, let's figure out what to do with the special parameters
            val specials = specialParams.map {
                it to when (it.kind) {
                    INSTANCE -> { _: T -> instance }
                    EXTENSION_RECEIVER -> (recParam ?: unreachable()).run { { ctx: T -> ctx } }
                    VALUE -> unreachable()
                }
            }

            // When the execute function is not an extending function (on the context), we expect the context as first parameter.
            val parOffset = if (recParam == null) 1 else 0

            if (valueParameters.size + parOffset < arguments.size) {
                logger.warn("'$executeFun' has too little arguments, but we can still proceed. Current signature: ($gottenSignature), wanted: ($wantedSignature).")
            }

            // Now, we match every value parameter to the command argument.
            val values = valueParameters.mapIndexed { idx, par ->
                // If this is not an extension function on the context type, then the first parameter should always be the context
                par to if (idx == 0 && recParam == null) {
                    if (par.type.isSupertypeOf(contextClazz.starProjectedType)) {
                        { ctx: T -> ctx }
                    } else throw RuntimeException("'$executeFun': first argument must be of type '${contextClazz.qualifiedName}'")
                } else {
                    val arg = arguments.getOrElse(idx - parOffset) {
                        throw RuntimeException("'$executeFun' has more parameters than the command! Current signature: ($gottenSignature), wanted: ($wantedSignature).")
                    }

                    // If `arg` is optional, the parameter must be nullable!
                    if (!arg.required && !par.type.isMarkedNullable) throw RuntimeException("$par is linked to the optional argument `${arg.name}`, thus, it must be nullable, but it is not.");

                    { ctx: T -> ctx.getOption(arg.name, par.type.jvmErasure) }
                }
            }

            val execMap = (specials + values).toMap()

            return object : CommandExecutor<T, CommandFailure> {
                override suspend fun execute(context: T): Result<CommandFailure> {
                    executeFun.callSuspendBy(execMap.mapValues { (_, exec) -> exec(context) })

                    return Result.success()
                }
            }
        }

        fun createCommandData(): CommandData {
            fun createOptions(arguments: MutableList<XMLArg>) =
                arguments.map {
                    OptionData(it.type, it.name, it.comment).setRequired(it.required).also { data ->
                        if (it.choices.isNotEmpty()) {
                            when (it.type) {
                                OptionType.INTEGER -> it.choices.forEach { choice ->
                                    data.addChoice(
                                        choice,
                                        choice.toInt()
                                    )
                                }
                                OptionType.STRING -> it.choices.forEach { choice -> data.addChoice(choice, choice) }
                                else -> throw SAXException("Only INTEGER and STRING types may have choices")
                            }
                        }
                    }
                }

            fun SubcommandData.addOption(option: OptionData) =
                this.addOption(option.type, option.name, option.description, option.isRequired)

            fun CommandData.addOption(option: OptionData) =
                this.addOption(option.type, option.name, option.description, option.isRequired)

            val rootCommandData = CommandData(name, comment)
            if (subCommands.isNotEmpty()) {
                subCommands.forEach { subCommand ->
                    if (subCommand.subCommands.isNotEmpty()) {
                        // `subCommand` is a subcommand group
                        val groupData = SubcommandGroupData(subCommand.name, subCommand.comment)
                        subCommand.subCommands.forEach { subSubCommand ->
                            // subcommands in a group can not be groups
                            groupData.addSubcommands(
                                SubcommandData(
                                    subSubCommand.name,
                                    subSubCommand.comment
                                ).also { ssData ->
                                    createOptions(subSubCommand.arguments).forEach { ssData.addOption(it) }
                                })
                        }
                        rootCommandData.addSubcommandGroups(groupData)
                    } else {
                        // `subCommand` is not a subcommand group
                        rootCommandData.addSubcommands(
                            SubcommandData(
                                subCommand.name,
                                subCommand.comment
                            ).also { sData ->
                                createOptions(subCommand.arguments).forEach { sData.addOption(it) }
                            })
                    }
                }
            } else {
                createOptions(arguments).forEach { rootCommandData.addOption(it) }
            }

            return rootCommandData
        }
    }

    private data class XMLArg(
        val name: String,
        val type: OptionType,
        val comment: String,
        val required: Boolean,
        val choices: MutableList<String> = mutableListOf()
    )

}

@Suppress("NOTHING_TO_INLINE")
private inline fun unreachable(): Nothing = throw NotImplementedError()