package me.melijn.jda.commands;

import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;

import static me.melijn.jda.Melijn.PREFIX;

public class DonateCommand extends Command {

    public DonateCommand() {
        this.commandName = "donate";
        this.description = "gives you information about how you can support the developers of Melijn";
        this.usage = PREFIX + commandName;
        this.category = Category.DEFAULT;
    }

    @Override
    protected void execute(CommandEvent event) {
        event.reply("You can donate to the development and server cost of me here: **https://paypal.me/PixelHamster**\n" +
                "You can then also request a donator role in my support discord\n" +
                "**Warning** donations will not be refunded **and** don't use someone else their money");
    }
}
