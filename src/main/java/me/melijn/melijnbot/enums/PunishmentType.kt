package me.melijn.melijnbot.enums

enum class PunishmentType(val aliases: Array<String>) {

    BAN(arrayOf("bans")),
    MUTE(arrayOf("mutes")),
    KICK(arrayOf("kicks")),
    WARN(arrayOf("warns"));

    companion object {
        fun getMatchingTypesFromNode(node: String): List<PunishmentType> {
            return values().filter { punishmentType ->
                node.equals("all", true)
                        || punishmentType.aliases.contains(node.toLowerCase())
                        || node.equals(toString(), true)
            }
        }
    }
}