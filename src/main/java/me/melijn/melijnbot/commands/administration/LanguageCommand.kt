package me.melijn.melijnbot.commands.administration


import me.melijn.melijnbot.enums.Language
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.Translateable
import me.melijn.melijnbot.objects.utils.replacePrefix
import me.melijn.melijnbot.objects.utils.sendMsg
import me.melijn.melijnbot.objects.utils.sendMsgCodeBlock
import me.melijn.melijnbot.objects.utils.sendSyntax

class LanguageCommand : AbstractCommand() {

    init {
        id = 2
        name = "language"
        aliases = arrayOf("lang")
        description = Translateable("command.language.description")
        commandCategory = CommandCategory.ADMINISTRATION
        children = arrayOf(ListCommand(), SetCommand())
    }

    override fun execute(context: CommandContext) {
        sendMsg(context, syntax.replacePrefix(context))
    }


    /** SUBCOMMAND list **/
    class ListCommand : AbstractCommand() {

        init {
            name = "list"
            description = Translateable("command.language.list.description")
        }

        override fun execute(context: CommandContext) {
            sendMsgCodeBlock(context, replaceLangList(
                    Translateable("command.language.list.response1").string(context)
            ), "INI")
        }

        private fun replaceLangList(string: String): String {
            val sb = StringBuilder()
            var i = 1
            for (value in Language.values()) {
                sb.append(i++).append(" - [").append(value).append("]").append("\n")
            }
            return string.replace("%languageList%", sb.toString())
        }
    }


    /** SUBCOMMAND set **/
    class SetCommand : AbstractCommand() {

        init {
            name = "set"
            description = Translateable("command.language.set.description")
        }

        override fun execute(context: CommandContext) {
            if (context.commandParts.size != 3) {
                sendSyntax(this, context, "command.language.set.arguments")
                return
            }

            val lang: String
            try {
                lang = Language.valueOf(context.commandParts[2].toUpperCase()).toString()
            } catch (ignored: IllegalArgumentException) {
                sendMsg(context,
                        replaceArg(
                                Translateable("command.language.set.invalid").string(context),
                                context.commandParts[2]
                        )
                )
                return
            }


            val dao = context.daoManager.guildLanguageWrapper

            context.taskManager.async(Runnable {
                dao.setLanguage(context.guildId, lang)
            })

            sendMsg(context,
                    replaceLang(Translateable("command.language.set.success").string(context), lang)
            )
        }

        private fun replaceArg(msg: String, argument: String): String {
            return msg.replace("%argument%", argument)
        }

        private fun replaceLang(msg: String, lang: String): String {
            return msg.replace("%language%", lang)
        }
    }
}