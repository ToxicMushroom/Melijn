package me.melijn.jda.commands.music;

import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.blub.Need;

import static me.melijn.jda.Melijn.PREFIX;

public class SummonCommand extends Command {

    public SummonCommand() {
        this.commandName = "summon";
        this.description = "Summons the bot to your channel";
        this.aliases = new String[]{"join", "here"};
        this.usage = PREFIX + commandName;
        this.category = Category.MUSIC;
        this.needs = new Need[]{Need.GUILD, Need.SAME_VOICECHANNEL_OR_DISCONNECTED};
        this.id = 12;
    }


    @Override
    protected void execute(CommandEvent event) {
        if (event.hasPerm(event.getMember(), commandName, 0)) {
            if (event.getClient().getMelijn().getLava().tryToConnectToVC(event, event.getGuild(), event.getMember().getVoiceState().getChannel())) {
                event.reply("I have joined your channel");
            }
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
