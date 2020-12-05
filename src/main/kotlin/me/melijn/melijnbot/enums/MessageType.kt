package me.melijn.melijnbot.enums

enum class MessageType(val text: String) {
    PRE_VERIFICATION_JOIN_MESSAGE("PreVerificationJoinMessage"),
    PRE_VERIFICATION_LEAVE_MESSAGE("PreVerificationLeaveMessage"),
    JOIN("JoinMessage"),
    LEAVE("LeaveMessage"),
    BANNED("BannedMessage"),
    KICKED("KickedMessage"),
    CUSTOM_COMMAND("CustomCommandMessage"),
    BIRTHDAY("BirthdayMessage"),
    BOOST("BoostMessage"),
    GIVEAWAY("GiveawayMessage")
}