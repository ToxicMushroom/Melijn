package me.melijn.melijnbot.enums

import com.fasterxml.jackson.annotation.JsonCreator

enum class Environment {
    PRODUCTION,
    TESTING;


    companion object {

        @JsonCreator
        fun forValue(value: String): Environment {
            return if ("production" == value.toLowerCase()) {
                Environment.PRODUCTION
            } else Environment.TESTING
        }
    }
}

