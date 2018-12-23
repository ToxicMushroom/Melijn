package me.melijn.jda.commands.developer;

import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.utils.TaskScheduler;

import java.util.concurrent.ThreadPoolExecutor;

import static me.melijn.jda.Melijn.PREFIX;

public class TestCommand extends Command {

    public TestCommand() {
        this.commandName = "test";
        this.description = "this command is for testing";
        this.usage = PREFIX + commandName;
        this.category = Category.DEVELOPER;
    }

    @Override
    protected void execute(CommandEvent event) {
        //event.reply("test command is not in use atm");



        ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) TaskScheduler.EXECUTOR_SERVICE;
        event.reply(threadPoolExecutor.getActiveCount());

/*
        String[] args = event.getArgs().split("\\s+");
        if (args[0].matches("100(?:,0)?%?|\\d{1,2}(?:,\\d)?%?")) {
            float percent = Float.parseFloat(args[0].replaceAll("%", ""));
            BufferedImage bufferedImage = new BufferedImage(1020, 120, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics2D = bufferedImage.createGraphics();
            graphics2D.setColor(Color.BLACK);
            graphics2D.fillRect(0,0, bufferedImage.getWidth(), bufferedImage.getHeight());
            graphics2D.setColor(Color.GRAY);
            graphics2D.fillRect(10, 10, bufferedImage.getWidth() - 20, bufferedImage.getHeight() - 20);
            graphics2D.setColor(new Color(SetEmbedColorCommand.embedColorCache.getUnchecked(event.getGuild().getIdLong())));
            graphics2D.fillRect(10, 10, Math.round(percent * 10), 100);
            event.reply(bufferedImage);
        }*/
    }
}
