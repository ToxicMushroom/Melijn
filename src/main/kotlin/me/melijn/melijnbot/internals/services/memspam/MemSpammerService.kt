//package me.melijn.melijnbot.internals.services.memspam
//
//
//import me.melijn.melijnbot.Container
//import me.melijn.melijnbot.MelijnBot.Companion.eventManager
//import me.melijn.melijnbot.internals.services.Service
//import me.melijn.melijnbot.internals.threading.RunnableTask
//import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
//import net.dv8tion.jda.api.sharding.ShardManager
//import net.dv8tion.jda.api.utils.data.DataArray
//import net.dv8tion.jda.api.utils.data.DataObject
//import net.dv8tion.jda.internal.JDAImpl
//import java.util.concurrent.TimeUnit
//import kotlin.random.Random
//
//
//class SpamService(
//    val container: Container,
//    val shardManager: ShardManager
//) : Service("Spam", 20, 5000, TimeUnit.MILLISECONDS) {
//    var counterStart = System.currentTimeMillis()
//    val msgTemplate = DataObject.empty()
//        .put("author", DataObject.empty()
//            .put("id", "231459866630291459")
//            .put("username", "ToxicMushroom")
//            .put("discriminator", "2610")
//            .put("avatar", "dfaaefa54a2804addb1f494da7aa904d")
//        )
//        .put("content", "I like trains")
//        .put("timestamp", "2020-10-06T16:25:37Z")
//        .put("edited_timestamp", null)
//        .put("tts", false)
//        .put("mention_everyone", false)
//        .put("mentions", DataArray.empty())
//        .put("mention_roles", DataArray.empty())
//        .put("mention_channels", DataArray.empty())
//        .put("attachments", DataArray.empty())
//        .put("embeds", DataArray.empty())
//        .put("pinned", false)
//        .put("type", 0)
//
//    override val service = RunnableTask {
//        val msgAmount = Random.nextInt(5)
//        val shard = shardManager.getShardById(0)?.awaitReady() ?: return@RunnableTask
//        for (i in 0 until msgAmount) {
//            msgTemplate
//                .put("id", counterStart++)
//                .put("channel_id", "456055962004881408")
//                .put("guild_id", "340081887265685504")
//
//            val msg = (shard as JDAImpl).entityBuilder.createMessage(msgTemplate, true);
//            eventManager.handle(GuildMessageReceivedEvent(shard, 0, msg))
//        }
//    }
//}