package me.melijn.melijnbot.commands.administration

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.database.command.CustomCommand
import me.melijn.melijnbot.database.message.ModularMessage
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.i18n
import me.melijn.melijnbot.objects.utils.getLongFromArgNMessage
import me.melijn.melijnbot.objects.utils.sendMsg
import me.melijn.melijnbot.objects.utils.sendMsgCodeBlock
import me.melijn.melijnbot.objects.utils.sendSyntax
import java.util.regex.Pattern

class CustomCommandCommand : AbstractCommand("command.customcommand") {

    init {
        id = 36
        name = "customCommand"
        aliases = arrayOf("cc")
        children = arrayOf(
            ListArg(root),
            AddArg(root),
            AliasesArg(root),
            RemoveArg(root),
            SelectArg(root),
            SetChanceArg(root),
            SetPrefixStateArg(root),
            DisableArg(root),
            EnableArg(root)
        )
    }

    companion object {
        val selectionMap = HashMap<Pair<Long, Long>, Long>()
    }

    override suspend fun execute(context: CommandContext) {
        sendSyntax(context, syntax)
    }

    class ListArg(root: String) : AbstractCommand("$root.list") {

        init {
            name = "list"
        }

        override suspend fun execute(context: CommandContext) {
            val language = context.getLanguage()
            val title = i18n.getTranslation(language, "$root.title")

            val ccs = context.daoManager.customCommandWrapper.customCommandCache.get(context.getGuildId()).await()
            var content = "INI```"

            content += "\nid - name - chance - enabled"
            for (cc in ccs) {
                content += "\n[${cc.key}] - ${cc.value.name} - ${cc.value.chance} - ${cc.value.enabled}"
            }
            content += "```"

            val msg = title + content
            sendMsgCodeBlock(context, msg, "INI")
        }
    }

    class AddArg(root: String) : AbstractCommand("$root.add") {

        init {
            name = "add"
        }

        override suspend fun execute(context: CommandContext) {
            if (context.args.size < 2) {
                sendSyntax(context, syntax)
                return
            }

            val name = context.args[0]
            val content = context.rawArg.replaceFirst(("${Pattern.quote(name)}\\s+").toRegex(), "")
            val cc = CustomCommand(name, ModularMessage(content))

            context.daoManager.customCommandWrapper.add(context.getGuildId(), cc)

            val language = context.getLanguage()
            val msg = i18n.getTranslation(language, "$root.success")
                .replace("%name%", name)
                .replace("%content%", content)
            sendMsg(context, msg)
        }

    }

    class RemoveArg(root: String) : AbstractCommand("$root.remove") {

        init {
            name = "remove"
            aliases = arrayOf("r", "rm")
        }

        override suspend fun execute(context: CommandContext) {
            if (context.args.isEmpty()) {
                sendSyntax(context, syntax)
                return
            }

            val id = getLongFromArgNMessage(context, 0) ?: return

            context.daoManager.customCommandWrapper.remove(id)

            val language = context.getLanguage()
            val msg = i18n.getTranslation(language, "$root.success")
                .replace("%id%", id.toString())

            sendMsg(context, msg)
        }

    }

    class SelectArg(root: String) : AbstractCommand("$root.select") {

        init {
            name = "select"
            aliases = arrayOf("s")
        }

        override suspend fun execute(context: CommandContext) {
            if (context.args.isEmpty()) {
                sendSyntax(context, syntax)
                return
            }

            val id = getLongFromArgNMessage(context, 0) ?: return
            selectionMap[Pair(context.getGuildId(), context.authorId)] = id

            val language = context.getLanguage()
            val msg = i18n.getTranslation(language, "$root.selected")
                .replace("%id%", id.toString())
            sendMsg(context, msg)
        }
    }


    class AliasesArg(root: String) : AbstractCommand("$root.aliases") {

        init {
            name = "aliases"
            children = arrayOf(AddArg(root), RemoveArg(root), ListArg(root))
        }

        override suspend fun execute(context: CommandContext) {
            sendSyntax(context, syntax)
        }

        class AddArg(root: String) : AbstractCommand("$root.add") {

            init {
                name = "add"
            }

            override suspend fun execute(context: CommandContext) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

        }

        class RemoveArg(root: String) : AbstractCommand("$root.remove") {

            init {
                name = "remove"
            }

            override suspend fun execute(context: CommandContext) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

        }

        class ListArg(root: String) : AbstractCommand("$root.list") {

            init {
                name = "list"
            }

            override suspend fun execute(context: CommandContext) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

        }

    }

    class SetDescriptionArg(root: String) : AbstractCommand("$root.setdescription") {

        init {
            name = "setdescription"
        }

        override suspend fun execute(context: CommandContext) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

    }

    class SetChanceArg(root: String) : AbstractCommand("$root.setchance") {

        init {
            name = "setChance"
            aliases = arrayOf("sc")
        }

        override suspend fun execute(context: CommandContext) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

    }

    class SetPrefixStateArg(root: String) : AbstractCommand("$root.setprefixstate") {

        init {
            name = "setPrefixState"
            aliases = arrayOf("sps")
        }

        override suspend fun execute(context: CommandContext) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

    }


    class DisableArg(root: String) : AbstractCommand("$root.disable") {

        init {
            name = "disable"
        }

        override suspend fun execute(context: CommandContext) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

    }


    class EnableArg(root: String) : AbstractCommand("$root.enable") {

        init {
            name = "enable"
        }

        override suspend fun execute(context: CommandContext) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

    }
}