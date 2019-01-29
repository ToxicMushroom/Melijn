package me.melijn.jda.commands.fun;

import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;

import static me.melijn.jda.Melijn.PREFIX;

public class DogCommand extends Command {

    public DogCommand() {
        this.commandName = "dog";
        this.description = "Shows you a dog";
        this.usage = PREFIX + commandName;
        this.category = Category.FUN;
        this.aliases = new String[]{"hond"};
        this.id = 77;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() == null || event.hasPerm(event.getMember(), commandName, 0)) {
            event.getWebUtils().getImage("animal_dog", image ->
                    event.getMessageHelper().sendFunText("Enjoy your \uD83D\uDC36 ~woof~", image.getUrl(), event));
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
