package com.pixelatedsource.jda.commands.animals;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.async.Callback;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.blub.Category;
import com.pixelatedsource.jda.blub.Command;
import com.pixelatedsource.jda.blub.CommandEvent;
import net.dv8tion.jda.core.entities.MessageChannel;

import java.util.concurrent.TimeUnit;

public class CatCommand extends Command {

    public CatCommand() {
        this.commandName = "cat";
        this.description = "Shows you a random kittie";
        this.aliases = new String[]{"kitten", "kat", "poes"};
        this.category = Category.ANIMALS;
    }

    @Override
    protected void execute(CommandEvent event) {
        boolean acces = false;
        if (event.getGuild() == null) acces = true;
        if (!acces) acces = Helpers.hasPerm(event.getGuild().getMember(event.getAuthor()), this.commandName, 0);
        if (acces) {
            MessageChannel channel = event.getChannel();
            Unirest.post("http://random.cat/meow").asJsonAsync(new Callback<JsonNode>() {
                @Override
                public void completed(HttpResponse<JsonNode> response) {
                    channel.sendMessage(response.getBody().getObject().getString("file")).queue();
                }

                @Override
                public void failed(UnirestException e) {
                    channel.sendMessage("Something went wrong").queue(s -> s.delete().queueAfter(5, TimeUnit.SECONDS));
                }

                @Override
                public void cancelled() {
                    channel.sendMessage("Something went wrong").queue(s -> s.delete().queueAfter(5, TimeUnit.SECONDS));
                }
            });
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
