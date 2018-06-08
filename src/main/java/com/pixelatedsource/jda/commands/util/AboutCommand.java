package com.pixelatedsource.jda.commands.util;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.blub.Category;
import com.pixelatedsource.jda.blub.Command;
import com.pixelatedsource.jda.blub.CommandEvent;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;

import static com.pixelatedsource.jda.PixelSniper.OWNERID;
import static com.pixelatedsource.jda.PixelSniper.PREFIX;

public class AboutCommand extends Command {

    public AboutCommand() {
        this.commandName = "about";
        this.usage = PREFIX + this.commandName;
        this.description = "Shows you useful info about the bot itself";
        this.aliases = new String[]{"info", "botinfo", "author"};
        this.category = Category.UTILS;
        this.permissions = new Permission[]{Permission.MESSAGE_EMBED_LINKS};
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() == null || Helpers.hasPerm(event.getMember(), this.commandName, 0)) {
            String[] args = event.getArgs().split("\\s+");
            EmbedBuilder eb = new EmbedBuilder();
            eb.setTitle("About");
            eb.setColor(Helpers.EmbedColor);
            eb.addField("Author", "[ToxicMushroom](https://www.youtube.com/toxicmushroom)", true);
            eb.addField("Total server count", String.valueOf(event.getJDA().getGuilds().size()), true);
            eb.addField("Total user count", String.valueOf(event.getJDA().getUsers().size()), true);
            eb.addField("Libs", "[JDA](https://github.com/DV8FromTheWorld/JDA), [MySQL connector](https://dev.mysql.com/downloads/connector/j/5.1.html), [LavaPlayer](https://github.com/sedmelluq/lavaplayer)", false);
            eb.addField("Online time", Helpers.getOnlineTime(), false);
            int i = 0;
            for (Guild guild : event.getJDA().getGuilds()) {
                if (guild.getAudioManager().isConnected() || guild.getAudioManager().isAttemptingToConnect()) i++;
            }
            eb.addField("Playing music count", String.valueOf(i), false);
            event.reply(eb.build());
            if (event.getAuthor().getIdLong() == OWNERID) {
                StringBuilder desc = new StringBuilder();
                int blub = 0;
                for (Guild guild : event.getJDA().getGuilds()) {
                    if (guild.getAudioManager().isConnected() || guild.getAudioManager().isAttemptingToConnect())
                        desc.append("**#").append(++blub).append("** - ").append(guild.getName()).append("\n");
                }
                if (desc.toString().length() > 0)
                    event.getAuthor().openPrivateChannel().queue(s -> s.sendMessage("dis is da wae:\n" + desc.toString()).queue());
            }
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
