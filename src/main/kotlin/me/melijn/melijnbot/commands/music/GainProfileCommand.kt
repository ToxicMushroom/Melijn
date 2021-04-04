package me.melijn.melijnbot.commands.music

import me.melijn.melijnbot.database.audio.GainProfile
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.command.PLACEHOLDER_PREFIX
import me.melijn.melijnbot.internals.translation.PLACEHOLDER_ARG
import me.melijn.melijnbot.internals.utils.getIntegerFromArgNMessage
import me.melijn.melijnbot.internals.utils.getStringFromArgsNMessage
import me.melijn.melijnbot.internals.utils.isPositiveNumber
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.withSafeVariable

class GainProfileCommand : AbstractGainProfileCommand(
    "command.gainprofile",
    { context ->
        context.guildId
    }) {

    init {
        id = 141
        name = "gainProfile"
        aliases = arrayOf("gp")
    }
}

suspend fun getGainProfileNMessage(
    context: ICommandContext,
    map: Map<String, GainProfile>,
    index: Int
): Pair<String, GainProfile>? {
    val name: String
    val profileName = if (context.args[index].isPositiveNumber()) {
        val profileIndex = getIntegerFromArgNMessage(context, index, 0, map.size - 1) ?: return null
        name = map.keys.sortedBy { it }[profileIndex]
        map[name]
    } else {
        name = getStringFromArgsNMessage(context, index, 1, 20) ?: return null
        if (map.keys.contains(name)) {
            map[name]
        } else {
            val msg = context.getTranslation("${context.commandOrder.first().root}.notfound")
                .withSafeVariable(PLACEHOLDER_ARG, name)
                .withSafeVariable(PLACEHOLDER_PREFIX, context.usedPrefix)
            sendRsp(context, msg)
            return null
        }
    } ?: throw IllegalArgumentException("no this is bad code smh")

    return Pair(name, profileName)
}