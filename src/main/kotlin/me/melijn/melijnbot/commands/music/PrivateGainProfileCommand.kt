package me.melijn.melijnbot.commands.music

const val PRIVATE_GAIN_PROFILES_LIMIT = 3
const val PREMIUM_PRIVATE_GAIN_PROFILES_LIMIT = 30
const val PRIVATE_GAIN_PROFILES_LIMIT_PATH = "premium.feature.privategainprofiles.limit"
const val PRIVATE_GAIN_PROFILES_PREMIUM_LIMIT_PATH = "premium.feature.privategainprofiles.premiumlimit"

class PrivateGainProfileCommand : AbstractGainProfileCommand(
    "command.privategainprofile", { context ->
        context.authorId
    }) {

    init {
        name = "privateGainProfile"
        aliases = arrayOf("pgp")
    }
}