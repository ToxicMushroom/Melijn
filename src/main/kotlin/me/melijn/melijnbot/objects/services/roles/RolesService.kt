package me.melijn.melijnbot.objects.services.roles

import me.melijn.melijnbot.database.role.TempRoleWrapper
import me.melijn.melijnbot.objects.services.Service
import me.melijn.melijnbot.objects.threading.RunnableTask
import me.melijn.melijnbot.objects.utils.awaitOrNull
import net.dv8tion.jda.api.sharding.ShardManager
import java.util.concurrent.TimeUnit

class RolesService(
    private val roleWrapper: TempRoleWrapper,
    private val shardManager: ShardManager
) : Service("Roles", 1000, 5500, TimeUnit.MILLISECONDS) {

    override val service = RunnableTask {
        val theObjects = roleWrapper.getObjects()
        for ((guildId, userId, roleId, _, _, added) in theObjects) {
            val guild = shardManager.getGuildById(guildId) ?: continue
            val role = guild.getRoleById(roleId) ?: continue
            val member = guild.retrieveMemberById(userId).awaitOrNull() ?: continue

            if (added) {
                guild.removeRoleFromMember(member, role).reason("TempRole").queue(null, null)
            } else {
                guild.addRoleToMember(member, role).reason("TempRole").queue(null, null)
            }
        }
    }
}