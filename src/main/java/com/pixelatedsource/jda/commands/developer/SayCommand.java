package com.pixelatedsource.jda.commands.developer;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.blub.Category;
import com.pixelatedsource.jda.blub.Command;
import com.pixelatedsource.jda.blub.CommandEvent;
import com.pixelatedsource.jda.utils.MessageHelper;
import net.dv8tion.jda.core.EmbedBuilder;

import java.util.Date;

import static com.pixelatedsource.jda.PixelSniper.PREFIX;

public class SayCommand extends Command {

    public SayCommand() {
        this.commandName = "say";
        this.description = "Makes the bot say stuff";
        this.usage = PREFIX + commandName + " <message || Embed(String title, String titleUrl, String description, String thumbnail, String image, String footerText, String footerIcon, String Author, String AuthorUrl, String AuthorIcon, boolean timestamp)>";
        this.category = Category.DEVELOPER;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (!event.getArgs().matches("\\s+") && !event.getArgs().equalsIgnoreCase("")) {
            if (event.getArgs().startsWith("Embed(")) {
                String[] embedArgs = event.getArgs().split(",");
                if (embedArgs.length == 11) {
                    embedArgs[0] = embedArgs[0].substring(6, embedArgs[0].length());
                    embedArgs[embedArgs.length - 1] = embedArgs[embedArgs.length - 1].substring(0, embedArgs[embedArgs.length - 1].length() - 1);
                    EmbedBuilder eb = new EmbedBuilder().setColor(Helpers.EmbedColor);
                    if (!embedArgs[0].equalsIgnoreCase("")) eb.setTitle(embedArgs[0], embedArgs[1]);
                    if (!embedArgs[2].equalsIgnoreCase("")) eb.setDescription(embedArgs[2]);
                    if (!embedArgs[3].equalsIgnoreCase("")) eb.setThumbnail(embedArgs[3]);
                    if (!embedArgs[4].equalsIgnoreCase("")) eb.setImage(embedArgs[4]);
                    if (!embedArgs[5].equalsIgnoreCase("")) eb.setFooter(embedArgs[5], embedArgs[6]);
                    if (!embedArgs[7].equalsIgnoreCase("")) eb.setAuthor(embedArgs[7], embedArgs[8], embedArgs[9]);
                    if (embedArgs[10].equalsIgnoreCase("true")) eb.setTimestamp(new Date(System.currentTimeMillis()).toInstant());
                    event.reply(eb.build());
                } else {
                    MessageHelper.sendUsage(this, event);
                }
            } else event.reply(event.getArgs());
        } else {
            MessageHelper.sendUsage(this, event);
        }
    }
}
