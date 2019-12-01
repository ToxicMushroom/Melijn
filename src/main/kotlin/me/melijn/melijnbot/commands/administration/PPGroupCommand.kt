package me.melijn.melijnbot.commands.administration

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.utils.sendSyntax

class PPGroupCommand : AbstractCommand("command.ppgroup") {

    init {
        id = 127
        name = "ppgroup"
        aliases = arrayOf("punishmentPointsGroup", "ppg")
        children = arrayOf(
            AddArg(root),
            SelectArg(root),
            SetPPTriggerArg(root),
            SetPPGoalArg(root),
            CopyArg(root)
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