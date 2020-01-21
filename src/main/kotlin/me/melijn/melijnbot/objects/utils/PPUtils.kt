package me.melijn.melijnbot.objects.utils

import me.melijn.melijnbot.Container
import net.dv8tion.jda.api.entities.Member

object PPUtils {

    suspend fun updatePP(member: Member, ppMap: Map<String, Long>, container: Container) {
        val apWrapper = container.daoManager.autoPunishmentWrapper
        apWrapper.set(member.guild.idLong, member.idLong, ppMap)
    }
}