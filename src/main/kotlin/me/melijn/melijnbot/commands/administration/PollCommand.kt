package me.melijn.melijnbot.commands.administration

//class PollCommand : AbstractCommand("command.poll") {
//
//    init {
//        id = 126
//        name = "poll"
//        children = arrayOf(
//            AddArg(root),
//            AddTimedArg(root),
//            RemoveArg(root),
//            ListArg(root),
//            CloseArg(root)
//        )
//        commandCategory = CommandCategory.ADMINISTRATION
//    }
//
//    class AddTimedArg(parent: String) : AbstractCommand("$parent.addtimed") {
//
//        init {
//            name = "addTimed"
//        }
//
//        override suspend fun execute(context: CommandContext) {
//            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//        }
//    }
//
//    class AddArg(parent: String) : AbstractCommand("$parent.add") {
//
//        init {
//            name = "add"
//            aliases = arrayOf("start")
//        }
//
//        //>poll add [textChannel] <content*>
//        override suspend fun execute(context: CommandContext) {
//
//        }
//    }
//
//    override suspend fun execute(context: CommandContext) {
//        sendSyntax(context)
//    }
//}