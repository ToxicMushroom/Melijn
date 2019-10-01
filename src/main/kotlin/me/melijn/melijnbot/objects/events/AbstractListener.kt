package me.melijn.melijnbot.objects.events

import me.melijn.melijnbot.Container
import net.dv8tion.jda.api.hooks.EventListener
import org.slf4j.Logger
import org.slf4j.LoggerFactory

abstract class AbstractListener(protected val container: Container) : EventListener {

    val logger: Logger = LoggerFactory.getLogger(AbstractListener::class.java.name)

    private val botLists = longArrayOf(
            110373943822540800L, // Dbots
            264445053596991498L, // Dbl
            374071874222686211L, // Bots for discord
            112319935652298752L, // Carbon
            439866052684283905L, // Discord Boats
            387812458661937152L, // Botlist.space
            483344253963993113L, // AutomaCord
            454933217666007052L, // Divine Discord Bot List
            446682534135201793L, // Discords best bots
            477792727577395210L, // discordbotlist.xyz
            475571221946171393L  // bots.discordlist.app
    )
}