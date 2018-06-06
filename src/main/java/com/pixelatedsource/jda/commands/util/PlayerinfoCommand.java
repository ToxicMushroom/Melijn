package com.pixelatedsource.jda.commands.util;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.blub.Category;
import com.pixelatedsource.jda.blub.Command;
import com.pixelatedsource.jda.blub.CommandEvent;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;

import java.text.SimpleDateFormat;
import java.util.Date;

import static com.pixelatedsource.jda.PixelSniper.PREFIX;

public class PlayerinfoCommand extends Command {

    public PlayerinfoCommand() {
        this.commandName = "userinfo";
        this.description = "Shows you useful information about a user/member";
        this.usage = PREFIX + this.commandName + " <@user|id>";
        this.aliases = new String[]{"profile", "playerinfo", "memberinfo", "playerprofile"};
        this.category = Category.UTILS;
        this.permissions = new Permission[]{Permission.MESSAGE_EMBED_LINKS};
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() == null || Helpers.hasPerm(event.getMember(), this.commandName, 0)) {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm ss");
            String[] args = event.getArgs().split("\\s+");
            User user = args.length > 0 ? Helpers.getUserByArgs(event, args[0]) : event.getAuthor();

            EmbedBuilder eb = new EmbedBuilder();
            eb.setColor(Helpers.EmbedColor);
            eb.setTitle(user.getName() + "#" + user.getDiscriminator() + "'s profile");
            String url = user.getAvatarUrl() == null ? user.getDefaultAvatarUrl() : user.getAvatarUrl();
            eb.setThumbnail(url + "?size=1024");
            if (event.getGuild() == null || event.getGuild().getMember(user) == null) {
                eb.addField("Avatar:", "[Download](" + url + "?size=1024)", true);
                eb.addField("ID:", user.getId(), true);
                eb.addField("Discord join date:", String.valueOf(user.getCreationTime().toLocalDate()), false);
                eb.addField("Bot:", String.valueOf(user.isBot()), false);
                eb.addField("Member of this guild:", "false", false);
            } else {
                Member member = event.getGuild().getMember(user);
                String nickname = member.getNickname();
                if (nickname == null) nickname = "No nickname";
                eb.addField("Avatar:", "[Download](" + url + "?size=1024)", true);
                eb.addField("Nickname:", nickname, false);
                eb.addField("Status:", member.getOnlineStatus().toString(), false);
                eb.addField("Playing:", String.valueOf(member.getGame()), false);
                eb.addField("ID:", user.getId(), false);
                eb.addField("Discord join date:", String.valueOf(simpleDateFormat.format(Date.from(user.getCreationTime().toInstant()))) + "s", true);
                eb.addField("Guild join date:", String.valueOf(simpleDateFormat.format(Date.from(member.getJoinDate().toInstant()))) + "s", true);
                eb.addField("Bot:", String.valueOf(user.isBot()), false);
                eb.addField("Member of this guild:", "true", false);
                eb.addField("Owner of this guild:", String.valueOf(member.isOwner()).toLowerCase(), false);
            }
            event.reply(eb.build());
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
