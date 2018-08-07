package me.melijn.jda.commands.music;

import me.melijn.jda.Helpers;
import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.blub.Need;

import static me.melijn.jda.Melijn.PREFIX;

public class SummonCommand extends Command  {

    public SummonCommand() {
        this.commandName = "summon";
        this.description = "Summon's the bot to your channel";
        this.aliases = new String[]{"join", "here"};
        this.usage = PREFIX + commandName;
        this.category = Category.MUSIC;
        this.needs = new Need[]{Need.GUILD, Need.SAME_VOICECHANNEL_OR_DISCONNECTED};
    }

    @Override
    protected void execute(CommandEvent event) {
        if (Helpers.hasPerm(event.getMember(), commandName, 0)) {
            event.getGuild().getAudioManager().openAudioConnection(event.getMember().getVoiceState().getChannel());
            event.reply("I have been summoned to your channel");
        }
    }
}
