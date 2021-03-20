package me.melijn.melijnbot.commands.developer

import me.melijn.melijnbot.database.message.ModularMessage
import me.melijn.melijnbot.enums.MessageType
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.utils.toUCC
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class TestCommand : AbstractCommand("command.test") {

    init {
        id = 20
        name = "test"
        commandCategory = CommandCategory.DEVELOPER
    }

    val logger: Logger = LoggerFactory.getLogger(TestCommand::class.java)

    override suspend fun execute(context: ICommandContext) {
        val driver = context.daoManager.driverManager
        var counter = 0
        driver.executeQuery("SELECT * FROM messages_old", { rs ->
            while (rs.next()) {
                val guildId = rs.getLong("guildId")
                val typeStr = rs.getString("type")
                val type = try {
                    MessageType.valueOf(typeStr)
                } catch (t: Throwable) {
                    logger.warn("Unknown message type: $typeStr")
                    null
                }
                if (type != null) {
                    val message = rs.getString("message")
                    val modular = try {
                        ModularMessage.fromJSON(message)
                    } catch (t: Throwable) {
                        null
                    }
                    if (modular == null) logger.warn("Failed to parse message: $message")
                    else {
                        val msgName = type.toUCC()
                        context.daoManager.messageWrapper.setMessage(guildId, msgName, modular)
                        context.daoManager.linkedMessageWrapper.setMessage(guildId, type, msgName)
                        logger.info("Migrated row: ${++counter} :) of messages")
                    }
                }
            }

            context.reply("Migrated messages")
        })

        var counter2 = 0
        driver.executeQuery("SELECT * FROM customcommands", { rs ->
            while (rs.next()) {
                val guildId = rs.getLong("guildId")
                val id = rs.getLong("id")
                val chance = rs.getInt("chance")
                val prefix = rs.getBoolean("prefix")

                val name = rs.getString("name")
                val description = rs.getString("description")
                val aliases = rs.getString("aliases")
                val message = rs.getString("content")
                val modular = try {
                    ModularMessage.fromJSON(message)
                } catch (t: Throwable) {
                    null
                }
                if (modular == null) logger.warn("Failed to parse cc-message: $message")
                else {
                    val msgName = "cc.$id"
                    context.daoManager.messageWrapper.setMessage(guildId, msgName, modular)

                    driver.executeUpdate(
                        "INSERT INTO custom_commands (guild_id, name, description, msg_name, aliases, chance, prefix, contains_triggers) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                        guildId, name, description, msgName, aliases, chance, prefix, false
                    )
                    logger.info("Migrated row: ${++counter2} :) of customcommands")
                }

            }
            context.reply("Migrated custom commands")
        })

        context.reply("Migrating, pog pog")
    }
}
