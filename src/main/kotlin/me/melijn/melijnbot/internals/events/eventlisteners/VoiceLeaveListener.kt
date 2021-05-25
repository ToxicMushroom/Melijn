package me.melijn.melijnbot.internals.events.eventlisteners

import me.melijn.melijnbot.Container
import me.melijn.melijnbot.database.ban.BotBannedWrapper
import me.melijn.melijnbot.database.locking.EntityType
import me.melijn.melijnbot.internals.events.AbstractListener
import me.melijn.melijnbot.internals.events.eventutil.VoiceUtil
import me.melijn.melijnbot.internals.threading.TaskManager
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent

class VoiceLeaveListener(container: Container) : AbstractListener(container) {

    override suspend fun onEvent(event: GenericEvent) {
        if (event is GuildVoiceLeaveEvent) {
            if (!event.member.user.isBot && !BotBannedWrapper.isBotBanned(EntityType.GUILD, event.guild.idLong)) {
                TaskManager.async(event.member) {
                    VoiceUtil.channelUpdate(container, event.channelLeft)
                    VoiceUtil.handleChannelRoleLeave(container.daoManager, event.member, event.channelLeft)
                }
            }
        }
    }
}