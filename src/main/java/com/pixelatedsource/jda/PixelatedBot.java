package com.pixelatedsource.jda;

import com.jagrosh.jdautilities.commandclient.CommandClientBuilder;
import com.pixelatedsource.jda.commands.animals.CatCommand;
import com.pixelatedsource.jda.commands.animals.DogCommand;
import com.pixelatedsource.jda.commands.music.*;
import com.pixelatedsource.jda.commands.util.*;
import com.pixelatedsource.jda.db.MySQL;
import com.pixelatedsource.jda.events.AddReaction;
import com.pixelatedsource.jda.events.Channels;
import com.sedmelluq.discord.lavaplayer.jdaudp.NativeAudioSendFactory;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.events.DisconnectEvent;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import javax.security.auth.login.LoginException;
import java.util.HashMap;

public class PixelatedBot extends ListenerAdapter {

    public static MySQL mySQL;
    private static final Config config = new Config();
    public static String OWNERID = config.getValue("ownerid");
    public static String TOKEN = config.getValue("token");
    public static String PREFIX = config.getValue("prefix");
    private static String ip = config.getValue("ipadress");
    private static String user = config.getValue("username");
    private static String pass = config.getValue("password");
    private static String dbname = config.getValue("database");
    public static HashMap<Guild, Boolean> looped = new HashMap<>();


    public static void main(String[] args) throws LoginException, RateLimitedException {
        CommandClientBuilder client = new CommandClientBuilder();
        client.setOwnerId(OWNERID);
        client.setPrefix(PREFIX);
        client.addCommands(
                new PingCommand(),
                new PlayCommand(),
                new QueueCommand(),
                new CatCommand(),
                new SkipCommand(),
                new ClearCommand(),
                new StopCommand(),
                new ResumeCommand(),
                new VolumeCommand(),
                new AboutCommand(),
                new PlayerinfoCommand(),
                new LoopCommand(),
                new TexttoemojiCommand(),
                new SkipXCommand(),
                new PermCommand(),
                new NowPlayingCommand(),
                new RemoveCommand(),
                new GuildInfoCommand(),
                new RoleInfoCommand(),
                new DogCommand()
        );
        new JDABuilder(AccountType.BOT)
                .setToken(TOKEN)
                .setAudioSendFactory(new NativeAudioSendFactory())
                .setGame(Game.of(Game.GameType.STREAMING, PREFIX + "help", "https://www.twitch.tv/pixelhamster"))
                .addEventListener(client.build())
                .addEventListener(new AddReaction())
                .addEventListener(new Channels())
                .buildAsync();
        Helpers.starttime = System.currentTimeMillis();
        mySQL = new MySQL(ip, user, pass, dbname);

    }

    public void onDisconnect(DisconnectEvent e) {
        for (Guild guild : e.getJDA().getGuilds()) {
            guild.getAudioManager().closeAudioConnection();
        }
    }
}
