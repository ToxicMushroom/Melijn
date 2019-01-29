package me.melijn.jda.blub;

import me.melijn.jda.Melijn;

import java.util.List;

public interface CommandClient {

    Melijn getMelijn();
    void addCommand(Command command);
    void addCommand(Command command, int index);
    List<Command> getCommands();

}
