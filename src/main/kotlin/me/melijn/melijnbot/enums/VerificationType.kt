package me.melijn.melijnbot.enums

import me.melijn.melijnbot.enums.parsable.ParsableEnum

enum class VerificationType : ParsableEnum {
    PASSWORD,
    GOOGLE_RECAPTCHAV2,
    REACTION,
    NONE
}