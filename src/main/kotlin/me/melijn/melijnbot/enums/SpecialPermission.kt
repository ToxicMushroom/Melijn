package me.melijn.melijnbot.enums

enum class SpecialPermission(val node: String) {
    MUSIC_BYPASS_SAMEVC("music.bypass.samevc"),
    MUSIC_BYPASS_BOTALONE("music.bypass.botalone"),
    MUSIC_BYPASS_VCBOTALONE("music.bypass.vcbotalone"),
    MUSIC_BYPASS_SAMEVCALONE("music.bypass.samevcalone"),
    SUMMON_OTHER("summon.other"),
    PUNISH_BYPASS_HIGHER("punish.bypass.higher"),
    CHANGENAME_OTHER("changename.other"),
    GIVEROLE_BYPASS_HIGHER("giverole.bypass.higher"),
    TAKEROLE_BYPASS_HIGHER("takerole.bypass.higher")
}