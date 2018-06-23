package com.pixelatedsource.jda.commands.music;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.blub.Category;
import com.pixelatedsource.jda.blub.Command;
import com.pixelatedsource.jda.blub.CommandEvent;
import com.pixelatedsource.jda.music.MusicManager;
import com.pixelatedsource.jda.music.MusicPlayer;
import com.pixelatedsource.jda.utils.MessageHelper;

import static com.pixelatedsource.jda.PixelSniper.PREFIX;

public class NightCoreCommand extends Command {

    public NightCoreCommand() {
        this.commandName = "nightcore";
        this.description = "Toggle the nightcore mode";
        this.usage = PREFIX + commandName + " <on/enable/true | off/disable/false>";
        this.category = Category.MUSIC;
        this.extra = "Sets speed to 1.25 and pitch to 1.20";
    }


    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() != null) {
            if (Helpers.hasPerm(event.getMember(), commandName, 0)) {
                if (event.getMember().getVoiceState().getChannel() == event.getGuild().getSelfMember().getVoiceState().getChannel() && event.getMember().getVoiceState().getChannel() != null) {
                    String[] args = event.getArgs().split("\\s+");
                    MusicPlayer musicPlayer = MusicManager.getManagerinstance().getPlayer(event.getGuild());
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
                    event.reply("You have to be in the same channel as the bot");
                }
            } else {
                event.reply("You need the permission `" + commandName + "` to execute this command.");
            }
        } else {
            event.reply(Helpers.guildOnly);
        }
    }
}
