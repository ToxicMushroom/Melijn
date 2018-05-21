package com.pixelatedsource.jda.commands.music;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.PixelSniper;
import com.pixelatedsource.jda.blub.Category;
import com.pixelatedsource.jda.blub.Command;
import com.pixelatedsource.jda.blub.CommandEvent;
import com.pixelatedsource.jda.utils.MessageHelper;
import net.dv8tion.jda.core.entities.Guild;

import java.util.HashMap;

import static com.pixelatedsource.jda.PixelSniper.PREFIX;

public class SetStreamUrlCommand extends Command {


    public SetStreamUrlCommand() {
        this.commandName = "setstreamurl";
        this.description = "set the stream url of the bot";
        this.usage = PREFIX + commandName + " [stream url]";
        this.aliases = new String[]{"ssu"};
        this.category = Category.MUSIC;
    }

    private HashMap<String, String> linkjes = new HashMap<String, String>() {{
        put("slam-nonstop", "http://stream.radiocorp.nl/web10_mp3");
        put("radio538", "http://18973.live.streamtheworld.com/RADIO538.mp3");
        put("Joe-fm", "http://icecast-qmusic.cdp.triple-it.nl/JOEfm_be_live_128.mp3");
        put("mnm", "http://icecast.vrtcdn.be/mnm-high.mp3");
        put("mnm-hits", "http://icecast.vrtcdn.be/mnm_hits-high.mp3");
        put("Q-music", "http://icecast-qmusic.cdp.triple-it.nl/Qmusic_be_live_64.aac");
        put("Nostalgie", "http://nostalgiewhatafeeling.ice.infomaniak.ch/nostalgiewhatafeeling-128.mp3");
        put("Radio1", "http://icecast.vrtcdn.be/radio1-high.mp3");
        put("Radio2", "http://icecast.vrtcdn.be/ra2wvl-high.mp3");
        put("Studio-Brussel", "http://icecast.vrtcdn.be/stubru-high.mp3");
    }};

    public static HashMap<Long, String> streamUrls = PixelSniper.mySQL.getStreamUrlMap();

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() != null) {
            if (Helpers.hasPerm(event.getMember(), commandName, 1)) {
                Guild guild = event.getGuild();
                String[] args = event.getArgs().split("\\s+");
                String url = streamUrls.getOrDefault(guild.getId(), "null");
                if (args.length == 0 || args[0].equalsIgnoreCase("")) {
                    event.reply(url);
                } else if (args.length == 1) {
                    if (args[0].contains("http://") || args[0].contains("https://")) {
                        new Thread(() -> PixelSniper.mySQL.setStreamUrl(guild.getIdLong(), args[0])).start();
                        if (streamUrls.containsKey(guild.getIdLong())) streamUrls.replace(guild.getIdLong(), args[0]);
                        else streamUrls.put(guild.getIdLong(), args[0]);
                        event.reply("Changed the url from " + url + " to " + args[0]);
                    } else {
                        if (args[0].equalsIgnoreCase("list")) {
                            event.reply("**Radio**\n" + linkjes.keySet().toString().replaceAll("(,\\s+|,)", "\n+ ").replaceFirst("\\[", "+ ").replaceFirst("]", ""));
                        } else {
                            if (linkjes.keySet().contains(args[0])) {
                                new Thread(() -> PixelSniper.mySQL.setStreamUrl(guild.getIdLong(), linkjes.get(args[0]))).start();
                                if (streamUrls.containsKey(guild.getIdLong())) streamUrls.replace(guild.getIdLong(), linkjes.get(args[0]));
                                else streamUrls.put(guild.getIdLong(), linkjes.get(args[0]));
                                event.reply("Changed the url from " + url + " to " + linkjes.get(args[0]));
                            } else {
                                MessageHelper.sendUsage(this, event);
                            }
                        }
                    }
                } else {
                    MessageHelper.sendUsage(this, event);
                }
            } else {
                event.reply("You need the permission `" + commandName + "` to execute this command.");
            }
        } else {
            event.reply(Helpers.guildOnly);
        }
    }
}
