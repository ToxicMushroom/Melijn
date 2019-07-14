package me.melijn.melijnbot

import me.duncte123.botcommons.config.ConfigUtils


class Container {
    var settings: Settings = ConfigUtils.loadFromFile("config.json", Settings::class.java)
}
