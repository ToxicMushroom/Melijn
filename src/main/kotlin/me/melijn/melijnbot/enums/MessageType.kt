package me.melijn.melijnbot.enums

enum class MessageType(val base: String) {

    PRE_VERIFICATION_JOIN("PreVerificationJoin"),
    PRE_VERIFICATION_LEAVE("PreVerificationLeave"),
    JOIN("Join"),
    LEAVE("Leave"),
    BANNED("Banned"),
    KICKED("Kicked"),
    CUSTOM_COMMAND("CustomCommand"),
    BIRTHDAY("Birthday"),
    BOOST("Boost");

    val text: String = "${base}Message"

    companion object {

        fun getMatchingTypesFromNode(node: String): List<MessageType> {
            return values().filter { msgType ->
                node.equals("all", true)
                    || msgType.text.equals(node, true)
                    || msgType.base.equals(node, true)
                    || msgType.toString().equals(node, true)
            }
        }
    }
}