package com.pixelatedsource.jda.commands.util;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.blub.Category;
import com.pixelatedsource.jda.blub.Command;
import com.pixelatedsource.jda.blub.CommandEvent;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.User;

import static com.pixelatedsource.jda.PixelSniper.PREFIX;

public class AvatarCommand extends Command {

    public AvatarCommand() {
        this.commandName = "avatar";
        this.usage = PREFIX + this.commandName + " [user]";
        this.description = "Shows you your avatar and link.";
        this.aliases = new String[]{"profilepicture"};
        this.category = Category.UTILS;
        this.permissions = new Permission[]{Permission.MESSAGE_EMBED_LINKS};
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() == null || Helpers.hasPerm(event.getMember(), this.commandName, 0)) {
            String[] args = event.getArgs().split("\\s+");
            User user;
            if (args.length == 0 || args[0].equalsIgnoreCase("")) {
                user = event.getAuthor();
            } else {
                user = Helpers.getUserByArgsN(event, args[0]);
            }
            if (user != null) {
                String url = user.getAvatarUrl() == null ? user.getDefaultAvatarUrl() : user.getAvatarUrl();
                EmbedBuilder eb = new EmbedBuilder();
                eb.setColor(Helpers.EmbedColor);
                eb.setTitle(user.getName() + "#" + user.getDiscriminator() + "'s avatar");
                eb.setImage(url + "?size=2048");
                eb.setDescription("[open](" + url + "?size=4096)");
                 event.reply(eb.build());
            } else {
                event.reply("Unknown user");
            }
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
