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
        this.description = "Change the looping state or view the looping state of the playing song";
        this.usage = PREFIX + this.commandName + " [false/off/yes | true/on/off]";
        this.aliases = new String[]{"repeat", "loopsong"};
        this.category = Category.MUSIC;
    }

    public static HashMap<Long, Boolean> looped = new HashMap<>();

    @Override
    protected void execute(CommandEvent event) {
        executorLoops(this, event, looped);
    }

    static void executorLoops(Command cmd, CommandEvent event, HashMap<Long, Boolean> looped) {
        if (event.getGuild() != null) {
            if (Helpers.hasPerm(event.getGuild().getMember(event.getAuthor()), cmd.getCommandName(), 0)) {
                if (event.getGuild().getSelfMember().getVoiceState().getChannel() != null) {
                    if (event.getMember().getVoiceState().getChannel() == event.getGuild().getSelfMember().getVoiceState().getChannel()) {
                        String[] args = event.getArgs().split("\\s+");
                        Guild guild = event.getGuild();
                        if (MusicManager.getManagerinstance().getPlayer(guild).getListener().getTrackSize() > 0 || MusicManager.getManagerinstance().getPlayer(guild).getAudioPlayer().getPlayingTrack() != null) {
                            if (args.length == 0 || args[0].equalsIgnoreCase("")) {
                                if (looped.containsKey(guild.getIdLong())) {
                                    if (looped.get(guild.getIdLong())) {
                                        looped.replace(guild.getIdLong(), false);
                                        event.reply("Looping has been **disabled**");
                                    } else {
                                        looped.replace(guild.getIdLong(), true);
                                        event.reply("Looping has been **enabled**");
                                    }
                                } else {
                                    looped.put(guild.getIdLong(), true);
                                    event.reply("Looping has been **enabled**");
                                }
                            } else {
                                switch (args[0]) {
                                    case "on":
                                    case "yes":
                                    case "true":
                                        if (looped.replace(guild.getIdLong(), true) == null)
                                            looped.put(guild.getIdLong(), true);
                                        event.reply("Looping has been **enabled**");
                                        break;
                                    case "off":
                                    case "no":
                                    case "false":
                                        if (looped.replace(guild.getIdLong(), false) == null)
                                            looped.put(guild.getIdLong(), false);
                                        event.reply("Looping has been **disabled**");
                                        break;
                                    case "info":
                                        String ts = looped.getOrDefault(guild.getIdLong(), false) ? "enabled" : "disabled";
                                        event.reply("Looping is currently **" + ts + "**");
                                        break;
                                    default:
                                        MessageHelper.sendUsage(cmd, event);
                                        break;
                                }
                            }
                        } else {
                            event.reply("There is no music playing");
                        }
                    } else {
                        event.reply("You have to be in the same voice channel as me to loop");
                    }
                } else {
                    event.reply("I'm not in a voiceChannel");
                }
            } else {
                event.reply("You need the permission `" + cmd.getCommandName() + "` to execute this command.");
            }
        } else {
            event.reply(Helpers.guildOnly);
        }
    }
}
