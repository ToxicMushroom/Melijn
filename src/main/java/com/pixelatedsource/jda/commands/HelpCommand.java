package com.pixelatedsource.jda.commands;

import com.pixelatedsource.jda.PixelSniper;
import com.pixelatedsource.jda.blub.Category;
import com.pixelatedsource.jda.blub.Command;
import com.pixelatedsource.jda.blub.CommandEvent;

import static com.pixelatedsource.jda.PixelSniper.PREFIX;

public class HelpCommand extends Command {

    public HelpCommand() {
        this.commandName = "help";
        this.description = "Shows you this menu";
        this.usage = PREFIX + commandName;
        this.aliases = new String[]{"commands", "cmds"};
        this.category = Category.DEFAULT;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getAuthor().isFake()) return;
        StringBuilder helpMenu = new StringBuilder();
        StringBuilder defaultCommands = new StringBuilder(), utilCommands = new StringBuilder(), musicCommands = new StringBuilder(), permCommands = new StringBuilder(), animalCommands = new StringBuilder();
        helpMenu.append("**-->> Help Menu <<--**\n");
        defaultCommands.append("***-> Default category <-***\n");
        utilCommands.append("***-> Util category <-***\n");
        musicCommands.append("***-> Music category <-***\n");
        permCommands.append("***-> Management category <-***\n");
        animalCommands.append("***-> Animal category <-***\n");
        for (Command cmd : PixelSniper.commandClient.getCommands()) {
            switch (cmd.getCategory()) {
                case DEFAULT:
                    defaultCommands.append("- **").append(cmd.getCommandName()).append("**\n   - *Description*: ").append(cmd.getDescription()).append("\n   - *Usage*: ").append(cmd.getUsage()).append("\n");
                    break;
                case UTILS:
                    utilCommands.append("- **").append(cmd.getCommandName()).append("**\n   - *Description*: ").append(cmd.getDescription()).append("\n   - *Usage*: ").append(cmd.getUsage()).append("\n");
                    break;
                case MUSIC:
                    musicCommands.append("- **").append(cmd.getCommandName()).append("**\n   - *Description*: ").append(cmd.getDescription()).append("\n   - *Usage*: ").append(cmd.getUsage()).append("\n");
                    break;
                case PERMS:
                    permCommands.append("- **").append(cmd.getCommandName()).append("**\n   - *Description*: ").append(cmd.getDescription()).append("\n   - *Usage*: ").append(cmd.getUsage()).append("\n");
                    break;
                case ANIMALS:
                    animalCommands.append("- **").append(cmd.getCommandName()).append("**\n   - *Description*: ").append(cmd.getDescription()).append("\n   - *Usage*: ").append(cmd.getUsage()).append("\n");
                    break;
            }
        }
        helpMenu.append(defaultCommands.toString()).append(utilCommands.toString()).append(musicCommands.toString()).append(permCommands.toString()).append(animalCommands.toString());
        event.getAuthor().openPrivateChannel().queue(c -> {
            c.sendMessage(helpMenu.toString()).queue();
            event.getMessage().addReaction("\u2705").queue();
        });
    }
}
