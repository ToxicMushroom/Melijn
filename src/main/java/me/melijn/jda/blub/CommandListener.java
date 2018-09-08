package me.melijn.jda.blub;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public interface CommandListener {

    void onCommand(CommandEvent event, Command command);

    void onCompletedCommand(CommandEvent event, Command command);

    void onTerminatedCommand(CommandEvent event, Command command);

    void onNonCommandMessage(MessageReceivedEvent event);
}
