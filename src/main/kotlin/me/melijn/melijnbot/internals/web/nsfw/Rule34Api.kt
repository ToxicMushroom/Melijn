package me.melijn.melijnbot.internals.web.nsfw

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

class Rule34Api(val httpClient: HttpClient) {

    val baseUrl = "https://rule34.xxx/index.php?page=dapi&s=post&q=index"

    private val kotlinXmlMapper = XmlMapper(JacksonXmlModule().apply {
        setDefaultUseWrapper(false)
    }).registerKotlinModule()
        .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    suspend fun getRandomPost(tags: String): Rule34Entry? {
        val response = httpClient.get<String>(baseUrl) {
            this.parameter("tags", tags)
        }

        return try {
            val legal = kotlinXmlMapper.readValue<Rule34Response>(response).posts.filter { !it.hasChildren }
            legal[Random.nextInt(legal.size)]
        } catch (t: Throwable) {
            null
        }
    }

}

@JsonRootName("posts")
data class Rule34Response(

    @JsonProperty("post")
    var posts: List<Rule34Entry>
)

@JsonRootName("post")
data class Rule34Entry(
    @JacksonXmlProperty(isAttribute = true, localName = "file_url")
    var imageUrl: String,
    @JacksonXmlProperty(isAttribute = true, localName = "has_children")
    var hasChildren: Boolean
)