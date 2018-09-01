package me.melijn.jda.commands.music;

import me.melijn.jda.Helpers;
import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.blub.Need;
import me.melijn.jda.music.MusicManager;
import me.melijn.jda.music.MusicPlayer;
import me.melijn.jda.utils.MessageHelper;

import static me.melijn.jda.Melijn.PREFIX;

public class NightCoreCommand extends Command {

    public NightCoreCommand() {
        this.commandName = "nightcore";
        this.description = "Toggle the nightcore mode";
        this.usage = PREFIX + commandName + " <on/enable/true | off/disable/false>";
        this.category = Category.MUSIC;
        this.needs = new Need[]{Need.GUILD, Need.SAME_VOICECHANNEL};
        this.extra = "Sets speed to 1.25 and pitch to 1.20";
    }

    @Override
    protected void execute(CommandEvent event) {
        if (Helpers.hasPerm(event.getMember(), commandName, 0)) {
            String[] args = event.getArgs().split("\\s+");
            MusicPlayer musicPlayer = MusicManager.getManagerInstance().getPlayer(event.getGuild());
            if (args.length == 1) {
                switch (args[0]) {
                    case "true":
                    case "on":
                    case "enabled":
                        musicPlayer.setSpeed(1.25);
                        musicPlayer.setPitch(1.20);
                        musicPlayer.updateFilters();
                        event.reply("The NightCore filter has been **enabled** by **" + event.getFullAuthorName() + "**");
                        break;
                    case "false":
                    case "off":
                    case "disabled":
                        musicPlayer.setSpeed(1);
                        musicPlayer.setPitch(1);
                        musicPlayer.updateFilters();
                        event.reply("The NightCore filter has been **disabled** by **" + event.getFullAuthorName() + "**");
                        break;
                    default:
                        MessageHelper.sendUsage(this, event);
                        break;
                }
            } else {
                MessageHelper.sendUsage(this, event);
            }
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
