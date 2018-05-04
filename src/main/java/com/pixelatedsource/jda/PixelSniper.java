package com.pixelatedsource.jda;

import com.pixelatedsource.jda.blub.CommandClient;
import com.pixelatedsource.jda.blub.CommandClientBuilder;
import com.pixelatedsource.jda.commands.HelpCommand;
import com.pixelatedsource.jda.commands.fun.*;
import com.pixelatedsource.jda.commands.management.*;
import com.pixelatedsource.jda.commands.music.*;
import com.pixelatedsource.jda.commands.util.*;
import com.pixelatedsource.jda.db.MySQL;
import com.pixelatedsource.jda.events.AddReaction;
import com.pixelatedsource.jda.events.Channels;
import com.pixelatedsource.jda.events.Chat;
import com.pixelatedsource.jda.events.JoinLeave;
import com.sedmelluq.discord.lavaplayer.jdaudp.NativeAudioSendFactory;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.events.DisconnectEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import javax.security.auth.login.LoginException;
import java.util.HashMap;

public class PixelSniper extends ListenerAdapter {

    private static final Config config = new Config();
    private static String OWNERID = config.getValue("ownerid");
    private static String TOKEN = config.getValue("token");
    public static String PREFIX = config.getValue("prefix");
    private static String IP = config.getValue("ipadress");
    private static String USER = config.getValue("username");
    private static String PASS = config.getValue("password");
    private static String DBNAME = config.getValue("database");

    public static HashMap<Guild, Boolean> looped = new HashMap<>();
    public static MySQL mySQL = new MySQL(IP, USER, PASS, DBNAME);

    public static void main(String[] args) throws LoginException, InterruptedException {
        mySQL.update("TRUNCATE TABLE commands");
        CommandClientBuilder client = new CommandClientBuilder();
        client.setOwnerId(OWNERID);
        client.setPrefix(PREFIX);
        client.addCommands(new SetJoinLeaveChannelCommand(), new SetJoinRoleCommand(), new SetJoinMessageCommand(),new SetLeaveMessageCommand(), new TriggeredCommand(), new SlapCommand(), new PatCommand(), new FilterCommand(), new PotatoCommand(), new PauseCommand(), new SPlayCommand(), new BanCommand(), new HistoryCommand(), new MuteCommand(), new SetMuteRoleCommand(), new TempMuteCommand(), new UnmuteCommand(), new AvatarCommand(), new WarnCommand(), new PurgeCommand(), new HelpCommand(), new PingCommand(), new PlayCommand(), new QueueCommand(), new CatCommand(), new SkipCommand(), new ClearCommand(), new StopCommand(), new ResumeCommand(), new VolumeCommand(), new AboutCommand(), new PlayerinfoCommand(), new LoopCommand(), new TexttoemojiCommand(), new SkipXCommand(), new PermCommand(), new NowPlayingCommand(), new RemoveCommand(), new GuildInfoCommand(), new RoleInfoCommand(), new DogCommand(), new SetPrefixCommand(), new SetMusicChannelCommand(), new SetLogChannelCommand(), new TempBanCommand(), new UnbanCommand(), new SetStreamerModeCommand(), new SetStreamUrlCommand());
        CommandClient commandClient = client.build();

        JDA jda = new JDABuilder(AccountType.BOT).setToken(TOKEN).setGame(Game.streaming(PREFIX + "help", "https://www.twitch.tv/pixelhamster")).setAutoReconnect(true)
                .addEventListener(commandClient).addEventListener(new JoinLeave()).addEventListener(new AddReaction()).addEventListener(new Channels()).addEventListener(new Chat()).setAudioSendFactory(new NativeAudioSendFactory()).buildBlocking();
        Helpers.startTimer(jda);
        Helpers.starttime = System.currentTimeMillis();
    }

    public void onDisconnect(DisconnectEvent e) {
        for (Guild guild : e.getJDA().getGuilds()) {
            guild.getAudioManager().closeAudioConnection();
        }
    }
}
