package com.pixelatedsource.jda.commands.fun;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.blub.Category;
import com.pixelatedsource.jda.blub.Command;
import com.pixelatedsource.jda.blub.CommandEvent;
import com.pixelatedsource.jda.utils.WebUtils;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;

import static com.pixelatedsource.jda.PixelSniper.PREFIX;

public class TriggeredCommand extends Command {

    public TriggeredCommand() {
        this.commandName = "triggered";
        this.description = "Will visualize your triggered state to other people";
        this.usage = PREFIX + commandName;
        this.aliases = new String[]{"rage"};
        this.category = Category.FUN;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() == null || Helpers.hasPerm(event.getMember(), this.commandName, 0)) {
            if (event.getGuild() == null || event.getGuild().getSelfMember().hasPermission(Permission.MESSAGE_EMBED_LINKS))
                event.reply(new EmbedBuilder()
                        .setColor(Helpers.EmbedColor)
                        .setDescription("**TRIGGERED**")
                        .setImage(WebUtils.getUrl("triggered"))
                        .build());
            else
                event.reply("**TRIGGERED** \n" + WebUtils.getUrl("triggered"));
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
