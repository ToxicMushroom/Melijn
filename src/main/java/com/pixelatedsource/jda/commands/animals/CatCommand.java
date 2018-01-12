package com.pixelatedsource.jda.commands.animals;

import com.jagrosh.jdautilities.commandclient.Command;
import com.jagrosh.jdautilities.commandclient.CommandEvent;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.async.Callback;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.PixelSniper;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;

import static net.dv8tion.jda.core.Permission.MESSAGE_EMBED_LINKS;

public class CatCommand extends Command {

    public CatCommand() {
        this.name = "cat";
        this.help = "Usage: " + PixelSniper.PREFIX + this.name;
        this.botPermissions = new Permission[]{Permission.MESSAGE_EMBED_LINKS};
        this.aliases = new String[] {"kitten", "kat", "poes"};
        this.guildOnly = false;
        this.botPermissions = new Permission[] {MESSAGE_EMBED_LINKS};
    }

    @Override
    protected void execute(CommandEvent event) {
        boolean acces = false;
        if (event.getGuild() == null) acces = true;
        if (!acces) acces = Helpers.hasPerm(event.getGuild().getMember(event.getAuthor()), this.name, 0);
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
