package me.melijn.melijnbot.enums

enum class LogChannelType(val text: String = this.toString(), val parentNodes: Array<String> = emptyArray()) {

    //Punishments
    PERMANENT_BAN("PermanentBan", arrayOf("punishment", "punishments", "ban", "permban", "pblc")),
    TEMP_BAN("TemporaryBan", arrayOf("punishment", "punishments", "ban", "tempban", "tblc")),
    SOFT_BAN("SoftBan", arrayOf("punishment", "punishments", "ban", "sblc")),
    UNBAN("Unban", arrayOf("punishment", "punishments", "ban", "unban", "pardon", "ublc")),

    PERMANENT_MUTE("PermanentMute", arrayOf("punishment", "punishments", "mute", "permmute", "pmlc")),
    TEMP_MUTE("TempMute", arrayOf("punishment", "punishments", "mute", "tempmute", "tmlc")),
    UNMUTE("Unmute", arrayOf("punishment", "punishments", "mute", "unmute", "umlc")),

    KICK("Kick", arrayOf("punishment", "punishments", "kick", "klc")),
    WARN("Warn", arrayOf("punishment", "punishments", "warn", "wlc")),

    //Deleted Messages
    OTHER_DELETED_MESSAGE("Other-Deleted-Message", arrayOf("deleted-messages", "deleted-message", "odm", "other-deleted-messages", "other-deleted-message", "odmlc")),
    SELF_DELETED_MESSAGE("Self-Deleted-Message", arrayOf("deleted-messages", "deleted-message", "sdm", "self-deleted-messages", "self-deleted-message", "sdmlc")),
    PURGED_MESSAGE("Purged-Message", arrayOf("deleted-messages", "deleted-message", "pm", "purged-messages", "purges-message", "pmlc")),
    FILTERED_MESSAGE("Filtered-Message", arrayOf("deleted-messages", "deleted-message", "fm", "filtered-messages", "filtered-message", "fmlc")),

    //Others
    VERIFICATION("mlo-Verification", arrayOf("verification", "v", "uv", "vlc", "uvlc")),
    EDITED_MESSAGE("Edited-Message", arrayOf("em", "edited-messages", "edited-message", "emlc")),
    REACTION("Reaction", arrayOf("r", "reaction", "reactions", "mr", "rlc")),
    ATTACHMENT("Attachment", arrayOf("a", "alc")),
    BOT("Bot", arrayOf("b", "blc"));


    //Messages
    companion object {

        fun getMatchingTypesFromNode(node: String): List<LogChannelType> {
            return values().filter { channel ->
                channel.parentNodes.contains(node.toLowerCase()) || node.equals("all", true)
            }
        }
    }
}
