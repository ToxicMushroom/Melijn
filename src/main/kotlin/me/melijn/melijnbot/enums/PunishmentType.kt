package me.melijn.melijnbot.enums

import me.melijn.melijnbot.enums.parsable.ParsableEnum

enum class PunishmentType(val aliases: Set<String>) : ParsableEnum {

    BAN(setOf("ban", "bans")),
    MUTE(setOf("mute", "mutes")),
    KICK(setOf("kick", "kicks")),
    WARN(setOf("warn", "warns")),
    SOFTBAN(setOf("softban", "softbans")),
    ADDROLE(setOf("addRole")),
    REMOVEROLE(setOf("removeRole"));

    override fun aliases(): Set<String> = this.aliases

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