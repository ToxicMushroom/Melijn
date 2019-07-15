package me.melijn.melijnbot.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum Environment {
    PRODUCTION,
    TESTING;


    @JsonCreator
    public static Environment forValue(String value) {
        if ("production".equalsIgnoreCase(value)) {
            return Environment.PRODUCTION;
        } else return Environment.TESTING;
    }
}
