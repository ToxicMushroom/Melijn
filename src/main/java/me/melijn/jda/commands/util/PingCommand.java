package me.melijn.jda.commands.util;

import me.melijn.jda.Helpers;
import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;

import java.time.temporal.ChronoUnit;

import static me.melijn.jda.Melijn.PREFIX;

public class PingCommand extends Command {

    public PingCommand() {
        this.commandName = "ping";
        this.description = "Shows you the bot's ping";
        this.usage = PREFIX + this.commandName;
        this.category = Category.UTILS;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() == null || Helpers.hasPerm(event.getMember(), this.commandName, 0)) {
            event.getChannel().sendMessage("Pinging... ").queue((m) ->
                    m.editMessage("\uD83C\uDFD3 Ping: " +
                            event.getMessage().getCreationTime().until(m.getCreationTime(), ChronoUnit.MILLIS) + "ms | " + "Websocket: " + event.getJDA().getPing() + "ms").queue());
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
