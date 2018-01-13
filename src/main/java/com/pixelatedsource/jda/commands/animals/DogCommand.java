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
import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

import static com.pixelatedsource.jda.PixelSniper.PREFIX;

public class DogCommand extends Command {

    public DogCommand() {
        this.commandName = "dog";
        this.description = "Shows you a random dog";
        this.category = Category.ANIMALS;
        this.usage = PREFIX + commandName;
        this.aliases = new String[]{"hond"};
    }

    @Override
    protected void execute(CommandEvent event) {
        boolean acces = false;
        if (event.getGuild() == null) acces = true;
        if (!acces) acces = Helpers.hasPerm(event.getGuild().getMember(event.getAuthor()), this.commandName, 0);
        if (acces) {
            MessageChannel channel = event.getChannel();
            Unirest.post("https://api.thedogapi.co.uk/v2/dog.php").asJsonAsync(new Callback<JsonNode>() {
                @Override
                public void completed(HttpResponse<JsonNode> response) {
                    JSONObject jsonObject = (JSONObject) response.getBody().getObject().getJSONArray("data").get(0);
                    channel.sendMessage(jsonObject.getString("url")).queue();

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
        }
    }
}
