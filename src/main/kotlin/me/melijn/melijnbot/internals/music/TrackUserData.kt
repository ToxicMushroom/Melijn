package me.melijn.melijnbot.internals.music

import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.utils.data.DataObject

data class TrackUserData(
    val userId: Long,
    val username: String,
    val userDiscrim: String,
    val currentTime: Long = System.nanoTime()
) {

    constructor(user: User) : this(user.idLong, user.name, user.discriminator)

    companion object {
        fun fromMessage(message: String): TrackUserData {
            val dataObject = DataObject.fromJson(message)

            return TrackUserData(
                dataObject.getLong("userId"),
                dataObject.getString("username"),
                dataObject.getString("userDiscrim"),
                dataObject.getLong("currentTime")
            )
        }
    }

    val userTag = "$username#$userDiscrim"
}

fun TrackUserData.toMessage(): String = DataObject.empty()
    .put("userId", this.userId)
    .put("username", this.username)
    .put("userDiscrim", this.userDiscrim)
    .put("currentTime", this.currentTime)
    .toString()

