package com.pixelatedsource.jda.utils;

import com.pixelatedsource.jda.blub.Command;
import com.pixelatedsource.jda.blub.CommandEvent;
import com.pixelatedsource.jda.commands.management.SetPrefixCommand;
import net.dv8tion.jda.core.entities.User;

import java.util.Calendar;
import java.util.HashMap;

import static com.pixelatedsource.jda.PixelSniper.PREFIX;

public class MessageHelper {

    public static HashMap<String, String> filterDeletedMessages = new HashMap<>();
    //public static HashMap<String, User> deletedByEmote = new HashMap<>();
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
        return String.valueOf(mHour) + ":" + mMinutes + "-" + mSeconds + "s " + mDay + "/" + mMonth + "/" + mYear;
    }

    public static boolean isRightFormat(String string) {
        if (string.matches("\\d++[smhdwMy]")) return true;
        else return false;
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
        if (SetPrefixCommand.prefixes.containsKey(event.getGuild().getId()))
            event.reply(cmd.getUsage().replaceFirst(PREFIX, SetPrefixCommand.prefixes.get(event.getGuild().getId())));
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
}
