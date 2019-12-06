package me.melijn.melijnbot.commands.administration

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.utils.sendSyntax

class PunishmentPointGroupCommand : AbstractCommand("command.punishmentpointgroup") {

    init {
        id = 124
        name = "punishmentPointGroup"
        aliases = arrayOf("ppg", "PPGroup", "punishmentPG", "punishPG")
        children = arrayOf(
            AddArg(root),
            RemoveArg(root),
            SelectArg(root),
            ListArg(root),
            SetPPTriggerArg(root)
        )
        commandCategory = CommandCategory.DEVELOPER
    }


    override suspend fun execute(context: CommandContext) {
        sendSyntax(context)
    }

    class AddArg(parent: String) : AbstractCommand("$parent.add") {

        init {
            name = "add"
        }

        override suspend fun execute(context: CommandContext) {
            val wrapper = context.daoManager.autoPunishmentGroupWrapper
            //wrapper.set()
        }
    }

    class RemoveArg(parent: String) : AbstractCommand("$parent.arg") {

        init {
            name = "remove"
        }

        override suspend fun execute(context: CommandContext) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }

    class ListArg(parent: String) : AbstractCommand("$parent.list") {

        init {
            name = "list"
        }

        override suspend fun execute(context: CommandContext) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

    }

    class SelectArg(parent: String) : AbstractCommand("$parent.add") {

        init {
            name = "add"
        }

        override suspend fun execute(context: CommandContext) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }

    class SetPPTriggerArg(parent: String) : AbstractCommand("$parent.add") {

        init {
            name = "add"
        }

        override suspend fun execute(context: CommandContext) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }

    class SetPPGoalArg(parent: String) : AbstractCommand("$parent.add") {

        init {
            name = "add"
        }

        override suspend fun execute(context: CommandContext) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }


    class CopyArg(parent: String) : AbstractCommand("$parent.add") {

        init {
            name = "add"
        }

        override suspend fun execute(context: CommandContext) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }
}