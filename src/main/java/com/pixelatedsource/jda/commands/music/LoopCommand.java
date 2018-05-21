package com.pixelatedsource.jda.commands.music;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.blub.Category;
import com.pixelatedsource.jda.blub.Command;
import com.pixelatedsource.jda.blub.CommandEvent;
import com.pixelatedsource.jda.music.MusicManager;
import com.pixelatedsource.jda.utils.MessageHelper;
import net.dv8tion.jda.core.entities.Guild;

import java.util.HashMap;

import static com.pixelatedsource.jda.PixelSniper.PREFIX;

public class LoopCommand extends Command {

    public LoopCommand() {
        this.commandName = "loop";
        this.description = "Change the looping state or view the looping state";
        this.usage = PREFIX + this.commandName + " [false|true]";
        this.aliases = new String[]{"repeat"};
        this.category = Category.MUSIC;
    }

    public static HashMap<String, Boolean> looped = new HashMap<>();

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() != null) {
            if (Helpers.hasPerm(event.getGuild().getMember(event.getAuthor()), this.commandName, 0)) {
                String[] args = event.getArgs().split("\\s+");
                Guild guild = event.getGuild();
                if (MusicManager.getManagerinstance().getPlayer(guild).getListener().getTrackSize() > 0 || MusicManager.getManagerinstance().getPlayer(guild).getAudioPlayer().getPlayingTrack() != null) {
                    if (args.length == 0 || args[0].equalsIgnoreCase("")) {
                        if (looped.containsKey(guild.getId())) {
                            if (looped.get(guild.getId())) {
                                looped.put(guild.getId(), false);
                                event.reply("Looping has been **disabled**");
                            } else {
                                looped.put(guild.getId(), true);
                                event.reply("Looping has been **enabled**");
                            }
                        } else {
                            looped.put(guild.getId(), true);
                            event.reply("Looping has been **enabled**");
                        }
                    } else {
                        switch (args[0]) {
                            case "on":
                            case "yes":
                            case "true":
                                if (looped.containsKey(guild.getId())) looped.replace(guild.getId(), true);
                                else looped.put(guild.getId(), true);
                                event.reply("Looping has been **enabled**");
                                break;
                            case "off":
                            case "no":
                            case "false":
                                if (looped.containsKey(guild.getId())) looped.replace(guild.getId(), false);
                                else looped.put(guild.getId(), false);
                                event.reply("Looping has been **disabled**");
                                break;
                            case "info":
                                String ts = looped.get(guild.getId()) == null || !looped.get(guild.getId()) ? "off" : "on";
                                event.reply("The state of looping is '" + ts + "'");
                                break;
                            default:
                                MessageHelper.sendUsage(this, event);
                                break;
                        }
                    }
                } else {
                    event.reply("No music playing atm!");
                }
            } else {
                event.reply("You need the permission `" + commandName + "` to execute this command.");
            }
        } else {
            event.reply(Helpers.guildOnly);
        }
    }
}
