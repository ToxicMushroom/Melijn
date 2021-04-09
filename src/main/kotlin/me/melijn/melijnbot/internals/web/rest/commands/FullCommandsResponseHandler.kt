package me.melijn.melijnbot.internals.web.rest.commands

import io.ktor.http.*
import io.ktor.response.*
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.translation.i18n
import me.melijn.melijnbot.internals.utils.message.getSyntax
import me.melijn.melijnbot.internals.web.RequestContext
import net.dv8tion.jda.api.utils.data.DataArray
import net.dv8tion.jda.api.utils.data.DataObject

object FullCommandsResponseHandler {

    var cmds = ""


    suspend fun handleFullCommandsResponse(context: RequestContext) {
        if (cmds.isEmpty()) {
            val dataObject = DataObject.empty()
            for (root in context.container.commandSet) {
                if (root.commandCategory == CommandCategory.DEVELOPER) continue
                val dataArray = if (dataObject.hasKey(root.commandCategory.toString())) {
                    dataObject.getArray(root.commandCategory.toString())
                } else {
                    DataArray.empty()
                }
                val darr = getDataArrayArrayFrom(arrayOf(root)).getArray(0)
                dataArray.add(darr)
                dataObject.put(root.commandCategory.toString(), dataArray)
            }
            cmds = dataObject.toString()
        }

        context.call.respondText(cmds, ContentType.Application.Json)
    }

    private fun getDataArrayArrayFrom(children: Array<AbstractCommand>): DataArray {
        val dataArray = DataArray.empty()
        for (c in children) {
            val innerDataArray = DataArray.empty()

            innerDataArray.add(c.name) // 0
            innerDataArray.add(i18n.getTranslation("en", c.description)) // 1
            innerDataArray.add(getSyntax("en", c.syntax)) // 2
            innerDataArray.add(DataArray.fromCollection(c.aliases.toList())) // 3
            val argumentsHelp = i18n.getTranslationN("en", c.argumentString, false)
            innerDataArray.add(argumentsHelp?.replace("%help\\.arg\\..*?%".toRegex()) {
                it.groups[0]?.let { (value) ->
                    i18n.getTranslation("en", value.substring(1, value.length - 1))
                } ?: "report to devs it's BROKEN :c"
            } ?: "") // 4
            innerDataArray.add(
                DataArray.fromCollection(c.discordChannelPermissions.map { it.toString() })
            ) // 5
            innerDataArray.add(
                DataArray.fromCollection(c.discordPermissions.map { it.toString() })
            ) // 6
            innerDataArray.add(
                DataArray.fromCollection(c.runConditions.map { it.toString() })
            ) // 7
            innerDataArray.add(
                c.permissionRequired
            ) // 8
            innerDataArray.add(getDataArrayArrayFrom(c.children)) // 9
            innerDataArray.add(i18n.getTranslationN("en", c.help, false) ?: "") // 10
            innerDataArray.add(i18n.getTranslationN("en", c.examples, false) ?: "") // 11
            dataArray.add(innerDataArray)
        }
        return dataArray
    }
}