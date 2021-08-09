package me.melijn.melijnbot.internals.web.rest.commands

import io.ktor.http.*
import io.ktor.response.*
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.RunCondition
import me.melijn.melijnbot.internals.translation.i18n
import me.melijn.melijnbot.internals.utils.message.getSyntax
import me.melijn.melijnbot.internals.web.RequestContext
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.utils.data.DataArray
import net.dv8tion.jda.api.utils.data.DataObject

object FullCommandsResponseHandler {

    var cmds = ""

    suspend fun handleFullCommandsResponse(context: RequestContext) {
        if (cmds.isEmpty()) {
            val dataObject = DataObject.empty()
            val discordPermissionList = mutableListOf<Permission>()
            for (root in context.container.commandSet) {
                if (root.commandCategory == CommandCategory.DEVELOPER) continue
                val dataArray = if (dataObject.hasKey(root.commandCategory.toString())) {
                    dataObject.getArray(root.commandCategory.toString())
                } else {
                    DataArray.empty()
                }
                val darr = getDataArrayArrayFrom(arrayOf(root), discordPermissionList).getArray(0)
                dataArray.add(darr)
                dataObject.put(root.commandCategory.toString(), dataArray)
            }

            val extra = DataObject.empty()
            val runConditions = DataArray.empty()
            for ((_, condition) in RunCondition.values().withIndex().sortedBy { it.index }) {
                val titlePath = "runcondition." + condition.toString().lowercase().replace("_", ".") + ".title"
                val descPath = "runcondition." + condition.toString().lowercase().replace("_", ".") + ".description"
                val conditionInfo = DataArray.empty()
                    .add(i18n.getTranslation("en", titlePath))
                    .add(i18n.getTranslation("en", descPath))
                runConditions.add(conditionInfo)
            }
            val names = discordPermissionList.map { it.getName() }
            val permArr = DataArray.fromCollection(names)
            extra.put("runconditions", runConditions)
            extra.put("discordpermissions", permArr)
            dataObject.put("extra", extra)
            cmds = dataObject.toString()
        }

        context.call.respondText(cmds, ContentType.Application.Json)
    }

    private val argRegex = "%help\\.arg\\..*?%".toRegex()
    private fun getDataArrayArrayFrom(
        children: Array<AbstractCommand>,
        discordPermissionList: MutableList<Permission>,
        root: AbstractCommand = children.first()
    ): DataArray {
        val dataArray = DataArray.empty()
        val permissionToIndex: (Permission) -> Int = {
            val index = discordPermissionList.indexOf(it)
            if (index == -1) {
                discordPermissionList.add(it)
                discordPermissionList.size - 1
            } else index
        }

        for (c in children) {
            val innerDataArray = DataArray.empty()

            innerDataArray.add(c.name) // 0
            innerDataArray.add(i18n.getTranslation("en", c.description)) // 1
            innerDataArray.add(getSyntax("en", c.syntax)) // 2
            innerDataArray.add(DataArray.fromCollection(c.aliases.toList())) // 3
            val argumentsHelp = i18n.getTranslationN("en", c.arguments, false)

            innerDataArray.add(argumentsHelp?.replace(argRegex) {
                it.groups[0]?.let { (value) ->
                    i18n.getTranslation("en", value.substring(1, value.length - 1))
                } ?: "report to devs it's BROKEN :c"
            } ?: "") // 4

            innerDataArray.add(DataArray.fromCollection(c.discordChannelPermissions.map(permissionToIndex))) // 5
            innerDataArray.add(DataArray.fromCollection(c.discordPermissions.map(permissionToIndex))) // 6
            val listOfConditions = c.runConditions.map { RunCondition.values().indexOf(it) }
            innerDataArray.add(DataArray.fromCollection(listOfConditions)) // 7
            innerDataArray.add(getDataArrayArrayFrom(c.children, discordPermissionList, root)) // 8
            innerDataArray.add(i18n.getTranslationN("en", c.help, false) ?: "") // 9
            innerDataArray.add(i18n.getTranslationN("en", c.examples, false) ?: "") // 10
            dataArray.add(innerDataArray)
        }
        return dataArray
    }
}