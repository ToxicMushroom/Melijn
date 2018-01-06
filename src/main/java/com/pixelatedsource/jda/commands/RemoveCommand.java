package com.pixelatedsource.jda.commands;

import com.jagrosh.jdautilities.commandclient.Command;
import com.jagrosh.jdautilities.commandclient.CommandEvent;
import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.music.MusicManager;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.BlockingQueue;

import static net.dv8tion.jda.core.Permission.MESSAGE_EMBED_LINKS;

public class RemoveCommand extends Command {
    public RemoveCommand() {
        this.name = "remove";
        this.aliases = new String[]{"delete"};
        this.help = "Remove songs of the queue";
        this.guildOnly = true;
        this.botPermissions = new Permission[] {MESSAGE_EMBED_LINKS};
    }

    private MusicManager manager = MusicManager.getManagerinstance();

    @Override
    protected void execute(CommandEvent event) {
        if (Helpers.hasPerm(event.getGuild().getMember(event.getAuthor()), this.name, 0)) {
            BlockingQueue<AudioTrack> tracks = manager.getPlayer(event.getGuild()).getListener().getTracks();
            String[] args = event.getArgs().replaceAll("\\s+", "").split(",");
            HashMap<Integer, AudioTrack> songs = new HashMap<>();
            int i = 0;
            for (AudioTrack track : tracks) {
                i++;
                songs.put(i, track);
            }
            StringBuilder sb = new StringBuilder();
            ArrayList<String> desc = new ArrayList<>();
            for (String s : args) {
                if (s.contains("-")) {
                    if (s.matches("\\d+-\\d+")) {
                        String[] list = s.split("-");
                        if (list.length == 2) {
                            ArrayList<String> numbersBetween = getNumbersBetween(Integer.valueOf(list[0]), Integer.valueOf(list[1]));
                            for (String sl : numbersBetween) {
                                if (songs.get(Integer.valueOf(sl)) != null) {
                                    manager.getPlayer(event.getGuild()).getListener().tracks.remove(songs.get(Integer.valueOf(sl)));
                                    sb.append("**#").append(sl).append("**").append(" - ").append(songs.get(Integer.valueOf(sl)).getInfo().title).append("\n");
                                    desc.add("**#" + sl + "**" + " - " + songs.get(Integer.valueOf(sl)).getInfo().title + "\n");
                                }
                            }
                        } else {
                            event.reply("Wrong format!");
                        }
                    }
                } else if (s.matches("\\d+")) {
                    if (songs.get(Integer.valueOf(s)) != null) {
                        manager.getPlayer(event.getGuild()).getListener().tracks.remove(songs.get(Integer.valueOf(s)));
                        sb.append("**#").append(s).append("**").append(" - ").append(songs.get(Integer.valueOf(s)).getInfo().title).append("\n");
                        desc.add("**#" + s + "**" + " - " + songs.get(Integer.valueOf(s)).getInfo().title + "\n");
                    }
                } else {
                    event.reply("Wrong arguments check the site for more info: http://pixelnetwork.be/commands");
                }
            }
            if (sb.toString().length() > 1900) {
                sb = new StringBuilder();
                int pi = 1;
                for (String s : desc) {
                    if (sb.toString().length() < 1850) {
                        sb.append(s);
                    } else {
                        EmbedBuilder eb = new EmbedBuilder();
                        eb.setTitle("Removed part **#" + pi + "**");
                        eb.setColor(Helpers.EmbedColor);
                        eb.setDescription(sb.toString());
                        eb.setFooter(Helpers.getFooterStamp(), Helpers.getFooterIcon());
                        event.reply(eb.build());
                        sb = new StringBuilder();
                        pi++;
                    }
                }
                if (sb.toString().length() != 0) {
                    EmbedBuilder eb = new EmbedBuilder();
                    eb.setTitle("Removed part **#" + pi + "**");
                    eb.setColor(Helpers.EmbedColor);
                    eb.setDescription(sb.toString());
                    eb.setFooter(Helpers.getFooterStamp(), Helpers.getFooterIcon());
                    event.reply(eb.build());
                }
            } else {
                EmbedBuilder eb = new EmbedBuilder();
                eb.setTitle("Removed");
                eb.setColor(Helpers.EmbedColor);
                eb.setDescription(sb.toString());
                eb.setFooter(Helpers.getFooterStamp(), Helpers.getFooterIcon());
                event.reply(eb.build());
            }
        }
    }

    private ArrayList<String> getNumbersBetween(int a, int b) {
        ArrayList<String> toReturn = new ArrayList<>();
        if (a > 0 && b > 0) {
            if (a < b) {
                int c = a;
                while (c <= b) {
                    toReturn.add(String.valueOf(c));
                    c++;
                }
            } else if (a > b) {
                int c = a;
                int d = b;
                while (c >= d) {
                    toReturn.add(String.valueOf(c));
                    d++;
                }
            } else if (a == b) {
                toReturn.add(String.valueOf(a));
            }
        }
        return toReturn;
    }
}
