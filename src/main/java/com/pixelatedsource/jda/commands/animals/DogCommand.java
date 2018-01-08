package com.pixelatedsource.jda.commands.animals;

import com.jagrosh.jdautilities.commandclient.Command;
import com.jagrosh.jdautilities.commandclient.CommandEvent;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.async.Callback;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.pixelatedsource.jda.Helpers;
import net.dv8tion.jda.core.Permission;
import org.json.JSONObject;

public class DogCommand extends Command {

    public DogCommand() {
        this.name = "dog";
        this.help = "Shows you a random dog.";
        this.guildOnly = false;
        this.aliases = new String[]{"hond"};
        this.botPermissions = new Permission[]{Permission.MESSAGE_EMBED_LINKS};
    }

    @Override
    protected void execute(CommandEvent event) {
        boolean acces = false;
        if (event.getGuild() == null) acces = true;
        if (!acces) acces = Helpers.hasPerm(event.getGuild().getMember(event.getAuthor()), this.name, 0);
        if (acces) {
            Unirest.post("https://api.thedogapi.co.uk/v2/dog.php").asJsonAsync(new Callback<JsonNode>() {
                @Override
                public void completed(HttpResponse<JsonNode> response) {
                    JSONObject jsonObject = (JSONObject) response.getBody().getObject().getJSONArray("data").get(0);
                    event.reply(jsonObject.getString("url"));

                }

                @Override
                public void failed(UnirestException e) {
                    event.reply("error");
                }

                @Override
                public void cancelled() {
                    event.reply("error");
                }
            });
        }
    }
}
