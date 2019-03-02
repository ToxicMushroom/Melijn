package me.melijn.jda.db;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Sets;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.melijn.jda.Melijn;
import me.melijn.jda.blub.*;
import net.dv8tion.jda.core.entities.Message;
import org.discordbots.api.client.DiscordBotListAPI;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static me.melijn.jda.blub.ChannelType.SELF_ROLE;

public class Variables {

    public final Map<Long, List<Integer>> disabledGuildCommands;
    public final LoadingCache<Long, Boolean> serverHasCC;
    public final Map<Long, Map<Long, Long>> possibleDeletes;
    public final Map<Long, Long> messageUser;

    public final CooldownManager cooldownManager;
    public final LoadingCache<Long, Map<Integer, Integer>> cooldowns;

    public final LoadingCache<Long, Map<Long, String>> selfRoles;

    public final LoadingCache<Long, Integer> embedColorCache;

    public final int embedColor = -16711720;
    public final List<Long> userBlackList = new ArrayList<>();
    public final List<Long> serverBlackList = new ArrayList<>();

    public final LoadingCache<Long, Long> selfRolesChannels;
    public final LoadingCache<Long, Boolean> streamerModeCache;
    public final LoadingCache<Long, String> verificationCodeCache;
    public final LoadingCache<Long, Long> verificationChannelsCache;
    public final LoadingCache<Long, Long> unverifiedRoleCache;
    public final LoadingCache<Long, Integer> verificationThresholdCache;
    public final LoadingCache<Long, VerificationType> verificationTypes;
    public final LoadingCache<Long, Map<Long, Long>> unVerifiedGuildMembersCache;
    public final Map<Long, String> filteredMessageDeleteCause = new HashMap<>();
    public final Map<Long, Long> purgedMessageDeleter = new HashMap<>();
    public final Set<Long> botDeletedMessages = new HashSet<>();
    public final Set<String> unLoggedThreads = new HashSet<>();
    public final Set<Long> looped = new HashSet<>();
    public final Set<Long> loopedQueues = new HashSet<>();
    public final Map<Long, Long> toLeaveTimeMap = new HashMap<>(); // guild, insert time
    public final Set<String> providers = Sets.newHashSet("yt", "sc", "link", "youtube", "soundcloud");
    public final Map<Long, Message> usersFormToReply = new HashMap<>();
    public final Map<Long, Map<Integer, AudioTrack>> userChoices = new HashMap<>();

    public final Map<Long, List<AudioTrack>> userRequestedSongs = new HashMap<>();
    public final Map<Long, Long> userMessageToAnswer = new HashMap<>();
    public final LoadingCache<Long, Long> joinChannelCache;
    public final LoadingCache<Long, Long> leaveChannelCache;
    public final LoadingCache<Long, Long> muteRoleCache;
    public final LoadingCache<Long, String> prefixes;
    public final LoadingCache<Long, List<String>> privatePrefixes;
    public final LoadingCache<Long, String> joinMessages;
    public final LoadingCache<Long, Long> joinRoleCache;
    public final LoadingCache<Long, String> leaveMessages;

    public final LoadingCache<Long, Long> banLogChannelCache;
    public final LoadingCache<Long, Long> muteLogChannelCache;
    public final LoadingCache<Long, Long> kickLogChannelCache;
    public final LoadingCache<Long, Long> warnLogChannelCache;
    public final LoadingCache<Long, Long> musicLogChannelCache;
    public final LoadingCache<Long, Long> sdmLogChannelCache;
    public final LoadingCache<Long, Long> emLogChannelCache;
    public final LoadingCache<Long, Long> odmLogChannelCache;
    public final LoadingCache<Long, Long> pmLogChannelCache;
    public final LoadingCache<Long, Long> fmLogChannelCache;
    public final LoadingCache<Long, Long> reactionLogChannelCache;
    public final LoadingCache<Long, Long> attachmentLogChannelCache;

    public final LoadingCache<Long, Long> musicChannelCache;
    public DiscordBotListAPI dblAPI = null;

    public long startTime = 0;
    public double queryAmount = 0.0;
    public double timerAmount = 0.0;


