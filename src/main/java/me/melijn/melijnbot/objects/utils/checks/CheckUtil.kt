package me.melijn.melijnbot.objects.utils.checks

import me.melijn.melijnbot.database.channel.ChannelWrapper
import me.melijn.melijnbot.database.logchannel.LogChannelWrapper
import me.melijn.melijnbot.enums.ChannelType
import me.melijn.melijnbot.enums.LogChannelType
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.TextChannel

suspend fun Guild.getAndVerifyLogChannelById(type: LogChannelType, channelId: Long, logChannelWrapper: LogChannelWrapper): TextChannel? {
    val textChannel = getTextChannelById(channelId)
    var shouldRemove = false
    if (channelId != -1L && textChannel == null) shouldRemove = true
    if (textChannel?.canTalk() == true) shouldRemove = true

    if (shouldRemove) {
        logChannelWrapper.removeChannel(this.idLong, type)
    }
    return textChannel
}

suspend fun Guild.getAndVerifyChannelById(type: ChannelType, channelId: Long, channelWrapper: ChannelWrapper): TextChannel? {
    val textChannel = getTextChannelById(channelId)
    var shouldRemove = false
    if (channelId != -1L && textChannel == null) shouldRemove = true
    if (textChannel?.canTalk() == false) shouldRemove = true

    if (shouldRemove) {
        channelWrapper.removeChannel(this.idLong, type)
    }

    return textChannel
}