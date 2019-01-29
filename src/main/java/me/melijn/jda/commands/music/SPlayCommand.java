package me.melijn.jda.commands.music;

import me.melijn.jda.audio.Lava;
import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.blub.Need;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.VoiceChannel;

import static me.melijn.jda.Melijn.PREFIX;

public class SPlayCommand extends Command {

    public SPlayCommand() {
        this.commandName = "splay";
        this.description = "Gives you search results to pick from";
        this.usage = PREFIX + commandName + " [sc] [songname]";
        this.aliases = new String[]{"search", "searchplay", "sp"};
        this.category = Category.MUSIC;
        this.needs = new Need[]{Need.GUILD, Need.SAME_VOICECHANNEL_OR_DISCONNECTED};
        this.permissions = new Permission[]{Permission.MESSAGE_EMBED_LINKS, Permission.VOICE_CONNECT};
        this.id = 45;
    }

    @Override
    protected void execute(CommandEvent event) {
        Guild guild = event.getGuild();
        boolean access = event.hasPerm(guild.getMember(event.getAuthor()), commandName + ".*", 1);
        VoiceChannel senderVoiceChannel = guild.getMember(event.getAuthor()).getVoiceState().getChannel();
        String[] args = event.getArgs().split("\\s+");
        if (args.length == 0 || args[0].isEmpty()) {
            event.sendUsage(this, event);
            return;
        }
        Lava lava = event.getClient().getMelijn().getLava();
        StringBuilder sb = new StringBuilder();
        event.getMessageHelper().argsToSongName(args, sb, event.getVariables().providers);
        String songName = sb.toString();
        switch (args[0].toLowerCase()) {
            case "sc":
            case "soundcloud":
                if (event.hasPerm(guild.getMember(event.getAuthor()), commandName + ".sc", 0) || access) {
                    if (!lava.tryToConnectToVC(event, guild, senderVoiceChannel)) return;
                    lava.getAudioLoader().searchTracks(event.getTextChannel(), "scsearch:" + songName, event.getAuthor());
                } else {
                    event.reply("You need the permission `" + commandName + ".sc` to execute this command.");
                }
                break;
            default:
                if (event.hasPerm(guild.getMember(event.getAuthor()), commandName + ".yt", 0) || access) {
                    if (!lava.tryToConnectToVC(event, guild, senderVoiceChannel)) return;
                    lava.getAudioLoader().searchTracks(event.getTextChannel(), "ytsearch:" + songName, event.getAuthor());
                } else {
                    event.reply("You need the permission `" + commandName + ".yt` to execute this command.");
                }
                break;

        }
    }
}
