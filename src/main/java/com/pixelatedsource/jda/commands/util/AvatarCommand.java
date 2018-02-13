package com.pixelatedsource.jda.commands.util;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.blub.Category;
import com.pixelatedsource.jda.blub.Command;
import com.pixelatedsource.jda.blub.CommandEvent;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.User;

import static com.pixelatedsource.jda.PixelSniper.PREFIX;

public class AvatarCommand extends Command {

    public AvatarCommand() {
        this.commandName = "avatar";
        this.usage = PREFIX + this.commandName + " [user]";
        this.description = "Shows you your avatar and link.";
        this.aliases = new String[]{"profilepicture"};
        this.category = Category.UTILS;
    }

    @Override
    protected void execute(CommandEvent event) {
        boolean acces = false;
        if (event.getGuild() == null) acces = true;
        if (!acces) acces = Helpers.hasPerm(event.getGuild().getMember(event.getAuthor()), this.commandName, 0);
        if (acces) {
            String[] args = event.getArgs().split("\\s+");
            User author = null;
            if (args.length == 0 || args[0].equalsIgnoreCase("")) {
                author = event.getAuthor();
            } else {
                if (event.getMessage().getMentionedUsers().size() < 1) {
                    author = event.getMessage().getMentionedUsers().get(0);
                } else {
                    if (args[0].matches("\\d+")) {
                        User toCheck = event.getJDA().retrieveUserById(args[0]).complete();
                        if (toCheck != null) author = toCheck;
                    }
                }
            }
            if (author != null) {
                EmbedBuilder eb = new EmbedBuilder();
                eb.setColor(Helpers.EmbedColor);
                eb.setTitle(author.getName() + "#" + author.getDiscriminator() + "'s avatar");
                eb.setImage(author.getAvatarUrl() + "?size=1024");
                eb.setDescription("[Avatar URL Link](" + author.getAvatarUrl() + "?size=1024)");
                event.reply(eb.build());
            } else {
                event.reply("Unknown user");
            }
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
