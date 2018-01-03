package com.pixelatedsource.jda.commands;

import com.jagrosh.jdautilities.commandclient.Command;
import com.jagrosh.jdautilities.commandclient.CommandEvent;
import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.PixelatedBot;
import net.dv8tion.jda.core.EmbedBuilder;

public class AboutCommand extends Command {

    public AboutCommand() {
        this.name = "about";
        this.help = "Shows info about the bot -> Usage: " + PixelatedBot.PREFIX + this.name;
        this.guildOnly = false;
        this.aliases = new String[]{"info", "botinfo", "author"};
    }

    @Override
    protected void execute(CommandEvent event) {
        event.getJDA().getSelfUser().getManager().setName("PixelSniper").queue();
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("About");
        eb.setColor(Helpers.EmbedColor);
        eb.addField("Author", "[ToxicMushroom](https://www.youtube.com/toxicmushroom)", true);
        eb.addField("Total server count", String.valueOf(event.getJDA().getGuilds().size()), true);
        eb.addField("Total user count", String.valueOf(event.getJDA().getUsers().size()), true);
        eb.addField("Libs", "[JDA](https://github.com/DV8FromTheWorld/JDA), [JDA-Utilities](https://github.com/JDA-Applications/JDA-Utilities), [LavaPlayer](https://github.com/sedmelluq/lavaplayer)", false);
        eb.addField("Online time", Helpers.getOnlineTime(), false);
        event.reply(eb.build());
    }
}
