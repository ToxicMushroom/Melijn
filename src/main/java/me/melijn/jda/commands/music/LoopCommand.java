package me.melijn.jda.commands.music;

import me.melijn.jda.audio.MusicPlayer;
import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.blub.Need;
import net.dv8tion.jda.core.entities.Guild;

import java.util.Set;

import static me.melijn.jda.Melijn.PREFIX;

public class LoopCommand extends Command {


    public LoopCommand() {
        this.commandName = "loop";
        this.description = "Changes the looping state of the playing track";
        this.usage = PREFIX + commandName + " [false/off/yes | true/on/off]";
        this.aliases = new String[]{"repeat", "loopsong"};
        this.needs = new Need[]{Need.GUILD, Need.SAME_VOICECHANNEL};
        this.category = Category.MUSIC;
        this.id = 68;
    }

    @Override
    protected void execute(CommandEvent event) {
        executorLoops(this, event, event.getVariables().looped);
    }

    static void executorLoops(Command cmd, CommandEvent event, Set<Long> looped) {
        if (event.hasPerm(event.getGuild().getMember(event.getAuthor()), cmd.getCommandName(), 0)) {
            String[] args = event.getArgs().split("\\s+");
            Guild guild = event.getGuild();
            MusicPlayer musicPlayer = event.getClient().getMelijn().getLava().getAudioLoader().getPlayer(guild);
            if (musicPlayer.getTrackManager().getTrackSize() == 0 && musicPlayer.getAudioPlayer().getPlayingTrack() == null) {
                event.reply("There is no music playing");
                return;
            }
            if (args.length == 0 || args[0].isEmpty()) {
                if (looped.contains(guild.getIdLong())) {
                    looped.remove(guild.getIdLong());
                    event.reply("Looping has been **disabled**");
                } else {
                    looped.add(guild.getIdLong());
                    event.reply("Looping has been **enabled**");
                }
            } else {
                switch (args[0]) {
                    case "on":
                    case "yes":
                    case "true":
                        looped.add(guild.getIdLong());
                        event.reply("Looping has been **enabled**");
                        break;
                    case "off":
                    case "no":
                    case "false":
                        looped.remove(guild.getIdLong());
                        event.reply("Looping has been **disabled**");
                        break;
                    case "info":
                        String ts = looped.contains(guild.getIdLong()) ? "enabled" : "disabled";
                        event.reply("Looping is currently **" + ts + "**");
                        break;
                    default:
                        event.sendUsage(cmd, event);
                        break;
                }
            }
        } else {
            event.reply("You need the permission `" + cmd.getCommandName() + "` to execute this command.");
        }
    }
}
