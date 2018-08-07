package me.melijn.jda.utils;

import me.melijn.jda.Helpers;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.commands.management.SetPrefixCommand;
import me.melijn.jda.commands.music.NowPlayingCommand;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Emote;
import net.dv8tion.jda.core.entities.User;
import okhttp3.HttpUrl;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Random;

import static me.melijn.jda.Melijn.PREFIX;

public class MessageHelper {

    public static HashMap<String, String> filterDeletedMessages = new HashMap<>();
    public static HashMap<String, User> purgedMessages = new HashMap<>();

    public static String millisToDate(long millis) {
        Calendar start = Calendar.getInstance();
        start.setTimeInMillis(millis);
        int mYear = start.get(Calendar.YEAR);
        int mMonth = start.get(Calendar.MONTH);
        int mDay = start.get(Calendar.DAY_OF_MONTH);
        int mHour = start.get(Calendar.HOUR_OF_DAY);
        int mMinutes = start.get(Calendar.MINUTE);
        int mSeconds = start.get(Calendar.SECOND);
        return String.valueOf(mHour) + ":" + mMinutes + ":" + mSeconds + "s " + mDay + "/" + mMonth + "/" + mYear;
    }

    public static boolean isRightFormat(String string) {
        return string.matches("\\d++[smhdwMy]");
    }

    public static long easyFormatToSeconds(String string) {
        if (string.matches("\\d++[s]")) {
            return Long.parseLong(string.replaceAll("s", ""));
        }
        if (string.matches("\\d++[m]")) {
            return Long.parseLong(string.replaceAll("m", ""))*60;
        }
        if (string.matches("\\d++[h]")) {
            return Long.parseLong(string.replaceAll("h", ""))*3600;
        }
        if (string.matches("\\d++[d]")) {
            return Long.parseLong(string.replaceAll("d", ""))*86_400;
        }
        if (string.matches("\\d++[w]")) {
            return Long.parseLong(string.replaceAll("w", ""))*604_800;
        }
        if (string.matches("\\d++[M]")) {
            return Long.parseLong(string.replaceAll("M", ""))*18_144_000;
        }
        if (string.matches("\\d++[y]")) {
            return Long.parseLong(string.replaceAll("y", ""))*217_728_000;
        }
        System.exit(86400);
        return 0;
    }

    public static void sendUsage(Command cmd, CommandEvent event) {
        if (SetPrefixCommand.prefixes.containsKey(event.getGuild().getIdLong()))
            event.reply(cmd.getUsage().replaceFirst(PREFIX, SetPrefixCommand.prefixes.get(event.getGuild().getIdLong())));
        else
            event.reply(cmd.getUsage());
    }

    public static String millisToVote(long untilNext) {
        String hours = String.valueOf(untilNext/3600000);
        untilNext -= (untilNext/3600000) * 3600000;
        String minutes = String.valueOf(untilNext/60000);
        untilNext -= (untilNext/60000) * 60000;
        String seconds = String.valueOf(untilNext/1000);
        return hours + ":" + minutes + ":" + seconds + "s";
    }

    public static String progressBar(AudioTrack track, Emote emote) {
        double procent = ((double)track.getPosition()/(double)track.getDuration())*100D;
        int procentInt = (int) (procent/1);
        String s = "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬";
        StringBuilder sb = new StringBuilder();
        while (procentInt > 0) {
            procentInt -= 5;
            sb.append("▬");
        }

        return "[" + sb.toString() + "](https://melijn.com/)" + (emote != null ? emote.getAsMention() : "") + s.substring(0, (20-sb.toString().length())) + " (" + Helpers.getDurationBreakdown(track.getPosition()) + "/" + Helpers.getDurationBreakdown(track.getDuration()) + ")";
    }

    public static String getThumbnailURL(String url) {
        HttpUrl httpUrl = HttpUrl.parse(url);
        if (httpUrl != null) {
            if (url.matches(NowPlayingCommand.youtubePattern.pattern())) {
                if (httpUrl.queryParameter("v") != null) {
                    return "https://img.youtube.com/vi/" + httpUrl.queryParameter("v") + "/hqdefault.jpg";
                }
            } else if (url.matches(NowPlayingCommand.youtuBePattern.pattern())) {
                return "https://img.youtube.com/vi/" + url.replaceFirst(NowPlayingCommand.youtuBePattern.pattern(), "") + "/hqdefault.jpg";
            }
        }
        return null;
    }

    public static void sendFunText(String desc, String url, CommandEvent event) {
        if (url == null) url = "https://melijn.com/files/u/07-05-2018--19.42-08s.png";
        if (event.getGuild() == null || event.getGuild().getSelfMember().hasPermission(Permission.MESSAGE_EMBED_LINKS)) {
            event.reply(new EmbedBuilder()
                    .setDescription(desc)
                    .setColor(Helpers.EmbedColor)
                    .setImage(url)
                    .setFooter("Powered by weeb.sh & weeb.java", null)
                    .build());
        } else {
            event.reply(desc + "\n" + url + "Powered by weeb.sh & weeb.java");
        }
    }

    public static int randInt(int start, int end) {
        Random random = new Random();
        return random.nextInt(end + 1 - start) + start;
    }

    public static String capFirstChar(String input) {
        return input.substring(0, 1).toUpperCase() + input.substring(1);
    }

}
