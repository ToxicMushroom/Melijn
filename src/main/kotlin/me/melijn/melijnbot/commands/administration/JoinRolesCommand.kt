package me.melijn.melijnbot.commands.administration

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.utils.sendSyntax

class JoinRolesCommand : AbstractCommand("command.joinroles") {

    init {
        id = 157
        name = "joinRoles"
        aliases = arrayOf("jr")
        children = arrayOf(
            AddArg(root),
            RemoveArg(root),
            GroupArg(root),
            RemoveAtArg(root),
            SetChanceArg(root),
            ListArg(root)
        )
        commandCategory = CommandCategory.ADMINISTRATION
    }

    class GroupArg(parent: String) : AbstractCommand("$parent.group") {

        init {
            name = "group"
            aliases = arrayOf("g", "gr", "gp")
            children = arrayOf(
                AddArg(root),
                RemoveArg(root),
                RemoveAtArg(root),
                SetEnabledArg(root),
                SetGetAllRolesArg(root),
                ListArg(root)
            )
        }

        class SetGetAllRolesArg(parent: String) : AbstractCommand("$parent.setgetallroles") {

            init {
                name = "setGetAllRoles"
                aliases = arrayOf("sgar")
            }

            override suspend fun execute(context: CommandContext) {
                TODO("Not yet implemented")
            }
        }

        class SetEnabledArg(parent: String) : AbstractCommand("$parent.setenabled") {

            init {
                name = "setEnabled"
                aliases = arrayOf("se")
            }

            override suspend fun execute(context: CommandContext) {
                TODO("Not yet implemented")
            }
        }

        class AddArg(parent: String) : AbstractCommand("$parent.add") {

            init {
                name = "add"
                aliases = arrayOf("a")
            }

            override suspend fun execute(context: CommandContext) {
                TODO("Not yet implemented")
            }
        }


        class RemoveArg(parent: String) : AbstractCommand("$parent.remove") {

            init {
                name = "remove"
                aliases = arrayOf("rm", "r")
            }

            override suspend fun execute(context: CommandContext) {
                TODO("Not yet implemented")
            }
        }


        class RemoveAtArg(parent: String) : AbstractCommand("$parent.removeat") {

            init {
                name = "removeAt"
                aliases = arrayOf("rma", "ra")
            }

            override suspend fun execute(context: CommandContext) {
                TODO("Not yet implemented")
            }
        }

        class ListArg(parent: String) : AbstractCommand("$parent.list") {

            init {
                name = "list"
                aliases = arrayOf("ls")
            }

            override suspend fun execute(context: CommandContext) {
                TODO("Not yet implemented")
            }
        }


        override suspend fun execute(context: CommandContext) {
            sendSyntax(context)
        }
    }


    class AddArg(parent: String) : AbstractCommand("$parent.add") {

        init {
            name = "add"
            aliases = arrayOf("a")
        }

        override suspend fun execute(context: CommandContext) {
            TODO("Not yet implemented")
        }
    }


    class RemoveArg(parent: String) : AbstractCommand("$parent.remove") {

        init {
            name = "remove"
            aliases = arrayOf("rm", "r")
        }

        override suspend fun execute(context: CommandContext) {
            TODO("Not yet implemented")
        }
    }


    class RemoveAtArg(parent: String) : AbstractCommand("$parent.removeat") {

        init {
            name = "removeAt"
            aliases = arrayOf("rma", "ra")
        }

        override suspend fun execute(context: CommandContext) {
            TODO("Not yet implemented")
        }
    }


    class SetChanceArg(parent: String) : AbstractCommand("$parent.setchance") {

        init {
            name = "setChance"
            aliases = arrayOf("sc")
        }

        override suspend fun execute(context: CommandContext) {
            TODO("Not yet implemented")
        }
    }


    class ListArg(parent: String) : AbstractCommand("$parent.list") {

        init {
            name = "list"
            aliases = arrayOf("ls")
        }

        override suspend fun execute(context: CommandContext) {
            TODO("Not yet implemented")
        }
    }

    override suspend fun execute(context: CommandContext) {
        sendSyntax(context)
    }
}