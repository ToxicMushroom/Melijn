package me.melijn.melijnbot.commands.image


import com.wrapper.spotify.Base64
import io.lettuce.core.SetArgs
import kotlinx.coroutines.future.await
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.utils.ImageUtils
import me.melijn.melijnbot.internals.utils.message.sendFileRsp
import me.melijn.melijnbot.internals.utils.message.sendSyntax
import me.melijn.melijnbot.internals.utils.retrieveUserByArgsNMessage
import net.dv8tion.jda.api.Permission
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.URL
import javax.imageio.ImageIO


class JailCommand : AbstractCommand("command.jail") {

    init {
        id = 219
        name = "jail"
        aliases = arrayOf("jailGif")
        discordChannelPermissions = arrayOf(Permission.MESSAGE_ATTACH_FILES)
        commandCategory = CommandCategory.IMAGE
    }

    val jail = JailCommand::class.java.getResourceAsStream("/jail.png").use { ImageIO.read(it) }

    suspend fun execute(context: ICommandContext) {
        if (context.commandParts[1].equals("jailGif", true)) {
            executeGif(context)
        } else {
            executeNormal(context)
        }
    }

    private suspend fun executeNormal(context: ICommandContext) {
        if (context.args.isEmpty()) {
            sendSyntax(context)
            return
        }

        val user = retrieveUserByArgsNMessage(context, 0) ?: return

        val rediCon = context.daoManager.driverManager.redisConnection
        val avatar = rediCon?.async()
            ?.get("avatarjailed:${user.id}")
            ?.await()

        val jailedBytesArr = if (avatar == null) {
            val inputImg = ImageIO.read(URL(user.effectiveAvatarUrl.replace(".gif", ".png") + "?size=512"))
            val graphics = inputImg.graphics
            graphics.drawImage(jail, 0, 0, inputImg.width, inputImg.height, null)
            graphics.dispose()

            ByteArrayOutputStream().use { baos ->
                ImageIO.write(inputImg, "png", baos)
                baos.toByteArray()
            }
        } else {
            rediCon.async()
                .expire("avatarjailed:${user.id}", 600)
            Base64.decode(avatar)
        }


        sendFileRsp(context, "**Go to jail** ${user.asTag} 👮‍♀️", jailedBytesArr, "png")

        rediCon?.async()
            ?.set("avatarjailed:${user.id}", Base64.encode(jailedBytesArr), SetArgs().ex(600))
    }

    private suspend fun executeGif(context: ICommandContext) {
        val user = retrieveUserByArgsNMessage(context, 0) ?: return

        val rediCon = context.daoManager.driverManager.redisConnection
        val avatar = rediCon?.async()
            ?.get("avatarjailedgif:${user.id}")
            ?.await()
        if (avatar != null) {
            val byteArray = Base64.decode(avatar)
            sendFileRsp(context, "**Go to jail** ${user.asTag} 👮‍♀️", byteArray, "gif")
            return
        }

        val img = ImageUtils.downloadImage(context, user.effectiveAvatarUrl + "?size=256", true)
        val imgInputStream = ByteArrayInputStream(img).use { ImageIO.createImageInputStream(it) }
        val gd = GifSequenceReader(imgInputStream)
        var frame1: BufferedImage? = null
        var gw: GifSequenceWriter? = null
        ByteArrayOutputStream().use { baos ->
            ImageIO.createImageOutputStream(baos).use { ios ->
                gd.readAll {
                    if (frame1 == null) {
                        frame1 = it
                        gw = GifSequenceWriter(ios, BufferedImage.TYPE_INT_ARGB, gd.currentLoop)
                    }

                    val graphics = it.graphics
                    graphics.drawImage(jail, 0, 0, it.width, it.height, null)
                    graphics.dispose()
                    gw?.writeToSequence(it, gd.currentDelay * 10)
                }
                gw?.close()
            }
            val byteArray = baos.toByteArray()
            sendFileRsp(context, "**Go to jail** ${user.asTag} 👮‍♀️", byteArray, "gif")

            rediCon?.async()?.set("avatarjailedgif:${user.id}", Base64.encode(byteArray), SetArgs().ex(600))
        }
    }
}
