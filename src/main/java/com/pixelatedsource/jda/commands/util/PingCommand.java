package com.pixelatedsource.jda.commands.util;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.blub.Category;
import com.pixelatedsource.jda.blub.Command;
import com.pixelatedsource.jda.blub.CommandEvent;

import java.time.temporal.ChronoUnit;

import static com.pixelatedsource.jda.PixelSniper.PREFIX;

public class PingCommand extends Command {

    public PingCommand() {
        this.commandName = "ping";
        this.description = "Shows you the bot's ping";
        this.usage = PREFIX + this.commandName;
        this.category = Category.UTILS;
    }

    @Override
    protected void execute(CommandEvent event) {
        boolean acces = false;
        if (event.getGuild() == null) acces = true;
        if (!acces) acces = Helpers.hasPerm(event.getGuild().getMember(event.getAuthor()), this.commandName, 0);
        if (acces) {
            event.getChannel().sendMessage("Ping... ").queue(m -> m.editMessage("Ping: " + event.getMessage().getCreationTime().until(m.getCreationTime(), ChronoUnit.MILLIS) + "ms | " + "Websocket: " + event.getJDA().getPing() + "ms").queue());
        }
    }
}
