package com.pixelatedsource.jda.utils;

import net.dv8tion.jda.core.entities.User;

import java.util.Calendar;
import java.util.HashMap;

public class MessageHelper {

    public static HashMap<String, String> filterDeletedMessages = new HashMap<>();
    public static HashMap<String, User> deletedByEmote = new HashMap<>();

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
}
