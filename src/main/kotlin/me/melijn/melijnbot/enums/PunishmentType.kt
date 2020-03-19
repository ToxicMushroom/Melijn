package me.melijn.melijnbot.enums

enum class PunishmentType(val aliases: Array<String>) {

    BAN(arrayOf("ban", "bans")),
    MUTE(arrayOf("mute", "mutes")),
    KICK(arrayOf("kick", "kicks")),
    WARN(arrayOf("warn", "warns")),
    SOFTBAN(arrayOf("softban", "softbans")),
    ADDROLE(arrayOf("addRole")),
    REMOVEROLE(arrayOf("removeRole"));

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