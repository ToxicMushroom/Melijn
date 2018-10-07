package me.melijn.jda.blub;

import me.melijn.jda.Melijn;

import java.util.ArrayList;
import java.util.List;

public class CommandClientBuilder {

    private long ownerId;
    private final List<Command> commands = new ArrayList<>();
    //private CommandListener listener;

    public CommandClient build() {
        //if (listener != null) client.setListener(listener);
        return new CommandClientImpl(ownerId, commands);
    }

    public List<Command> getCommands() {
        return commands;
    }

    public CommandClientBuilder setOwnerId(long ownerId) {
        this.ownerId = ownerId;
        return this;
    }
/*
    public CommandClientBuilder setListener(CommandListener listener) {
        this.listener = listener;
        return this;
    }
*/
    private CommandClientBuilder addCommand(Command command) {
        commands.add(command);
        return this;
    }

    public CommandClientBuilder addCommands(Command... commands) {
        for (Command command : commands) {
            this.addCommand(command);
            Melijn.mySQL.addCommand(command);
        }
        return this;
    }
}