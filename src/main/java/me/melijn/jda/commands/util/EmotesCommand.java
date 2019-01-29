package me.melijn.jda.commands.util;

import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.blub.Need;
import net.dv8tion.jda.core.entities.Emote;

import static me.melijn.jda.Melijn.PREFIX;

public class EmotesCommand extends Command {

    public EmotesCommand() {
        this.commandName = "emotes";
        this.description = "Shows you all the emotes in a server";
        this.usage = PREFIX + commandName;
        this.category = Category.UTILS;
        this.needs = new Need[]{Need.GUILD};
        this.id = 106;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.hasPerm(event.getMember(), commandName, 0)) {
            StringBuilder sb = new StringBuilder("**Emotes:**\n");
            for (Emote emote : event.getGuild().getEmoteCache()) {
                sb.append(emote.getAsMention()).append("   ").append(emote.getName()).append("   `").append(emote.getId()).append("`\n");
            }
            if (sb.length() > 13)
                event.getMessageHelper().sendSplitMessage(event.getTextChannel(), sb.toString());
            else event.reply("This server as no emotes :/");
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
