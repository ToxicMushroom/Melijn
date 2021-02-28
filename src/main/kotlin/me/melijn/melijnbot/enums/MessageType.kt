package me.melijn.melijnbot.enums

enum class MessageType(val text: String) {
    PRE_VERIFICATION_JOIN("PreVerificationJoinMessage"),
    PRE_VERIFICATION_LEAVE("PreVerificationLeaveMessage"),
    JOIN("JoinMessage"),
    LEAVE("LeaveMessage"),
    BANNED("BannedMessage"),
    KICKED("KickedMessage"),
    CUSTOM_COMMAND("CustomCommandMessage"),
    BIRTHDAY("BirthdayMessage"),
    BOOST("BoostMessage"),
}