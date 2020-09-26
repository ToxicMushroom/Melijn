package me.melijn.melijnbot.commands.image


import com.wrapper.spotify.Base64
import io.lettuce.core.SetArgs
import kotlinx.coroutines.future.await
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.utils.message.sendFileRsp
import me.melijn.melijnbot.internals.utils.message.sendSyntax
import me.melijn.melijnbot.internals.utils.retrieveUserByArgsNMessage
import java.io.ByteArrayOutputStream
import java.net.URL
import javax.imageio.ImageIO


class JailCommand : AbstractCommand("command.jail") {

    init {
        id = 219
        name = "jail"
        aliases = arrayOf("jailGif")
        commandCategory = CommandCategory.IMAGE
    }

    val jail = JailCommand::class.java.getResourceAsStream("/jail.png").use { ImageIO.read(it) }

    override suspend fun execute(context: CommandContext) {
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
}
