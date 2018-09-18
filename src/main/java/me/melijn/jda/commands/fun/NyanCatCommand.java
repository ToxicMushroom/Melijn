package me.melijn.jda.commands.fun;

import me.melijn.jda.Helpers;
import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.utils.MessageHelper;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Emote;
import net.dv8tion.jda.core.entities.Guild;

import static me.melijn.jda.Melijn.PREFIX;

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
            Guild guild = event.getJDA().asBot().getShardManager().getGuildById(340081887265685504L);
            guild.retrieveEmoteById(490976816018751503L).queue(listedEmote -> sendNyanCat(event, listedEmote), failed -> sendNyanCat(event, null));
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }

    void sendNyanCat(CommandEvent event, Emote emote) {
        if (event.getGuild() == null || event.getGuild().getSelfMember().hasPermission(event.getTextChannel(), Permission.MESSAGE_EMBED_LINKS))
            event.reply(new EmbedBuilder()
                    .setColor(Helpers.EmbedColor)
                    .setDescription("Enjoy your " + (emote != null ? emote.getAsMention() : "nyancat") + " ~meow!~")
                    .setImage("https://github.com/ToxicMushroom/nyan-cats/raw/master/cat%20(" + MessageHelper.randInt(2, 33) + ").gif")
                    .build());
        else
            event.reply("Enjoy your " + (emote != null ? emote.getAsMention() : "nyancat") + " ~meow!~\n"
                    + "https://github.com/ToxicMushroom/nyan-cats/raw/master/cat%20(" + MessageHelper.randInt(2, 33) + ").gif");
    }
}
