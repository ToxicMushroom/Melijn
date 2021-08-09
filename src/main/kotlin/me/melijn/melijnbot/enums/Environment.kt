package me.melijn.melijnbot.enums

import com.fasterxml.jackson.annotation.JsonProperty

enum class Environment {

    @JsonProperty("production")
    PRODUCTION,

    @JsonProperty("testing")
    TESTING;

}