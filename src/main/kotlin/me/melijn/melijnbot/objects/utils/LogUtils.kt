package me.melijn.melijnbot.objects.utils

import me.melijn.melijnbot.enums.ChannelType
import me.melijn.melijnbot.objects.translation.i18n
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.TextChannel
import java.awt.Color

object LogUtils {
    fun sendRemovedChannelLog(language: String, channel: ChannelType,  logChannel: TextChannel, cause: String, causeArg: String) {
        val title = i18n.getTranslation(language, "logging.removed.channel.title")
            .replace("%type%", channel.toUCC())
        val cause = i18n.getTranslation(language, "logging.removed.channel.cause.$cause")
            .replace("%causeArg%", causeArg)


        val eb = EmbedBuilder()
        eb.setTitle(title)
        eb.setColor(Color.decode("#7289DA"))
        eb.setDescription(cause)
        eb.setFooter(getTimeStamp)
    }
}