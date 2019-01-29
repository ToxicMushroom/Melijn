package me.melijn.jda.commands.music;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.melijn.jda.audio.AudioLoader;
import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.blub.Need;
import me.melijn.jda.utils.Embedder;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;

import java.util.*;

import static me.melijn.jda.Melijn.PREFIX;

public class RemoveCommand extends Command {

    public RemoveCommand() {
        this.commandName = "remove";
        this.description = "Removes songs from the queue";
        this.usage = PREFIX + commandName + " [x-x,x]";
        this.aliases = new String[]{"delete"};
        this.category = Category.MUSIC;
        this.needs = new Need[] {Need.GUILD, Need.SAME_VOICECHANNEL};
        this.permissions = new Permission[]{Permission.MESSAGE_EMBED_LINKS};
        this.id = 73;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.hasPerm(event.getGuild().getMember(event.getAuthor()), commandName, 0)) {
            String[] args = event.getArgs().replaceAll("\\s+", "").split(",");
            if (args.length == 0 || args[0].isEmpty()) {
                event.sendUsage(this, event);
                return;
            }
            AudioLoader audioLoader = event.getClient().getMelijn().getLava().getAudioLoader();
            Queue<AudioTrack> tracks = audioLoader.getPlayer(event.getGuild()).getTrackManager().getTracks();
            Map<Integer, AudioTrack> songs = new HashMap<>();
            int i = 0;
            for (AudioTrack track : tracks) {
                i++;
                songs.put(i, track);
            }
            StringBuilder sb = new StringBuilder();
            List<String> desc = new ArrayList<>();
            for (String s : args) {
                if (s.contains("-")) {
                    if (s.matches("\\d+-\\d+")) {
                        String[] list = s.split("-");
                        if (list.length == 2) {
                            List<String> numbersBetween = getNumbersBetween(Integer.valueOf(list[0]), Integer.valueOf(list[1]));
                            for (String sl : numbersBetween) {
                                if (songs.get(Integer.valueOf(sl)) != null) {
                                    audioLoader.getPlayer(event.getGuild()).getTrackManager().tracks.remove(songs.get(Integer.valueOf(sl)));
                                    sb.append("**#").append(sl).append("**").append(" - ").append(songs.get(Integer.valueOf(sl)).getInfo().title).append("\n");
                                    desc.add("**#" + sl + "**" + " - " + songs.get(Integer.valueOf(sl)).getInfo().title + "\n");
                                }
                            }
                        } else {
                            event.reply("Wrong format!");
                            return;
                        }
                    }
                } else if (s.matches("\\d+")) {
                    if (songs.get(Integer.valueOf(s)) != null) {
                        audioLoader.getPlayer(event.getGuild()).getTrackManager().tracks.remove(songs.get(Integer.valueOf(s)));
                        sb.append("**#").append(s).append("**").append(" - ").append(songs.get(Integer.valueOf(s)).getInfo().title).append("\n");
                        desc.add("**#" + s + "**" + " - " + songs.get(Integer.valueOf(s)).getInfo().title + "\n");
                    }
                } else {
                    event.sendUsage(this, event);
                    return;
                }
            }
            if (sb.length() > 1900) {
                sb = new StringBuilder();
                int pi = 1;
                for (String s : desc) {
                    if (sb.length() < 1850) {
                        sb.append(s);
                    } else {
                        EmbedBuilder eb = new Embedder(event.getVariables(), event.getGuild());
                        eb.setTitle("Removed part **#" + pi + "**");
                        eb.setDescription(sb.toString());
                        eb.setFooter(event.getHelpers().getFooterStamp(), event.getHelpers().getFooterIcon());
                        event.reply(eb.build());
                        sb = new StringBuilder();
                        pi++;
                    }
                }
                if (sb.length() != 0) {
                    EmbedBuilder eb = new Embedder(event.getVariables(), event.getGuild());
                    eb.setTitle("Removed part **#" + pi + "**");
                    eb.setDescription(sb.toString());
                    eb.setFooter(event.getHelpers().getFooterStamp(), event.getHelpers().getFooterIcon());
                    event.reply(eb.build());
                }
            } else {
                EmbedBuilder eb = new Embedder(event.getVariables(), event.getGuild());
                eb.setTitle("Removed");
                eb.setDescription(sb.toString());
                eb.setFooter(event.getHelpers().getFooterStamp(), event.getHelpers().getFooterIcon());
                event.reply(eb.build());
            }
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }

    private List<String> getNumbersBetween(int a, int b) {
        List<String> toReturn = new ArrayList<>();
        if (a > 0 && b > 0) {
            if (a < b) {
                int c = a;
                while (c <= b) {
                    toReturn.add(String.valueOf(c));
                    c++;
                }
            } else if (a > b) {
                int d = b;
                while (a >= d) {
                    toReturn.add(String.valueOf(a));
                    d++;
                }
            } else {
                toReturn.add(String.valueOf(a));
            }
        }
        return toReturn;
    }
}
