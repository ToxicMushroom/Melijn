package me.melijn.melijnbot.enums

enum class ModularMessageProperty(val variableName: String) {
    CONTENT("content"),
    EMBED_DESCRIPTION("description"),
    EMBED_TITLE("title"),
    EMBED_URL("titleUrl"),
    EMBED_COLOR("color"),
    EMBED_THUMBNAIL("thumbnail"),
    EMBED_IMAGE("image"),
    EMBED_AUTHOR("author"),
    EMBED_AUTHOR_URL("authorUrl"),
    EMBED_AUTHOR_ICON_URL("authorIcon"),
    EMBED_FOOTER("footer"),
    EMBED_FOOTER_ICON_URL("footerIcon"),
    EMBED_TIME_STAMP("timeStamp")
}