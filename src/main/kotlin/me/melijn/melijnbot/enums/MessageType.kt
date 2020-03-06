package me.melijn.melijnbot.enums

enum class MessageType(val text: String) {
    PRE_VERIFICATION_MESSAGE("PreVerificationMessage"),
    JOIN("JoinMessage"),
    LEAVE("LeaveMessage"),
    CUSTOM_COMMAND("CustomCommandMessage"),
    BIRTHDAY("BirthdayMessage")
}