package me.melijn.melijnbot.objects.jagtag

import com.jagrosh.jagtag.JagTag
import com.jagrosh.jagtag.Parser
import java.util.function.Supplier
import java.util.regex.Pattern

val REGEX_PARSER_SUPPLIER: Supplier<Parser> = Supplier {
    JagTag().newDefaultBuilder()
        .addMethods(RegexMethods.getMethods())
        .build()
}

object RegexJagTagParser {
    suspend fun makeIntoPattern(messageContent: String): Pattern {
        val parser = REGEX_PARSER_SUPPLIER.get()
        val parsed = parser.parse(Pattern.quote(messageContent))
        parser.clear()
        return Pattern.compile(parsed)
    }
}
