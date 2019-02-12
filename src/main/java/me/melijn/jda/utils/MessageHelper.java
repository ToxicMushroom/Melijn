package me.melijn.jda.utils;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import lavalink.client.player.LavalinkPlayer;
import me.melijn.jda.Melijn;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import okhttp3.HttpUrl;
import org.apache.commons.text.StringEscapeUtils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class MessageHelper {

    private final Pattern youtubePattern = Pattern.compile("^((?:https?:)?//)?((?:www|m)\\.)?((?:youtube\\.com))/watch(.*?)");
    private final Pattern youtuBePattern = Pattern.compile("^((?:https?:)?//)?((?:www|m)\\.)?((?:youtu\\.be/))(.*?)");
    private final Pattern datePattern = Pattern.compile("(\\d+):(\\d+):(\\d+)s (\\d+)/(\\d+)/(\\d+)");
    private static final Random random = new Random();
    private final Pattern prefixPattern;
    private final Melijn melijn;

    public MessageHelper(Melijn melijn) {
        this.melijn = melijn;
        prefixPattern = Pattern.compile(Melijn.PREFIX);
    }

    public String millisToDate(long millis) {
        LocalDateTime dateTime = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDateTime();
        int mYear = dateTime.getYear();
        int mMonth = dateTime.getMonthValue();
        int mDay = dateTime.getDayOfMonth();
        int mHour = dateTime.getHour();
        int mMinutes = dateTime.getMinute();
        int mSeconds = dateTime.getSecond();
        return mHour + ":" + mMinutes + ":" + mSeconds + "s " + mDay + "/" + mMonth + "/" + mYear;
    }

    public long dateToMillis(String date) {
        Matcher matcher = datePattern.matcher(date);
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

        return LocalDateTime.of(year, month, day, hour, minutes, seconds).atZone(ZoneId.systemDefault()).toEpochSecond() * 1000;
    }

    public boolean isWrongFormat(String string) {
        return !string.matches("\\d{0,18}[smhdwMy]");
    }

    public long easyFormatToSeconds(String string) {
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

    public void sendUsage(Command cmd, CommandEvent event) {
        event.reply(prefixPattern.matcher(cmd.getUsage()).replaceFirst(StringEscapeUtils.escapeJava(event.getVariables().prefixes.getUnchecked(event.getGuild().getIdLong()))));
    }

    public String millisToVote(long untilNext) {
        long temp = untilNext;
        String hours = String.valueOf(temp / 3600000);
        temp -= (temp / 3600000) * 3600000;
        String minutes = String.valueOf(temp / 60000);
        temp -= (temp / 60000) * 60000;
        String seconds = String.valueOf(temp / 1000);
        return hours + ":" + minutes + ":" + seconds + "s";
    }

    public String progressBar(LavalinkPlayer player) {
        AudioTrack track = player.getPlayingTrack();
        if (track.getInfo().isStream) {
            return "**" + getDurationBreakdown(player.getTrackPosition()) + " | \uD83D\uDD34 Live**";
        }
        int percent = (int) (((double) player.getTrackPosition() / (double) track.getDuration()) * 18D);
        StringBuilder sb = new StringBuilder("▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        sb.insert(percent, "](https://melijn.com/)<a:cool_nyan:490978764264570894>");
        sb.append(" **").append(getDurationBreakdown(player.getTrackPosition())).append("/").append(getDurationBreakdown(track.getDuration())).append("**");
        sb.insert(0, "[");
        return sb.toString();
    }

    public String getDurationBreakdown(long milliseconds) {
        long millis = milliseconds;
        if (millis < 0L) {
            return "error";
        }
        if (millis > 43200000000L) return "LIVE";
        long days = TimeUnit.MILLISECONDS.toDays(millis);
        millis -= TimeUnit.DAYS.toMillis(days);
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        millis -= TimeUnit.HOURS.toMillis(hours);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        millis -= TimeUnit.MINUTES.toMillis(minutes);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);

        StringBuilder sb = new StringBuilder(64);
        if (days != 0) {
            sb.append(days);
            sb.append("d ");
        }
        appendTimePart(hours, sb);
        appendTimePart(minutes, sb);
        if (seconds < 10) sb.append(0);
        sb.append(seconds);
        sb.append("s");

        return (sb.toString());
    }

    private void appendTimePart(long hours, StringBuilder sb) {
        if (hours != 0) {
            if (hours < 10) sb.append(0);
            sb.append(hours);
            sb.append(":");
        }
    }

    public String getThumbnailURL(String url) {
        HttpUrl httpUrl = HttpUrl.parse(url);
        if (httpUrl != null) {
            if (url.matches(youtubePattern.pattern())) {
                if (httpUrl.queryParameter("v") != null) {
                    return "https://img.youtube.com/vi/" + httpUrl.queryParameter("v") + "/hqdefault.jpg";
                }
            } else if (url.matches(youtuBePattern.pattern())) {
                return "https://img.youtube.com/vi/" + url.replaceFirst(youtuBePattern.pattern(), "") + "/hqdefault.jpg";
            }
        }
        return null;
    }

    public void sendFunText(String desc, String url, CommandEvent event) {
        String tempUrl = url;
        if (tempUrl == null) tempUrl = "https://melijn.com/files/u/07-05-2018--19.42-08s.png";
        if (event.getGuild() == null || event.getGuild().getSelfMember().hasPermission(event.getTextChannel(), Permission.MESSAGE_EMBED_LINKS)) {
            event.reply(new Embedder(melijn.getVariables(), event.getGuild())
                    .setDescription(desc)
                    .setImage(tempUrl)
                    .setFooter("Powered by weeb.sh & weeb.java", null)
                    .build());
        } else {
            event.reply(desc + "\n" + tempUrl + "Powered by weeb.sh & weeb.java");
        }
    }

    public int randInt(int start, int end) {
        return random.nextInt(end + 1 - start) + start;
    }

    public String capFirstChar(String input) {
        return input.substring(0, 1).toUpperCase() + input.substring(1);
    }


    public void printException(final Thread thread, final Throwable ex, Guild guild, final MessageChannel channel) {
        StringWriter writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);

        if (guild != null)
            printWriter.write("**Guild**: " + guild.getName() + " | " + guild.getIdLong() + "\r\n");
        else if (channel != null && channel.getType() == ChannelType.PRIVATE) {
            final PrivateChannel privateChannel = (PrivateChannel) channel;
            printWriter.write("**Direct-Message**: " + privateChannel.getName() + " | " + privateChannel.getUser().getIdLong() + "\r\n");
        }
        if (thread != null) printWriter.write("**Thread**: " + thread.getName() + "\r\n");
        if (thread != null && melijn.getVariables().unLoggedThreads.contains(thread.getName())) return;

        ex.printStackTrace(printWriter);
        String message = writer.toString().replaceAll("me\\.melijn\\.jda", "**me.melijn.jda**");
        final List<String> messages = getSplitMessage(message, 0);
        messages.forEach(msg -> melijn.getShardManager().getGuildById(340081887265685504L).getTextChannelById(486042641692360704L).sendMessage(msg).queue());
    }

    private List<String> getSplitMessage(String message, int margin) {
        String msg = message;
        final List<String> messages = new ArrayList<>();
        while (msg.length() > 2000 - margin) {
            final String findLastNewline = msg.substring(0, 2000 - margin);
            int index = findLastNewline.lastIndexOf("\n");
            if (index < 1800 - margin) {
                index = findLastNewline.lastIndexOf(".");
            }
            if (index < 1800 - margin) {
                index = findLastNewline.lastIndexOf(" ");
            }
            if (index < 1800 - margin) {
                index = 1999 - margin;
            }
            messages.add(msg.substring(0, index));
            msg = msg.substring(index);
        }
        if (msg.length() > 0)
            messages.add(msg);
        return messages;
    }

    public void sendSplitMessage(TextChannel channel, String text) {
        final List<String> messages = getSplitMessage(text, 0);
        messages.forEach(message -> channel.sendMessage(message).queue());
    }

    public void sendSplitCodeBlock(TextChannel channel, String text, String style) {
        final List<String> messages = getSplitMessage(text, 8 + style.length());
        messages.forEach(message -> channel.sendMessage("```" + style + "\n" + message + "```").queue());
    }

    public void argsToSongName(String[] args, StringBuilder sb, Set<String> providers) {
        if (providers.contains(args[0].toLowerCase())) {
            for (int i = 1; i < args.length; i++) {
                sb.append(args[i]).append(" ");
            }
        } else {
            for (String s : args) {
                sb.append(s).append(" ");
            }
        }
    }

    public String variableFormat(String s, Guild guild, User user) {
        return s.replaceAll("%USER%", "<@" + user.getIdLong() + ">")
                .replaceAll("%USERNAME%", user.getName() + "#" + user.getDiscriminator())
                .replaceAll("%GUILDNAME%", guild.getName())
                .replaceAll("%SERVERNAME%", guild.getName())
                .replaceAll("%MEMBERSIZE%", String.valueOf(guild.getMemberCache().size()));
    }
}
