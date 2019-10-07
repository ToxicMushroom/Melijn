package me.melijn.melijnbot.objects.utils

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.database.DaoManager
import me.melijn.melijnbot.enums.RoleType
import me.melijn.melijnbot.objects.translation.getLanguage
import me.melijn.melijnbot.objects.translation.i18n
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.entities.User

object VerificationUtils {
    suspend fun getUnverifiedRoleNMessage(user: User?, textChannel: TextChannel, daoManager: DaoManager): Role? {
        val guild = textChannel.guild
        val unverifiedRoleId = daoManager.roleWrapper.roleCache[Pair(guild.idLong, RoleType.UNVERIFIED)].await()

        val role = if (unverifiedRoleId == -1L){
            null
        } else {
            guild.getRoleById(unverifiedRoleId)
        }

        if (role == null) {
            sendNoUnverifiedRoleIsSetMessage(daoManager, user, textChannel)
        }

        return role
    }

    private suspend fun sendNoUnverifiedRoleIsSetMessage(daoManager: DaoManager, user: User?, textChannel: TextChannel) {
        val language = getLanguage(daoManager, user?.idLong ?: -1L, textChannel.guild.idLong)
        val msg = i18n.getTranslation(language, "message.notset.role.unverified")
        sendMsg(textChannel, msg)
    }

    fun verify(role: Role, member: Member) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}