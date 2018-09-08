package me.melijn.jda.blub;

import me.melijn.jda.Melijn;

import java.util.ArrayList;

public class CommandClientBuilder {

    private long ownerId;
    private final ArrayList<Command> commands = new ArrayList<>();
    private CommandListener listener;
    private int linkedCacheSize = 200;

    public CommandClient build() {
        CommandClient client = new CommandClientImpl(ownerId, commands, linkedCacheSize);
        if (listener != null) client.setListener(listener);
        return client;
    }

    public ArrayList<Command> getCommands() {
        return commands;
    }

    public CommandClientBuilder setOwnerId(long ownerId) {
        this.ownerId = ownerId;
        return this;
    }

    public CommandClientBuilder setListener(CommandListener listener) {
        this.listener = listener;
        return this;
    }

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

    public CommandClientBuilder setLinkedCacheSize(int linkedCacheSize) {
        this.linkedCacheSize = linkedCacheSize;
        return this;
    }
}