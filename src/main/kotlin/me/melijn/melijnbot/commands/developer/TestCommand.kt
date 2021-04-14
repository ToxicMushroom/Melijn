package me.melijn.melijnbot.commands.developer

import me.melijn.melijnbot.database.votes.UserVote
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.embed.Embedder
import me.melijn.melijnbot.internals.utils.awaitOrNull
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
        val list = arrayOf(
            694536958931894285,
            694536958931894285,
            694536958931894285,
            694536958931894285,
            789189476756881418,
            789189476756881418,
            789189476756881418,
            789189476756881418,
            829861833024208936,
            829861833024208936,
            829861833024208936,
            829861833024208936,
            829865817268813894,
            829865817268813894,
            829865817268813894,
            829865817268813894,
            398257398311157770,
            803732624509501460,
            749937400498618490,
            560739551111282689,
            324448619535138816,
            765289736100511754,
            708301826004353035,
            708301826004353035,
            708301826004353035,
            708301826004353035,
            239399607732404224,
            239399607732404224,
            239399607732404224,
            728317309524901888,
            807447745441824809,
            398257398311157770,
            231459866630291459
        ).toSet()
        for (id in list) {
            val userVote = context.daoManager.voteWrapper.getUserVote(id)
            val th = 3 * 3600000
            val incident = System.currentTimeMillis() - th
            val newState = UserVote(id, (userVote?.votes ?: 0) + 4, (userVote?.streak ?: 0) + 4,
                incident,
                incident,
                incident,
                incident
            )
            context.daoManager.voteWrapper.setVote(newState)
            context.daoManager.balanceWrapper.addBalance(id, 8000)
            val user = context.shardManager.retrieveUserById(id).awaitOrNull()
            val pc = user?.openPrivateChannel()?.awaitOrNull()
            pc?.sendMessage(Embedder(context)
                .setTitle("Lost votes during downtime rewards")
                .setDescription("Hi, you were detected :eye: for voting during our downtime. Since I wasn't sure if you actually got your rewards/dm this is to make sure you did." +
                        "\nBot was down for 5h between 5am-10am utc+2 14/04/2021." +
                        "\nReplacement **rewards**:\n" +
                        "- all last vote times reset to end of downtime (10am)\n" +
                        "- **8k** mel\n" +
                        "- **+4** votes and **+4** streak")
                .setFooter("Sorry for this inconvenience.")
                .build()
            )?.awaitOrNull()
            context.reply("Set rewards for $id - ${user?.asTag ?: "unknown"}")
        }
        context.reply("done")
    }
}
