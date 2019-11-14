package me.melijn.melijnbot.objects.music

import net.dv8tion.jda.api.entities.User

data class TrackUserData(val userId: Long, val username: String, val userDiscrim: String, val currentTime: Long = System.nanoTime()) {
    constructor(user: User) : this(user.idLong, user.name, user.discriminator)

    val userTag = "$username#$userDiscrim"
}