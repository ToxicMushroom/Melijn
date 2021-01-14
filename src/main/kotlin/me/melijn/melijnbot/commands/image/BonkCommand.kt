package me.melijn.melijnbot.commands.image

import com.wrapper.spotify.Base64
import io.lettuce.core.SetArgs
import kotlinx.coroutines.future.await
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.utils.getBooleanFromArgN
import me.melijn.melijnbot.internals.utils.getLongFromArgN
import me.melijn.melijnbot.internals.utils.message.sendFileRsp
import me.melijn.melijnbot.internals.utils.message.sendSyntax
import me.melijn.melijnbot.internals.utils.retrieveUserByArgsNMessage
import me.melijn.melijnbot.internals.utils.withSafeVariable
import net.dv8tion.jda.api.Permission
import java.awt.image.RenderedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.URL
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageTypeSpecifier
import javax.imageio.metadata.IIOMetadata
import javax.imageio.metadata.IIOMetadataNode
import javax.imageio.stream.ImageOutputStream

class BonkCommand : AbstractCommand("command.bonk") {

    init {
        id = 205
        name = "bonk"
        commandCategory = CommandCategory.IMAGE
        discordChannelPermissions = arrayOf(Permission.MESSAGE_ATTACH_FILES)
    }

    override suspend fun execute(context: ICommandContext) {
        if (context.args.isEmpty()) {
            sendSyntax(context)
            return
        }

        val user = retrieveUserByArgsNMessage(context, 0) ?: return

        val rediCon = context.daoManager.driverManager.redisConnection
        val avatar = rediCon?.async()
            ?.get("avatar:${user.id}")
            ?.await()

        val inputImg = if (avatar == null) {
            ImageIO.read(URL(user.effectiveAvatarUrl.replace(".gif", ".png") + "?size=512"))
        } else {
            rediCon.async()
                .expire("avatar:${user.id}", 600)
            ImageIO.read(ByteArrayInputStream(Base64.decode(avatar)))
        }

        val delay = getLongFromArgN(context, 1, 20) ?: 200
        val loops = getBooleanFromArgN(context, 2) ?: true


        val image1 = BonkCommand::class.java.getResourceAsStream("/bonk1.png").use { ImageIO.read(it) }
        val image2 = BonkCommand::class.java.getResourceAsStream("/bonk2.png").use { ImageIO.read(it) }

        val graphics1 = image1.graphics
        val graphics2 = image2.graphics
        graphics1.drawImage(inputImg, 144, 144, 300, 300, null)
        graphics2.drawImage(inputImg, 60, 240, 420, 180, null)

        graphics1.dispose()
        graphics2.dispose()


        ByteArrayOutputStream().use { baos ->
            ImageIO.createImageOutputStream(baos).use { ios ->
                GifSequenceWriter(ios, image1.type, delay.toInt(), loops)
                    .writeToSequence(image1)
                    .writeToSequence(image2)
                    .close()
            }
            val text = "**bonk** %user% \uD83D\uDD28".withSafeVariable("user", user.asTag)
            sendFileRsp(context, text, baos.toByteArray(), "gif")
        }

        val baos = ByteArrayOutputStream()
        ImageIO.write(inputImg, "png", baos);

        rediCon?.async()
            ?.set("avatar:${user.id}", Base64.encode(baos.toByteArray()), SetArgs().ex(600))
    }
}

class GifSequenceWriter(outputStream: ImageOutputStream, imageType: Int, delay: Int, loop: Boolean) {

    private val writer = ImageIO.getImageWritersBySuffix("gif").next()
    private val params = writer.defaultWriteParam
    private val metadata: IIOMetadata

    init {
        val imageTypeSpecifier: ImageTypeSpecifier = ImageTypeSpecifier.createFromBufferedImageType(imageType)
        metadata = writer.getDefaultImageMetadata(imageTypeSpecifier, params)

        configureRootMetadata(delay, loop)

        writer.output = outputStream
        writer.prepareWriteSequence(null)
    }

    private fun configureRootMetadata(delay: Int, loop: Boolean) {
        val metaFormatName: String = metadata.nativeMetadataFormatName
        val root: IIOMetadataNode = metadata.getAsTree(metaFormatName) as IIOMetadataNode

        val graphicsControlExtensionNode: IIOMetadataNode = getNode(root, "GraphicControlExtension")
        graphicsControlExtensionNode.setAttribute("disposalMethod", "restoreToBackgroundColor")
        graphicsControlExtensionNode.setAttribute("userInputFlag", "FALSE")
        graphicsControlExtensionNode.setAttribute("transparentColorFlag", "FALSE")
        graphicsControlExtensionNode.setAttribute("delayTime", "${delay / 10}")
        graphicsControlExtensionNode.setAttribute("transparentColorIndex", "0")

        val appExtensionNode: IIOMetadataNode = getNode(root, "ApplicationExtensions")
        val child = IIOMetadataNode("ApplicationExtension")
        child.setAttribute("applicationID", "NETSCAPE")
        child.setAttribute("authenticationCode", "2.0")

        val loopContinuously: Int = if (loop) 0 else 1
        child.userObject =
            byteArrayOf(0x1, (loopContinuously and 0xFF).toByte(), ((loopContinuously shr 8) and 0xFF).toByte())
        appExtensionNode.appendChild(child)
        metadata.setFromTree(metaFormatName, root)
    }

    fun writeToSequence(img: RenderedImage): GifSequenceWriter {
        writer.writeToSequence(IIOImage(img, null, metadata), params)
        return this
    }

    fun close() {
        writer.endWriteSequence()
    }

    companion object {
        private fun getNode(rootNode: IIOMetadataNode, nodeName: String): IIOMetadataNode {
            val nNodes = rootNode.length
            for (i in 0 until nNodes) {
                if (rootNode.item(i).nodeName.equals(nodeName, ignoreCase = true)) {
                    return rootNode.item(i) as IIOMetadataNode
                }
            }
            val node = IIOMetadataNode(nodeName)
            rootNode.appendChild(node)
            return node
        }
    }

}