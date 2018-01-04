package com.pixelatedsource.jda.commands;

import com.jagrosh.jdautilities.commandclient.Command;
import com.jagrosh.jdautilities.commandclient.CommandEvent;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.async.Callback;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.PixelatedBot;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;

public class CatCommand extends Command {
    public CatCommand() {
        this.name = "cat";
        this.help = "Usage: " + PixelatedBot.PREFIX + this.name;
        this.botPermissions = new Permission[]{Permission.MESSAGE_EMBED_LINKS};
        this.aliases = new String[] {"kitten", "kat", "poes"};
        this.guildOnly = false;
    }

    @Override
    protected void execute(CommandEvent event) {
        boolean acces = false;
        if (event.getGuild() == null) acces = true;
        if (!acces) acces = Helpers.hasPerm(event.getGuild().getMember(event.getAuthor()), this.name);
        if (acces) {
            Unirest.post("http://random.cat/meow").asJsonAsync(new Callback<JsonNode>() {
                @Override
                public void completed(HttpResponse<JsonNode> response) {
                    event.reply(new EmbedBuilder()
                            .setColor(Helpers.EmbedColor)
                            .setImage(response.getBody().getObject().getString("file"))
                            .build());
                }

                @Override
                public void failed(UnirestException e) {
                    event.reactError();
                }

                @Override
                public void cancelled() {
                    event.reactError();
                }
            });
        }
    }
}
