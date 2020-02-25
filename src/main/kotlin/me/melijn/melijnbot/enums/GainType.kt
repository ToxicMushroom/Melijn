package me.melijn.melijnbot.enums

import me.melijn.melijnbot.database.audio.GainProfile

enum class GainType(
    val gainProfile: GainProfile = GainProfile()
) {

    BASS(
        GainProfile(
            band0 = 0.40f,
            band1 = 0.40f,
            band2 = 0.40f,
            band3 = 0.40f,
            band4 = 0.40f,
            band5 = 0.40f,
            band6 = 0.30f,
            band7 = 0.25f,
            band8 = 0.25f,
            band9 = 0.25f,
            band10 = 0.20f,
            band11 = 0.20f,
            band12 = 0.20f,
            band13 = 0.20f,
            band14 = 0.20f
        )
    ),
    DEFAULT()
}