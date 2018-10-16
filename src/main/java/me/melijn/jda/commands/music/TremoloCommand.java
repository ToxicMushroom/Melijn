package me.melijn.jda.commands.music;

import me.melijn.jda.Helpers;
import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.blub.Need;
import me.melijn.jda.music.MusicManager;
import me.melijn.jda.music.MusicPlayer;
import me.melijn.jda.utils.MessageHelper;

import static me.melijn.jda.Melijn.PREFIX;

public class TremoloCommand extends Command {

    public TremoloCommand() {
        this.commandName = "tremolo";
        this.description = "Apply or remove a tremolo effect on audio";
        this.usage = PREFIX + this.commandName + " [depth|frequency|off] [depthValue | frequencyValue]";
        this.category = Category.MUSIC;
        this.needs = new Need[]{Need.GUILD};
        this.extra = "depth is in percent frequency is in times/second(max 100) (if any is set to 0 then the effect will stop)";
    }

    @Override
    protected void execute(CommandEvent event) {
        if (Helpers.hasPerm(event.getGuild().getMember(event.getAuthor()), this.commandName, 0)) {
            String[] args = event.getArgs().split("\\s+");
            MusicPlayer musicPlayer = MusicManager.getManagerInstance().getPlayer(event.getGuild());
            if (args.length > 0 && !args[0].isEmpty()) {
                if (event.getGuild().getSelfMember().getVoiceState().getChannel() != null) {
                    if (event.getMember().getVoiceState().getChannel() == event.getGuild().getSelfMember().getVoiceState().getChannel()) {
                        if (args[0].equalsIgnoreCase("off")) {
                            musicPlayer.setDepth(0);
                            musicPlayer.setFrequency(0);
                            musicPlayer.updateFilters();
                            event.reply("The tremolo effect has been **disabled** by **" + event.getFullAuthorName() + "**");
                        } else if (args.length > 1) {
                            if (args[0].toLowerCase().matches("depth|d")) {
                                if (args[1].matches("([0-9]%)|[0-9][0-9]%|100%")) {
                                    double i = Double.parseDouble(args[1].replace("%", "")) / 100D;
                                    musicPlayer.setDepth(i);
                                    musicPlayer.updateFilters();
                                    event.reply("The depth has been set to **" + args[1] + "** by **" + event.getFullAuthorName() + "**");
                                } else {
                                    MessageHelper.sendUsage(this, event);
                                }
                            } else if (args[0].toLowerCase().matches("frequency|fr|f")) {
                                if (args[1].matches("[0-9]|[0-9][0-9]|100|[0-9]?[0-9].[0-9]?[0-9]?[0-9]")) {
                                    double i = Double.parseDouble(args[1]);
                                    musicPlayer.setFrequency(i);
                                    musicPlayer.updateFilters();
                                    event.reply("The frequency has been set to **" + args[1] + "** by **" + event.getFullAuthorName() + "**");
                                } else {
                                    MessageHelper.sendUsage(this, event);
                                }
                            } else {
                                MessageHelper.sendUsage(this, event);
                            }
                        }
                    } else {
                        event.reply("You have to be in the same voice channel as me to apply tremolo effects");
                    }
                } else {
                    event.reply("I'm not in a voiceChannel");
                }
            } else {
                event.reply("The configured depth is **" + musicPlayer.getDepth() + "** and frequency is **" + musicPlayer.getFrequency() + "**");
            }
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
