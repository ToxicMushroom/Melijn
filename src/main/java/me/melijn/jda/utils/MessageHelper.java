package me.melijn.jda.utils;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import gnu.trove.list.TLongList;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.map.TLongLongMap;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongLongHashMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import me.melijn.jda.Helpers;
import me.melijn.jda.Melijn;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.commands.management.SetPrefixCommand;
import me.melijn.jda.commands.music.NowPlayingCommand;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import okhttp3.HttpUrl;
import org.apache.commons.text.StringEscapeUtils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static me.melijn.jda.Melijn.PREFIX;

public class MessageHelper {

    public static TLongObjectMap<String> filteredMessageDeleteCause = new TLongObjectHashMap<>();
    public static TLongLongMap purgedMessageDeleter = new TLongLongHashMap();
    public static TLongList botDeletedMessages = new TLongArrayList();
    public static String spaces = "                                                                                                    ";
    private static final Pattern prefixPattern = Pattern.compile(PREFIX);

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

    public static long dateToMillis(String date) {
        Matcher matcher = Pattern.compile("(\\d+):(\\d+):(\\d+)s (\\d+)/(\\d+)/(\\d+)").matcher(date);
        if (!matcher.find()) return -1;
        int hour = matcher.group(1).length() < 3 ? Integer.parseInt(matcher.group(1)) : -1;
        int minutes = matcher.group(2).length() < 3 ? Integer.parseInt(matcher.group(2)) : -1;
        int seconds = matcher.group(3).length() < 3 ? Integer.parseInt(matcher.group(3)) : -1;

        int day = matcher.group(4).length() < 3 ? Integer.parseInt(matcher.group(4)) : -1;
        int month = matcher.group(5).length() < 3 ? Integer.parseInt(matcher.group(5)) : -1;
        int year = matcher.group(6).length() < 5 ? Integer.parseInt(matcher.group(6)) : -1;
        if (hour > 23 || hour == -1 ||
                minutes > 59 || minutes == -1 ||
                seconds > 59 || seconds == -1 ||
                day > 31 || day == -1 ||
                month > 12 || month < 0 ||
                year < 2016)
            return -1;

        Calendar start = Calendar.getInstance();
        start.set(year, month, day, hour, minutes, seconds);
        return start.getTimeInMillis();
    }

    public static boolean isRightFormat(String string) {
        return string.matches("\\d{0,18}[smhdwMy]");
    }

    public static long easyFormatToSeconds(String string) {
        if (string.matches("\\d{0,18}[s]")) {
            return Long.parseLong(string.replaceAll("s", ""));
        }
        if (string.matches("\\d{0,18}[m]")) {
            return Long.parseLong(string.replaceAll("m", "")) * 60;
        }
        if (string.matches("\\d{0,18}[h]")) {
            return Long.parseLong(string.replaceAll("h", "")) * 3600;
        }
        if (string.matches("\\d{0,18}[d]")) {
            return Long.parseLong(string.replaceAll("d", "")) * 86_400;
        }
        if (string.matches("\\d{0,18}[w]")) {
            return Long.parseLong(string.replaceAll("w", "")) * 604_800;
        }
        if (string.matches("\\d{0,18}[M]")) {
            return Long.parseLong(string.replaceAll("M", "")) * 18_144_000;
        }
        if (string.matches("\\d{0,18}[y]")) {
            return Long.parseLong(string.replaceAll("y", "")) * 217_728_000;
        }
        return 0;
    }

    public static void sendUsage(Command cmd, CommandEvent event) {
        event.reply(prefixPattern.matcher(cmd.getUsage()).replaceFirst(StringEscapeUtils.escapeJava(SetPrefixCommand.prefixes.getUnchecked(event.getGuild().getIdLong()))));
    }

    public static String millisToVote(long untilNext) {
        long temp = untilNext;
        String hours = String.valueOf(temp / 3600000);
        temp -= (temp / 3600000) * 3600000;
        String minutes = String.valueOf(temp / 60000);
        temp -= (temp / 60000) * 60000;
        String seconds = String.valueOf(temp / 1000);
        return hours + ":" + minutes + ":" + seconds + "s";
    }

    public static String progressBar(AudioTrack track) {
        if (track.getInfo().isStream || track.getPosition() > track.getDuration()) {
            return "**" + Helpers.getDurationBreakdown(track.getPosition()) + " | \uD83D\uDD34 Live**";
        }
        int percent = (int) (((double) track.getPosition() / (double) track.getDuration()) * 18D);
        StringBuilder sb = new StringBuilder("▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        sb.insert(percent, "](https://melijn.com/)<a:cool_nyan:490978764264570894>");
        sb.append(" **").append(Helpers.getDurationBreakdown(track.getPosition())).append("/").append(Helpers.getDurationBreakdown(track.getDuration())).append("**");
        sb.insert(0, "[");
        return sb.toString();
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
        String tempUrl = url;
        if (tempUrl == null) tempUrl = "https://melijn.com/files/u/07-05-2018--19.42-08s.png";
        if (event.getGuild() == null || event.getGuild().getSelfMember().hasPermission(event.getTextChannel(), Permission.MESSAGE_EMBED_LINKS)) {
            event.reply(new Embedder(event.getGuild())
                    .setDescription(desc)
                    .setImage(tempUrl)
                    .setFooter("Powered by weeb.sh & weeb.java", null)
                    .build());
        } else {
            event.reply(desc + "\n" + tempUrl + "Powered by weeb.sh & weeb.java");
        }
    }

    public static int randInt(int start, int end) {
        Random random = new Random();
        return random.nextInt(end + 1 - start) + start;
    }

    public static String capFirstChar(String input) {
        return input.substring(0, 1).toUpperCase() + input.substring(1);
    }


    public static void printException(final Thread thread, final Throwable ex, Guild guild, final MessageChannel channel) {
        StringWriter writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);

        if (guild != null)
            printWriter.write("**Guild**: " + guild.getName() + " | " + guild.getIdLong() + "\r\n");
        else if (channel.getType() == ChannelType.PRIVATE) {
            final PrivateChannel privateChannel = (PrivateChannel) channel;
            printWriter.write("**Direct-Message**: " + privateChannel.getName() + " | " + privateChannel.getUser().getIdLong() + "\r\n");
        }
        if (thread != null) printWriter.write("**Thread**: " + thread.getName() + "\r\n");

        ex.printStackTrace(printWriter);
        String message = writer.toString().replaceAll("me\\.melijn\\.jda", "**me.melijn.jda**");
        final List<String> messages = getSplitMessage(message);
        messages.forEach(msg -> Melijn.getShardManager().getGuildById(340081887265685504L).getTextChannelById(486042641692360704L).sendMessage(msg).queue());
    }

    public static List<String> getSplitMessage(String message) {
        final List<String> messages = new ArrayList<>();
        while (message.length() > 2000) {
            final String findLastNewline = message.substring(0, 2000);
            int index = findLastNewline.lastIndexOf("\n");
            if (index < 1800) {
                index = findLastNewline.lastIndexOf(".");
            }
            if (index < 1800) {
                index = findLastNewline.lastIndexOf(" ");
            }
            if (index < 1800) {
                index = 1999;
            }
            messages.add(message.substring(0, index));
            message = message.substring(index);
        }
        if (message.length() > 0)
            messages.add(message);
        return messages;
    }

    public static void sendSplitMessage(TextChannel channel, String text) {
        final List<String> messages = getSplitMessage(text);
        messages.forEach(message -> channel.sendMessage(message).queue());
    }
}
