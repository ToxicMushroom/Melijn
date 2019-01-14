package me.melijn.jda.blub;

import me.melijn.jda.Melijn;

import java.util.ArrayList;
import java.util.List;

public class CommandClientBuilder {

    private long ownerId;
    private final List<Command> commands = new ArrayList<>();

    public CommandClient build() {
        return new CommandClientImpl(ownerId, commands);
    }

    public List<Command> getCommands() {
        return commands;
    }

    public CommandClientBuilder setOwnerId(long ownerId) {
        this.ownerId = ownerId;
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
}