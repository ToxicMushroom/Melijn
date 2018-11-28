package me.melijn.jda.commands.util;

import me.melijn.jda.Helpers;
import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.blub.Need;
import me.melijn.jda.utils.Embedder;
import me.melijn.jda.utils.MessageHelper;
import me.melijn.jda.utils.CrapUtils;
import net.dv8tion.jda.core.Permission;
import org.json.JSONObject;

import static me.melijn.jda.Melijn.PREFIX;

public class UrbanCommand extends Command {

    public UrbanCommand() {
        this.commandName = "urban";
        this.usage = PREFIX + commandName + " <word>";
        this.description = "Searches a word on urbandictionary.com";
        this.aliases = new String[]{"dictionary", "meaning"};
        this.category = Category.UTILS;
        this.permissions = new Permission[]{Permission.MESSAGE_EMBED_LINKS};
        this.needs = new Need[]{Need.NSFW};
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() == null || Helpers.hasPerm(event.getMember(), this.commandName, 0)) {
            String[] args = event.getArgs().split("\\s+");
            if (args.length > 0 && !args[0].isBlank()) {
                String result = CrapUtils.getWebUtilsInstance().run("https://api.urbandictionary.com/v0/define?term=" + event.getArgs());
                if (result != null) {
                    JSONObject jresult = new JSONObject(result);
                    if (jresult.getJSONArray("list").toList().size() > 0) {
                        JSONObject firstMeaning = jresult.getJSONArray("list").getJSONObject(0);

                        event.reply(new Embedder(event.getGuild())
                                .setTitle(firstMeaning.getString("word"))
                                .setDescription("**Meaning**\n " + removeBrackets(firstMeaning.getString("definition")) + "\n\n**Example**\n " + removeBrackets(firstMeaning.getString("example")))
                                .setFooter(Helpers.getFooterStamp(), null)
                                .build());
                    } else {
                        event.reply("Word not found, check for spelling mistakes");
                    }
                }
            } else {
                MessageHelper.sendUsage(this, event);
            }
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }

    private String removeBrackets(String input) {
        return input.replaceAll("\\[", "").replaceAll("\\]", "");
        /*
        Matcher match = Pattern.compile("\\[(.+)\\]").matcher(input);
        int i = 0;
        if (match.find()) {
            while (i < match.groupCount()) {
                if (!match.group(i).matches("\\s+"))
                    input = input.replaceFirst("\\[" + match.group(i) + "\\]", match.group(i++));
            }
        }
        return input;*/
    }
}