    public Variables(Melijn melijn) {
        disabledGuildCommands = melijn.getMySQL().getDisabledCommandsMap();
        int frequentSize = 150;
        int frequentDecayMinutes = 4;
        serverHasCC = CacheBuilder.newBuilder()
                .maximumSize(frequentSize)
                .expireAfterAccess(frequentDecayMinutes, TimeUnit.MINUTES)
                .build(new CacheLoader<>() {
                    public Boolean load(@NotNull Long key) {
                        return melijn.getMySQL().getCustomCommands(key).length() > 0;
                    }
                });
        possibleDeletes = new HashMap<>();
        messageUser = new HashMap<>();


        cooldowns = CacheBuilder.newBuilder()
                .maximumSize(frequentSize)
                .expireAfterAccess(frequentDecayMinutes, TimeUnit.MINUTES)
                .build(new CacheLoader<>() {
                    public Map<Integer, Integer> load(@NotNull Long key) {
                        return melijn.getMySQL().getCooldowns(key);
                    }
                });
        cooldownManager = new CooldownManager(this);

        selfRoles = CacheBuilder.newBuilder()
                .maximumSize(frequentSize)
                .expireAfterAccess(frequentDecayMinutes, TimeUnit.MINUTES)
                .build(new CacheLoader<>() {
                    public Map<Long, String> load(@NotNull Long key) {
                        return melijn.getMySQL().getSelfRoles(key);
                    }
                });
        selfRolesChannels = CacheBuilder.newBuilder()
                .maximumSize(frequentSize)
                .expireAfterAccess(frequentDecayMinutes, TimeUnit.MINUTES)
                .build(new CacheLoader<>() {
                    public Long load(@NotNull Long key) {
                        return melijn.getMySQL().getChannelId(key, SELF_ROLE);
                    }
                });

        int normalSize = 20;
        int normalDecayMinutes = 2;
        embedColorCache = CacheBuilder.newBuilder()
                .maximumSize(normalSize)
                .expireAfterAccess(normalDecayMinutes, TimeUnit.MINUTES)
                .build(new CacheLoader<>() {
                    public Integer load(@NotNull Long key) {
                        return melijn.getMySQL().getEmbedColor(key);
                    }
                });
        streamerModeCache = CacheBuilder.newBuilder()
                .maximumSize(normalSize)
                .expireAfterAccess(normalDecayMinutes, TimeUnit.MINUTES)
                .build(new CacheLoader<>() {
                    public Boolean load(@NotNull Long key) {
                        return melijn.getMySQL().getStreamerMode(key);
                    }
                });

        verificationCodeCache = CacheBuilder.newBuilder()
                .maximumSize(normalSize)
                .expireAfterAccess(normalDecayMinutes, TimeUnit.MINUTES)
                .build(new CacheLoader<>() {
                    public String load(@NotNull Long key) {
                        return melijn.getMySQL().getGuildVerificationCode(key);
                    }
                });
        verificationChannelsCache = CacheBuilder.newBuilder()
                .maximumSize(normalSize)
                .expireAfterAccess(normalDecayMinutes, TimeUnit.MINUTES)
                .build(new CacheLoader<>() {
                    public Long load(@NotNull Long key) {
                        return melijn.getMySQL().getChannelId(key, ChannelType.VERIFICATION);
                    }
                });
        unverifiedRoleCache = CacheBuilder.newBuilder()
                .maximumSize(normalSize)
                .expireAfterAccess(normalDecayMinutes, TimeUnit.MINUTES)
                .build(new CacheLoader<>() {
                    public Long load(@NotNull Long key) {
                        return melijn.getMySQL().getRoleId(key, RoleType.UNVERIFIED);
                    }
                });
        verificationThresholdCache = CacheBuilder.newBuilder()
                .maximumSize(normalSize)
                .expireAfterAccess(normalDecayMinutes, TimeUnit.MINUTES)
                .build(new CacheLoader<>() {
                    public Integer load(@NotNull Long key) {
                        return melijn.getMySQL().getGuildVerificationThreshold(key);
                    }
                });
        verificationTypes = CacheBuilder.newBuilder()
                .maximumSize(normalSize)
                .expireAfterAccess(normalDecayMinutes, TimeUnit.MINUTES)
                .build(new CacheLoader<>() {
                    public VerificationType load(@NotNull Long key) {
                        return melijn.getMySQL().getVerificationType(key);
                    }
                });
        unVerifiedGuildMembersCache = CacheBuilder.newBuilder()
                .maximumSize(normalSize)
                .expireAfterAccess(normalDecayMinutes, TimeUnit.MINUTES)
                .build(new CacheLoader<>() {
                    public Map<Long, Long> load(@NotNull Long key) {
                        return melijn.getMySQL().getUnverifiedMembers(key);
                    }
                });

        joinChannelCache = CacheBuilder.newBuilder()
                .maximumSize(normalSize)
                .expireAfterAccess(normalDecayMinutes, TimeUnit.MINUTES)
                .build(new CacheLoader<>() {
                    public Long load(@NotNull Long key) {
                        return melijn.getMySQL().getChannelId(key, ChannelType.JOIN);
                    }
                });
        leaveChannelCache = CacheBuilder.newBuilder()
                .maximumSize(normalSize)
                .expireAfterAccess(normalDecayMinutes, TimeUnit.MINUTES)
                .build(new CacheLoader<>() {
                    public Long load(@NotNull Long key) {
                        return melijn.getMySQL().getChannelId(key, ChannelType.LEAVE);
                    }
                });

        muteRoleCache = CacheBuilder.newBuilder()
                .maximumSize(normalSize)
                .expireAfterAccess(normalDecayMinutes, TimeUnit.MINUTES)
                .build(new CacheLoader<>() {
                    public Long load(@NotNull Long key) {
                        return melijn.getMySQL().getRoleId(key, RoleType.MUTE);
                    }
                });
        prefixes = CacheBuilder.newBuilder()
                .maximumSize(frequentSize)
                .expireAfterAccess(frequentDecayMinutes, TimeUnit.MINUTES)
                .build(new CacheLoader<>() {
                    public String load(@NotNull Long key) {
                        return melijn.getMySQL().getPrefix(key);
                    }
                });
        privatePrefixes = CacheBuilder.newBuilder()
                .maximumSize(frequentSize)
                .expireAfterAccess(frequentDecayMinutes, TimeUnit.MINUTES)
                .build(new CacheLoader<>() {
                    public List<String> load(@NotNull Long key) {
                        return melijn.getMySQL().getPrivatePrefixes(key);
                    }
                });
        joinMessages = CacheBuilder.newBuilder()
                .maximumSize(normalSize)
                .expireAfterAccess(normalDecayMinutes, TimeUnit.MINUTES)
                .build(new CacheLoader<>() {
                    public String load(@NotNull Long key) {
                        return melijn.getMySQL().getMessage(key, MessageType.JOIN);
                    }
                });
        joinRoleCache = CacheBuilder.newBuilder()
                .maximumSize(normalSize)
                .expireAfterAccess(normalDecayMinutes, TimeUnit.MINUTES)
                .build(new CacheLoader<>() {
                    public Long load(@NotNull Long key) {
                        return melijn.getMySQL().getRoleId(key, RoleType.JOIN);
                    }
                });
        leaveMessages = CacheBuilder.newBuilder()
                .maximumSize(normalSize)
                .expireAfterAccess(normalDecayMinutes, TimeUnit.MINUTES)
                .build(new CacheLoader<>() {
                    public String load(@NotNull Long key) {
                        return melijn.getMySQL().getMessage(key, MessageType.LEAVE);
                    }
                });



        banLogChannelCache = CacheBuilder.newBuilder()
                .maximumSize(normalSize)
                .expireAfterAccess(normalDecayMinutes, TimeUnit.MINUTES)
                .build(new CacheLoader<>() {
                    public Long load(@NotNull Long key) {
                        return melijn.getMySQL().getChannelId(key, ChannelType.BAN_LOG);
                    }
                });
        muteLogChannelCache = CacheBuilder.newBuilder()
                .maximumSize(normalSize)
                .expireAfterAccess(normalDecayMinutes, TimeUnit.MINUTES)
                .build(new CacheLoader<>() {
                    public Long load(@NotNull Long key) {
                        return melijn.getMySQL().getChannelId(key, ChannelType.MUTE_LOG);
                    }
                });
        kickLogChannelCache = CacheBuilder.newBuilder()
                .maximumSize(normalSize)
                .expireAfterAccess(normalDecayMinutes, TimeUnit.MINUTES)
                .build(new CacheLoader<>() {
                    public Long load(@NotNull Long key) {
                        return melijn.getMySQL().getChannelId(key, ChannelType.KICK_LOG);
                    }
                });
        warnLogChannelCache = CacheBuilder.newBuilder()
                .maximumSize(normalSize)
                .expireAfterAccess(normalDecayMinutes, TimeUnit.MINUTES)
                .build(new CacheLoader<>() {
                    public Long load(@NotNull Long key) {
                        return melijn.getMySQL().getChannelId(key, ChannelType.WARN_LOG);
                    }
                });
        musicLogChannelCache = CacheBuilder.newBuilder()
                .maximumSize(normalSize)
                .expireAfterAccess(normalDecayMinutes, TimeUnit.MINUTES)
                .build(new CacheLoader<>() {
                    public Long load(@NotNull Long key) {
                        return melijn.getMySQL().getChannelId(key, ChannelType.MUSIC_LOG);
                    }
                });
        sdmLogChannelCache = CacheBuilder.newBuilder()
                .maximumSize(normalSize)
                .expireAfterAccess(normalDecayMinutes, TimeUnit.MINUTES)
                .build(new CacheLoader<>() {
                    public Long load(@NotNull Long key) {
                        return melijn.getMySQL().getChannelId(key, ChannelType.SDM_LOG);
                    }
                });
        emLogChannelCache = CacheBuilder.newBuilder()
                .maximumSize(normalSize)
                .expireAfterAccess(normalDecayMinutes, TimeUnit.MINUTES)
                .build(new CacheLoader<>() {
                    public Long load(@NotNull Long key) {
                        return melijn.getMySQL().getChannelId(key, ChannelType.EM_LOG);
                    }
                });
        odmLogChannelCache = CacheBuilder.newBuilder()
                .maximumSize(normalSize)
                .expireAfterAccess(normalDecayMinutes, TimeUnit.MINUTES)
                .build(new CacheLoader<>() {
                    public Long load(@NotNull Long key) {
                        return melijn.getMySQL().getChannelId(key, ChannelType.ODM_LOG);
                    }
                });
        pmLogChannelCache = CacheBuilder.newBuilder()
                .maximumSize(normalSize)
                .expireAfterAccess(normalDecayMinutes, TimeUnit.MINUTES)
                .build(new CacheLoader<>() {
                    public Long load(@NotNull Long key) {
                        return melijn.getMySQL().getChannelId(key, ChannelType.PM_LOG);
                    }
                });
        fmLogChannelCache = CacheBuilder.newBuilder()
                .maximumSize(normalSize)
                .expireAfterAccess(normalDecayMinutes, TimeUnit.MINUTES)
                .build(new CacheLoader<>() {
                    public Long load(@NotNull Long key) {
                        return melijn.getMySQL().getChannelId(key, ChannelType.FM_LOG);
                    }
                });
        reactionLogChannelCache = CacheBuilder.newBuilder()
                .maximumSize(normalSize)
                .expireAfterAccess(normalDecayMinutes, TimeUnit.MINUTES)
                .build(new CacheLoader<>() {
                    public Long load(@NotNull Long key) {
                        return melijn.getMySQL().getChannelId(key, ChannelType.REACTION_LOG);
                    }
                });
        attachmentLogChannelCache = CacheBuilder.newBuilder()
                .maximumSize(normalSize)
                .expireAfterAccess(normalDecayMinutes, TimeUnit.MINUTES)
                .build(new CacheLoader<>() {
                    public Long load(@NotNull Long key) {
                        return melijn.getMySQL().getChannelId(key, ChannelType.ATTACHMENT_LOG);
                    }
                });

        musicChannelCache = CacheBuilder.newBuilder()
                .maximumSize(normalSize)
                .expireAfterAccess(normalSize, TimeUnit.MINUTES)
                .build(new CacheLoader<>() {
                    public Long load(@NotNull Long key) {
                        return melijn.getMySQL().getChannelId(key, ChannelType.MUSIC);
                    }
                });



        unLoggedThreads.addAll(melijn.getConfig().getSet("unLoggedThreads"));

    }
}
