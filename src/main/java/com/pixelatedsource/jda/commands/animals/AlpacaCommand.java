package com.pixelatedsource.jda.commands.animals;

import com.jagrosh.jdautilities.commandclient.Command;
import com.jagrosh.jdautilities.commandclient.CommandEvent;
import com.pixelatedsource.jda.Helpers;

import javax.swing.text.AttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

public class AlpacaCommand extends Command {

    public AlpacaCommand() {
        this.name = "alpaca";
        this.help = "Shows you a nice alpaca";
        this.guildOnly = false;

    }

    @Override
    protected void execute(CommandEvent event) {
        boolean acces = false;
        if (event.getGuild() == null) acces = true;
        if (!acces) acces = Helpers.hasPerm(event.getGuild().getMember(event.getAuthor()), this.name, 0);
        if (acces) {
            try {
                String webUrl = "http://www.randomalpaca.com/";
                URL url = new URL(webUrl);
                URLConnection connection = url.openConnection();
                InputStream is = connection.getInputStream();
                InputStreamReader isr = new InputStreamReader(is);
                HTMLDocument htmlDoc = (HTMLDocument) new HTMLEditorKit().createDefaultDocument();
                HTMLEditorKit.Parser parser = new ParserDelegator();
                HTMLEditorKit.ParserCallback callback = htmlDoc.getReader(0);
                parser.parse(new BufferedReader(isr), callback, true);
                for (HTMLDocument.Iterator iterator = htmlDoc.getIterator(HTML.Tag.IMG); iterator.isValid(); iterator.next()) {
                    AttributeSet attributes = iterator.getAttributes();
                    String imgSrc = (String) attributes.getAttribute(HTML.Attribute.SRC);
                    if (imgSrc != null && (imgSrc.endsWith(".jpg") || (imgSrc.endsWith(".png")) || (imgSrc.endsWith(".jpeg")) || (imgSrc.endsWith(".bmp")) || (imgSrc.endsWith(".ico")))) {
                        event.reply(imgSrc);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
