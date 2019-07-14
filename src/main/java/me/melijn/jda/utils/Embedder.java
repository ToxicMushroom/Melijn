package me.melijn.jda.utils;

import me.melijn.jda.db.Variables;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Guild;

public class Embedder extends EmbedBuilder {

    public Embedder(Variables variables, Guild guild) {
        if (guild != null) {
            setColor(variables.embedColorCache.get(guild.getIdLong()));
        } else setColor(variables.embedColor);
    }

    public Embedder(Variables variables, long guildId) {
        setColor(variables.embedColorCache.get(guildId));
    }
}
