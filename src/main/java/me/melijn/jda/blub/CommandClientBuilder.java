package me.melijn.jda.blub;

import me.melijn.jda.Melijn;

import java.util.HashSet;
import java.util.Set;

public class CommandClientBuilder {

    private final long ownerId;
    private final Set<Command> commands = new HashSet<>();
    private final Melijn melijn;

    public CommandClientBuilder(Melijn melijn, long ownerId) {
        this.melijn = melijn;
        this.ownerId = ownerId;
    }

    public CommandClient build() {
        return new CommandClientImpl(melijn, ownerId, commands);
    }

    public CommandClientBuilder addCommand(Command command) {
        commands.add(command);
        melijn.getMySQL().addCommand(command);
        return this;
    }
}