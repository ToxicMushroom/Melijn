package me.melijn.melijnbot.enums

enum class LogChannelType(val text: String = this.toString(), val parentNodes: Array<String> = emptyArray()) {

    //Punishments
    PERMANENT_BAN("PermanentBan", arrayOf("ban", "permBan", "pblc")),
    TEMP_BAN("TemporaryBan", arrayOf("ban", "tempBan", "tblc")),
    PERMANENT_MUTE("PermanentMute", arrayOf("mute", "permmute", "pmlc")),
    TEMP_MUTE("TempMute", arrayOf("mute", "tempmute", "tmlc")),
    KICK("Kick", arrayOf("kick", "klc"));

    //Messages

    companion object {
        fun getMatchingTypesFromNode(node: String): List<LogChannelType> {
            return values().filter { channel ->
                channel.parentNodes.contains(node.toLowerCase()) || node.equals("all", true)
            }
        }
    }

}