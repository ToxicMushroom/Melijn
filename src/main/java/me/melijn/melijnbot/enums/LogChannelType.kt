package me.melijn.melijnbot.enums

enum class LogChannelType(val text: String = this.toString(), val parentNodes: Array<String> = emptyArray()) {

    //Punishments
    PERMANENT_BAN("PermanentBanLogChannel", arrayOf("ban", "permBan", "pblc")),
    TEMP_BAN("TemporaryBanLogChannel", arrayOf("ban", "tempBan", "tblc")),
    PERMANENT_MUTE("PermanentMuteLogChannel", arrayOf("mute", "permmute", "pmlc")),
    TEMP_MUTE("TempMuteLogChannel", arrayOf("mute", "tempmute", "tmlc")),
    KICK("KickLogChannel", arrayOf("kick", "klc"));

    //Messages

    companion object {
        fun getMatchingTypesFromNode(node: String): List<LogChannelType> {
            return values().filter { channel ->
                channel.parentNodes.contains(node.toLowerCase()) || node.equals("all", true)
            }
        }
    }

}