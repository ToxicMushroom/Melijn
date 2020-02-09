package me.melijn.melijnbot.enums

enum class MessageType(val text: String){
    JOIN("JoinMessage"),
    LEAVE("LeaveMessage"),
    CUSTOM_COMMAND("CustomCommandMessage"),
    BIRTHDAY("BirthdayMessage")
}