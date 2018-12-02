package me.melijn.jda.commands.util;

import me.melijn.jda.Helpers;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.blub.Need;
import me.melijn.jda.utils.MessageHelper;
import net.dv8tion.jda.core.entities.Emote;

import static me.melijn.jda.Melijn.PREFIX;

public class EmotesCommand extends Command {

    public EmotesCommand() {
        this.commandName = "emotes";
        this.description = "Shows you all the emotes in a server";
        this.usage = PREFIX + commandName;
        this.needs = new Need[]{Need.GUILD};
    }

    @Override
    protected void execute(CommandEvent event) {
        if (Helpers.hasPerm(event.getMember(), commandName, 0)) {
            StringBuilder sb = new StringBuilder("**Emotes:**\n");
            for (Emote emote : event.getGuild().getEmoteCache()) {
                sb.append(emote.getAsMention()).append("   ").append(emote.getName()).append("   `").append(emote.getId()).append("`\n");
            }
            if (sb.toString().length() > 13)
                MessageHelper.sendSplitMessage(event.getTextChannel(), sb.toString());
            else event.reply("This server as no emotes :/");
        }
    }
}
