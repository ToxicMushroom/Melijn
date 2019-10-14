package me.melijn.melijnbot.objects.utils

import me.melijn.melijnbot.database.DaoManager
import me.melijn.melijnbot.enums.ChannelType
import me.melijn.melijnbot.objects.translation.i18n
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.TextChannel
import java.awt.Color

object LogUtils {
    fun sendRemovedChannelLog(language: String, channel: ChannelType, logChannel: TextChannel?, causePath: String, causeArg: String) {
        if (logChannel == null) return
        val title = i18n.getTranslation(language, "logging.removed.channel.title")
            .replace("%type%", channel.toUCC())
        val cause = i18n.getTranslation(language, "logging.removed.channel.causePath.$causePath")
            .replace("%causeArg%", causeArg)


        val eb = EmbedBuilder()
        eb.setTitle(title)
        eb.setColor(Color.decode("#7289DA"))
        eb.setDescription(cause)
        eb.setFooter(System.currentTimeMillis().asEpochMillisToDateTime())

        logChannel.sendMessage(eb.build())
    }

    fun sendHitVerificationTroughputLimitLog(daoManager: DaoManager, member: Member) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun sendFailedVerificationLog(dao: DaoManager, member: Member) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.

    }
}