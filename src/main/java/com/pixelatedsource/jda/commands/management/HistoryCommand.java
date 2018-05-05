package com.pixelatedsource.jda.commands.management;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.PixelSniper;
import com.pixelatedsource.jda.blub.Category;
import com.pixelatedsource.jda.blub.Command;
import com.pixelatedsource.jda.blub.CommandEvent;
import com.pixelatedsource.jda.utils.MessageHelper;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.User;

import java.util.ArrayList;
import java.util.Arrays;

import static com.pixelatedsource.jda.PixelSniper.PREFIX;

public class HistoryCommand extends Command {

    public HistoryCommand() {
        this.commandName = "history";
        this.description = "View bans/warns/mutes of a user.";
        this.usage = PREFIX + commandName + " <mode> <user>";
        this.category = Category.MANAGEMENT;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() != null) {
            if (Helpers.hasPerm(event.getMember(), commandName, 0)) {
                String[] args = event.getArgs().split("\\s+");
                if (args.length == 2) {
                    User target;
                    if (event.getMessage().getMentionedUsers().size() > 0) target = event.getMessage().getMentionedUsers().get(0);
                    else if (event.getJDA().retrieveUserById(args[1]) != null) target = event.getJDA().retrieveUserById(args[1]).complete();
                    else target = null;
                    if (target != null) {
                        switch (args[0]) {
                            case "bans":
                                ArrayList<String> bans = new ArrayList<>(Arrays.asList(PixelSniper.mySQL.getUserBans(target, event.getGuild(), event.getJDA())));
                                ArrayList<String> mergedBans = new ArrayList<>();
                                StringBuilder banBuffer = new StringBuilder();
                                for (String ban : bans) {
                                    banBuffer.append(ban);
                                    if (banBuffer.toString().length() > 1900) {
                                        banBuffer.delete(banBuffer.toString().length()-ban.length(), banBuffer.toString().length());
                                        mergedBans.add(banBuffer.toString());
                                        banBuffer = new StringBuilder();
                                    }
                                }
                                mergedBans.add(banBuffer.toString());
                                EmbedBuilder ebBan = new EmbedBuilder();
                                int banCount = 0;
                                for (String mergedBan : mergedBans) {
                                    ebBan.setTitle(target.getName() + "#" + target.getDiscriminator() + "'s bans " + ++banCount + "/" + mergedBans.size());
                                    ebBan.setDescription(mergedBan);
                                    ebBan.setColor(Helpers.EmbedColor);
                                    ebBan.setThumbnail(target.getAvatarUrl());
                                    event.reply(ebBan.build());
                                }
                                break;
                            case "mutes":
                                ArrayList<String> mutes = new ArrayList<>(Arrays.asList(PixelSniper.mySQL.getUserMutes(target, event.getGuild(), event.getJDA())));
                                ArrayList<String> mergedMutes = new ArrayList<>();
                                StringBuilder muteBuffer = new StringBuilder();
                                for (String mute : mutes) {
                                    muteBuffer.append(mute);
                                    if (muteBuffer.toString().length() > 1900) {
                                        muteBuffer.delete(muteBuffer.toString().length()-mute.length(), muteBuffer.toString().length());
                                        mergedMutes.add(muteBuffer.toString());
                                        muteBuffer = new StringBuilder();
                                    }
                                }
                                mergedMutes.add(muteBuffer.toString());
                                EmbedBuilder ebMute = new EmbedBuilder();
                                int muteCount = 0;
                                for (String mergedMute : mergedMutes) {
                                    ebMute.setTitle(target.getName() + "#" + target.getDiscriminator() + "'s mutes " + ++muteCount + "/" + mergedMutes.size());
                                    ebMute.setDescription(mergedMute);
                                    ebMute.setColor(Helpers.EmbedColor);
                                    ebMute.setThumbnail(target.getAvatarUrl());
                                    event.reply(ebMute.build());
                                }
                                break;
                            case "warns":
                                ArrayList<String> warns = new ArrayList<>(Arrays.asList(PixelSniper.mySQL.getUserWarns(target, event.getGuild(), event.getJDA())));
                                ArrayList<String> mergedWarns = new ArrayList<>();
                                StringBuilder warnsBuffer = new StringBuilder();
                                for (String warn : warns) {
                                    warnsBuffer.append(warn);
                                    if (warnsBuffer.toString().length() > 1900) {
                                        warnsBuffer.delete(warnsBuffer.toString().length()-warn.length(), warnsBuffer.toString().length());
                                        mergedWarns.add(warnsBuffer.toString());
                                        warnsBuffer = new StringBuilder();
                                    }
                                }
                                mergedWarns.add(warnsBuffer.toString());
                                EmbedBuilder ebWarn = new EmbedBuilder();
                                int warnCount = 0;
                                for (String mergedBan : mergedWarns) {
                                    ebWarn.setTitle(target.getName() + "#" + target.getDiscriminator() + "'s warns " + ++warnCount + "/" + mergedWarns.size());
                                    ebWarn.setDescription(mergedBan);
                                    ebWarn.setColor(Helpers.EmbedColor);
                                    ebWarn.setThumbnail(target.getAvatarUrl());
                                    event.reply(ebWarn.build());
                                }
                                break;
                        }
                    } else {
                        event.reply("Unknown user");
                    }
                } else {
                    MessageHelper.sendUsage(this, event);
                }
            } else {
                event.reply("You need the permission `" + commandName + "` to execute this command.");
            }
        } else {
            event.reply(Helpers.guildOnly);
        }
    }

}
