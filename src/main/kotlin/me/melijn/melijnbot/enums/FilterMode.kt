package me.melijn.melijnbot.enums

enum class FilterMode {
    MUST_CONTAIN_ANY_ALLOWED,
    MUST_CONTAIN_ALL_ALLOWED,
    MUST_MATCH_ALLOWED_FORMAT,
    NO_WRAP,
    DISABLED,
    DEFAULT,
    NO_MODE
}