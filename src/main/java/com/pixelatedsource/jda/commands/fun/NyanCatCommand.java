package com.pixelatedsource.jda.commands.fun;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.blub.Category;
import com.pixelatedsource.jda.blub.Command;
import com.pixelatedsource.jda.blub.CommandEvent;
import com.pixelatedsource.jda.utils.MessageHelper;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;

import static com.pixelatedsource.jda.PixelSniper.PREFIX;

public class NyanCatCommand extends Command {

    public NyanCatCommand() {
        this.commandName = "nyancat";
        this.description = "shows a random nyancat";
        this.usage = PREFIX + commandName;
        this.category = Category.FUN;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() == null || Helpers.hasPerm(event.getMember(), this.commandName, 0)) {
            if (event.getGuild() == null || event.getGuild().getSelfMember().hasPermission(Permission.MESSAGE_EMBED_LINKS))
                event.reply(new EmbedBuilder()
                        .setColor(Helpers.EmbedColor)
                        .setDescription("Enjoy your " + event.getJDA().getEmoteById("462632448442892316").getAsMention() + " ~meow!~")
                        .setImage("https://github.com/ToxicMushroom/nyan-cats/raw/master/cat%20(" + MessageHelper.randInt(2, 33) + ").gif")
                        .build());
            else
                event.reply("Enjoy your " + event.getJDA().getEmoteById("462632448442892316").getAsMention() + " ~meow!~\n"
                        + "https://github.com/ToxicMushroom/nyan-cats/raw/master/cat%20(" + MessageHelper.randInt(2, 33) + ").gif");
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
