package me.melijn.melijnbot.internals.web.apis

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonRootName
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.client.*
import io.ktor.client.request.*
import kotlin.random.Random

class BooruApi(val httpClient: HttpClient) {

    private val kotlinXmlMapper = XmlMapper(JacksonXmlModule().apply {
        setDefaultUseWrapper(false)
    }).registerKotlinModule()
        .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    val bannableDiscordItems = listOf(" loli ", "lolita", " cub ", " shota ")

    suspend fun getRandomPost(domain: String, tags: String): BooruEntry? {
        val response = httpClient.get<String>("https://$domain/index.php?page=dapi&s=post&q=index") {
            this.parameter("tags", tags)
        }

        return try {
            val legal = kotlinXmlMapper.readValue<BooruResponse>(response).posts
                .filter { !it.hasChildren }
                .filter { bannableDiscordItems
                    .all { banned ->
                        !it.tags.contains(banned)
                    }
                }
            legal[Random.nextInt(legal.size)]
        } catch (t: Throwable) {
            null
        }
    }

}

@JsonRootName("posts")
data class BooruResponse(

    @JsonProperty("post")
    var posts: List<BooruEntry>
)

@JsonRootName("post")
data class BooruEntry(
    @JacksonXmlProperty(isAttribute = true, localName = "file_url")
    var imageUrl: String,
    @JacksonXmlProperty(isAttribute = true, localName = "has_children")
    var hasChildren: Boolean,
    @JacksonXmlProperty(isAttribute = true, localName = "tags")
    var tags: String
)