package me.melijn.melijnbot.enums

enum class GainType(
    val band0: Float = 0.0f,
    val band1: Float = 0.0f,
    val band2: Float = 0.0f,
    val band3: Float = 0.0f,
    val band4: Float = 0.0f,
    val band5: Float = 0.0f,
    val band6: Float = 0.0f,
    val band7: Float = 0.0f,
    val band8: Float = 0.0f,
    val band9: Float = 0.0f,
    val band10: Float = 0.0f,
    val band11: Float = 0.0f,
    val band12: Float = 0.0f,
    val band13: Float = 0.0f,
    val band14: Float = 0.0f
) {
    BASS(
        band0 = 1f,
        band1 = 1f,
        band2 = 1f,
        band3 = 1f,
        band4 = 0.25f
    ),DEFAULT()
}