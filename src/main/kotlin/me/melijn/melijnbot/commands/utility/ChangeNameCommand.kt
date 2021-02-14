package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.utils.await
import me.melijn.melijnbot.internals.utils.getStringFromArgsNMessage
import me.melijn.melijnbot.internals.utils.message.sendSyntax
import me.melijn.melijnbot.internals.utils.retrieveMemberByArgsNMessage
import net.dv8tion.jda.api.entities.Member

class ChangeNameCommand : AbstractCommand("command.changename") {

    init {
        id = 249
        name = "changeName"
        aliases = arrayOf("cg", "nickname")
        commandCategory = CommandCategory.UTILITY
    }

    override suspend fun execute(context: ICommandContext) {
        val member: Member = if (context.args.size == 1) {
            context.member
        }
        else {
            retrieveMemberByArgsNMessage(context, 0) ?: return
        }

        val newNickname = if (context.args.size == 1) {
            getStringFromArgsNMessage(context, 0, 0, 32)
        }
        else {
            if (context.args.size > 2){
                return sendSyntax(context)
            }
            else{
                if (context.args.isEmpty()){
                    ""
                }
                else {
                    getStringFromArgsNMessage(context, 1, 0, 32)
                }
            }
        }
            member.modifyNickname(newNickname).await()

    }


}

