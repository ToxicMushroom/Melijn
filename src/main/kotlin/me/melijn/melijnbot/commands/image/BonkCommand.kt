package me.melijn.melijnbot.commands.image

import com.wrapper.spotify.Base64
import io.lettuce.core.SetArgs
import kotlinx.coroutines.future.await
import me.melijn.melijnbot.enums.DiscordSize
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
import org.w3c.dom.NamedNodeMap
import org.w3c.dom.Node
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.Closeable
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageTypeSpecifier
import javax.imageio.metadata.IIOMetadata
import javax.imageio.metadata.IIOMetadataNode
import javax.imageio.stream.ImageInputStream
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

        val redisCon = context.daoManager.driverManager.redisConnection
        val cachedAvatar = redisCon?.async()?.get("avatar:${user.id}")?.await()

        val inputImg = if (cachedAvatar == null) {
            val imageApi = context.webManager.imageApi
            val discordSize = DiscordSize.X512
            imageApi.downloadDiscordImgNMessage(context, user.effectiveAvatarUrl, discordSize, false) ?: return
        } else {
            redisCon.async().expire("avatar:${user.id}", 600)
            ImageIO.read(ByteArrayInputStream(Base64.decode(cachedAvatar)))
        }

        val delay = getLongFromArgN(context, 1, 20) ?: 200
        val loops = if (getBooleanFromArgN(context, 2) == false) 1 else 0


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
                GifSequenceWriter(ios, loops)
                    .writeToSequence(image1, delay.toInt())
                    .writeToSequence(image2, delay.toInt())
                    .close()
            }
            val text = "**bonk** %user% \uD83D\uDD28".withSafeVariable("user", user.asTag)
            sendFileRsp(context, text, baos.toByteArray(), "gif")
        }

        ByteArrayOutputStream().use { baos ->
            ImageIO.write(inputImg, "png", baos)

            val encodedAvatar = Base64.encode(baos.toByteArray())
            redisCon?.async()?.set("avatar:${user.id}", encodedAvatar, SetArgs().ex(600))
        }
    }
}

class GifSequenceReader(inputStream: ImageInputStream) {


    private val reader = ImageIO.getImageReadersBySuffix("gif").next()
    val frameCount: Int by lazy { reader.getNumImages(true) }
    var currentDelay = 0 // centiSeconds
    var currentLoop: Boolean = false
    var loopCount: Int = 0
    var currentTransparancyIndex = 0
    var first: BufferedImage? = null

    init {
        reader.input = inputStream
    }

    fun readAll(func: (BufferedImage, Int) -> Unit) {
        for (i in 0 until frameCount) {
            storeFrameInfo(i)
            if (i == 0) func(getFirstFrame(), i)
            else func(reader.read(i), i)
        }
    }

    private fun storeFrameInfo(index: Int) {
        val metaData = reader.getImageMetadata(index)
        val tree = metaData.getAsTree("javax_imageio_gif_image_1.0")
        val children = tree.childNodes
        for (j in 0 until children.length) {
            val nodeItem: Node = children.item(j)
            if (nodeItem.nodeName.equals("GraphicControlExtension")) {
                val attr: NamedNodeMap = nodeItem.attributes
                val delayTimeNode: Node = attr.getNamedItem("delayTime")
                currentDelay = Integer.valueOf(delayTimeNode.nodeValue)

                val transparentColorNode: Node = attr.getNamedItem("transparentColorIndex")
                currentTransparancyIndex = Integer.valueOf(transparentColorNode.nodeValue)
            } else if (nodeItem.nodeName.equals("ApplicationExtensions")) {
                val extensions = nodeItem.childNodes
                for (k in 0 until extensions.length) {
                    val node: IIOMetadataNode = extensions.item(k) as IIOMetadataNode
                    if (node.nodeName.equals("ApplicationExtension")) {
                        val loop = node.userObject as ByteArray // 3 part byte array
                        if (loop.size != 3) break
                        // Last 2 bytes are a little endian loopcount
                        val loopCount = loop[1] + (loop[2].toInt() shl 8)
                        // 0 == infinity loop

                        currentLoop = loopCount == 0
                    }
                }
            }
        }
    }

    private fun getFirstFrame(storeInfo: Boolean = false): BufferedImage {
        return first ?: run {
            first = reader.read(0)
            first ?: throw StinkyException()
        }
    }
}

class StinkyException : Throwable("Kotlin does the stinky haha")

class GifSequenceWriter(outputStream: ImageOutputStream, iterations: Int) : Closeable {

    private val writer = ImageIO.getImageWritersBySuffix("gif").next()
    private val params = writer.defaultWriteParam
    var frameType: Int = 0
    private val metadata by lazy { configureRootMetadata(iterations) }

    init {
        writer.output = outputStream
        writer.prepareWriteSequence(null)
    }

    private fun configureRootMetadata(iterations: Int): IIOMetadata {
        val imageTypeSpecifier: ImageTypeSpecifier = ImageTypeSpecifier.createFromBufferedImageType(frameType)
        val metadata = writer.getDefaultImageMetadata(imageTypeSpecifier, params)
        val metaFormatName: String = metadata.nativeMetadataFormatName
        val root: IIOMetadataNode = metadata.getAsTree(metaFormatName) as IIOMetadataNode

        val graphicsControlExtensionNode: IIOMetadataNode = getNode(root, "GraphicControlExtension")
        graphicsControlExtensionNode.setAttribute("disposalMethod", "restoreToBackgroundColor")
        graphicsControlExtensionNode.setAttribute("userInputFlag", "FALSE")
        graphicsControlExtensionNode.setAttribute("transparentColorFlag", "FALSE")
        graphicsControlExtensionNode.setAttribute("transparentColorIndex", "0")

        val appExtensionNode: IIOMetadataNode = getNode(root, "ApplicationExtensions")
        val child = IIOMetadataNode("ApplicationExtension")
        child.setAttribute("applicationID", "NETSCAPE")
        child.setAttribute("authenticationCode", "2.0")

        child.userObject = byteArrayOf(0b1, iterations.toByte(), 0b0)
        appExtensionNode.appendChild(child)
        metadata.setFromTree(metaFormatName, root)
        return metadata
    }

    /**
     * [delayMillis] in milliseconds
     */
    fun writeToSequence(img: BufferedImage, delayMillis: Int): GifSequenceWriter {
        frameType = img.type
        val metaFormatName: String = metadata.nativeMetadataFormatName
        val root: IIOMetadataNode = metadata.getAsTree(metaFormatName) as IIOMetadataNode

        val graphicsControlExtensionNode: IIOMetadataNode = getNode(root, "GraphicControlExtension")

        graphicsControlExtensionNode.setAttribute("delayTime", "${delayMillis / 10}")
        metadata.setFromTree(metaFormatName, root)

        writer.writeToSequence(IIOImage(img, null, metadata), params)
        return this
    }

    override fun close() {
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