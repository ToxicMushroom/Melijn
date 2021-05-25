/*
 * Original Work: Copyright 2015 Austin Keener, Michael Ritter, Florian Spieß, and the JDA contributors
 * Modified Work: Copyright 2020 ToxicMushroom
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package me.melijn.melijnbot.internals.models

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.EmbedType
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.internal.entities.EntityBuilder
import java.awt.Color
import java.time.*
import java.time.temporal.TemporalAccessor
import java.util.*
import javax.annotation.Nonnull

class EmbedEditor() {
    constructor(embed: MessageEmbed?) : this() {
        if (embed == null) return
        this.title = embed.title
        this.type = embed.type
        this.titleUrl = embed.url
        this.description = embed.description
        this.timestamp = embed.timestamp
        this.color = embed.colorRaw
        this.footer = embed.footer?.text?.let {
            Footer(it).apply {
                this.iconUrl = embed.footer?.iconUrl
                this.proxyIconUrl = embed.footer?.proxyIconUrl
            }
        }
        this.thumbnail = embed.thumbnail?.let {
            Thumbnail().apply {
                this.url = it.url
                this.proxyUrl = it.proxyUrl
                this.width = it.width
                this.height = it.height
            }
        }
        this.image = embed.image?.let {
            Image().apply {
                this.url = it.url
                this.proxyUrl = it.proxyUrl
                this.width = it.width
                this.height = it.height
            }
        }
        this.author = embed.author?.let {
            Author().apply {
                this.name = it.name
                this.url = it.url
                this.proxyIconUrl = it.proxyIconUrl
                this.iconUrl = it.iconUrl
            }
        }
        this.video = embed.videoInfo?.let {
            Video().apply {
                this.url = it.url
                this.proxyUrl = null
                this.height = it.height
                this.width = it.width
            }
        }
        this.provider = embed.siteProvider?.let {
            Provider().apply {
                this.name = it.name
                this.url = it.url
            }
        }
        this.fields = LinkedList(embed.fields.mapNotNull {
            val name = it.name ?: return@mapNotNull null
            val value = it.value ?: return@mapNotNull null
            Field(name, value).apply {
                this.inline = it.isInline
            }
        })
    }

    @JsonProperty("title")
    var title: String? = null

    @JsonProperty("type")
    var type: EmbedType? = null

    @JsonProperty("url")
    var titleUrl: String? = null

    @JsonProperty("description")
    var description: String? = null

    @JsonProperty("timestamp")
    var timestamp: OffsetDateTime? = null

    @JsonProperty("color")
    var color: Int? = null

    @JsonProperty("footer")
    var footer: Footer? = null

    @JsonProperty("image")
    var image: Image? = null

    @JsonProperty("thumbnail")
    var thumbnail: Thumbnail? = null

    @JsonProperty("video")
    var video: Video? = null

    @JsonProperty("provider")
    var provider: Provider? = null

    @JsonProperty("author")
    var author: Author? = null

    @JsonProperty("fields")
    var fields = mutableListOf<Field>()


    /**
     * Resets this builder to default state.
     * <br></br>All parts will be either empty or null after this method has returned.
     *
     * @return The current EmbedEditor with default values
     */
    @JsonIgnore
    fun clear(): EmbedEditor {
        description = null
        fields.clear()
        titleUrl = null
        title = null
        timestamp = null
        color = Role.DEFAULT_COLOR_RAW
        thumbnail = null
        author = null
        footer = null
        image = null
        return this
    }

    /**
     * Checks if the given embed is empty. Empty embeds will throw an exception if built
     *
     * @return true if the embed is empty and cannot be built
     */
    @JsonIgnore
    fun isEmpty(): Boolean {
        return title == null && timestamp == null && thumbnail == null && author == null && footer == null &&
            image == null && color == Role.DEFAULT_COLOR_RAW && description?.isEmpty() == true && fields.isEmpty()
    }

    /**
     * The overall length of the current EmbedBuilder in displayed characters.
     * <br></br>Represents the [MessageEmbed.getLength()][net.dv8tion.jda.api.entities.MessageEmbed.getLength] value.
     *
     * @return length of the current builder state
     */
    @JsonIgnore
    fun length(): Int {
        var length = description?.length ?: 0
        synchronized(fields) {
            length += fields
                .map { f -> f.name.length + f.value.length }
                .reduceOrNull { acc, i -> acc + i } ?: 0
        }
        title?.let { length += it.length }
        author?.let { length += it.name?.length ?: 0 }
        footer?.let { length += it.text.length }
        length += author?.name?.length ?: 0
        length += footer?.text?.length ?: 0
        return length
    }

    /**
     * Checks whether the constructed [MessageEmbed][net.dv8tion.jda.api.entities.MessageEmbed]
     * is within the limits for a bot account.
     *
     * @return True, if the [length][.length] is less or equal to the specific limit
     *
     * @see MessageEmbed.EMBED_MAX_LENGTH_BOT
     */
    @JsonIgnore
    fun isValidLength(): Boolean {
        val length = length()
        return length <= MessageEmbed.EMBED_MAX_LENGTH_BOT
    }


    /**
     * Sets the Title of the embed.
     * <br></br>Overload for [.setTitle] without URL parameter.
     *
     *
     * **[Example](https://raw.githubusercontent.com/DV8FromTheWorld/JDA/assets/assets/docs/embeds/04-setTitle.png)**
     *
     * @param  title
     * the title of the embed
     *
     * @throws java.lang.IllegalArgumentException
     *
     *  * If the provided `title` is an empty String.
     *  * If the length of `title` is greater than [net.dv8tion.jda.api.entities.MessageEmbed.TITLE_MAX_LENGTH].
     *
     *
     * @return the builder after the title has been set
     */
    @Nonnull
    @JsonIgnore
    fun setTitle(title: String?): EmbedEditor {
        return setTitle(title, null)
    }

    /**
     * Sets the Title of the embed.
     * <br></br>You can provide `null` as url if no url should be used.
     *
     *
     * **[Example](https://raw.githubusercontent.com/DV8FromTheWorld/JDA/assets/assets/docs/embeds/04-setTitle.png)**
     *
     * @param  title
     * the title of the embed
     * @param  url
     * Makes the title into a hyperlink pointed at this url.
     *
     * @throws java.lang.IllegalArgumentException
     *
     *  * If the provided `title` is an empty String.
     *  * If the length of `title` is greater than [net.dv8tion.jda.api.entities.MessageEmbed.TITLE_MAX_LENGTH].
     *  * If the length of `url` is longer than [net.dv8tion.jda.api.entities.MessageEmbed.URL_MAX_LENGTH].
     *  * If the provided `url` is not a properly formatted http or https url.
     *
     *
     * @return the builder after the title has been set
     */
    @Nonnull
    @JsonIgnore
    fun setTitle(title: String?, url: String?): EmbedEditor {
        this.title = title
        this.titleUrl = url
        return this
    }


    /**
     * Sets the Description of the embed. This is where the main chunk of text for an embed is typically placed.
     *
     *
     * **[Example](https://raw.githubusercontent.com/DV8FromTheWorld/JDA/assets/assets/docs/embeds/05-setDescription.png)**
     *
     * @param  description
     * the description of the embed, `null` to reset
     *
     * @throws java.lang.IllegalArgumentException
     * If the length of `description` is greater than [net.dv8tion.jda.api.entities.MessageEmbed.TEXT_MAX_LENGTH]
     *
     * @return the builder after the description has been set
     */
    @JsonIgnore
    fun setDescription(description: String?): EmbedEditor {
        this.description = description
        return this
    }

    /**
     * Appends to the description of the embed. This is where the main chunk of text for an embed is typically placed.
     *
     *
     * **[Example](https://raw.githubusercontent.com/DV8FromTheWorld/JDA/assets/assets/docs/embeds/05-setDescription.png)**
     *
     * @param  description
     * the string to append to the description of the embed
     *
     * @throws java.lang.IllegalArgumentException
     *
     *  * If the provided `description` String is null
     *  * If the length of `description` is greater than [net.dv8tion.jda.api.entities.MessageEmbed.TEXT_MAX_LENGTH].
     *
     *
     * @return the builder after the description has been set
     */
    @JsonIgnore
    fun appendDescription(description: String): EmbedEditor {
        this.description = (this.description ?: "") + description
        return this
    }

    /**
     * Sets the Timestamp of the embed.
     *
     *
     * **[Example](https://raw.githubusercontent.com/DV8FromTheWorld/JDA/assets/assets/docs/embeds/13-setTimestamp.png)**
     *
     *
     * **Hint:** You can get the current time using [Instant.now()][java.time.Instant.now] or convert time from a
     * millisecond representation by using [Instant.ofEpochMilli(long)][java.time.Instant.ofEpochMilli];
     *
     * @param  temporal
     * the temporal accessor of the timestamp
     *
     * @return the builder after the timestamp has been set
     */
    @JsonIgnore
    fun setTimestampField(temporal: TemporalAccessor?): EmbedEditor {
        timestamp = when (temporal) {
            null -> null
            is OffsetDateTime -> temporal
            else -> {
                val offset: ZoneOffset? = try {
                    ZoneOffset.from(temporal)
                } catch (ignore: DateTimeException) {
                    ZoneOffset.UTC
                }
                try {
                    val ldt = LocalDateTime.from(temporal)
                    OffsetDateTime.of(ldt, offset)
                } catch (ignore: DateTimeException) {
                    try {
                        val instant = Instant.from(temporal)
                        OffsetDateTime.ofInstant(instant, offset)
                    } catch (ex: DateTimeException) {
                        throw DateTimeException(
                            "Unable to obtain OffsetDateTime from TemporalAccessor: " +
                                temporal + " of type " + temporal.javaClass.name, ex
                        )
                    }
                }
            }
        }
        return this
    }

    /**
     * Sets the Color of the embed.
     *
     * [Example](https://raw.githubusercontent.com/DV8FromTheWorld/JDA/assets/assets/docs/embeds/02-setColor.png)
     *
     * @param  color
     * The [Color][java.awt.Color] of the embed
     * or `null` to use no color
     *
     * @return the builder after the color has been set
     *
     * @see .setColor
     */
    @JsonIgnore
    fun setColor(color: Color?): EmbedEditor {
        this.color = color?.rgb ?: Role.DEFAULT_COLOR_RAW
        return this
    }

    /**
     * Sets the raw RGB color value for the embed.
     *
     * [Example](https://raw.githubusercontent.com/DV8FromTheWorld/JDA/assets/assets/docs/embeds/02-setColor.png)
     *
     * @param  color
     * The raw rgb value, or [Role.DEFAULT_COLOR_RAW] to use no color
     *
     * @return the builder after the color has been set
     *
     * @see .setColor
     */
    @JsonIgnore
    fun setColor(color: Int): EmbedEditor {
        this.color = color
        return this
    }

    /**
     * Sets the Thumbnail of the embed.
     *
     *
     * **[Example](https://raw.githubusercontent.com/DV8FromTheWorld/JDA/assets/assets/docs/embeds/06-setThumbnail.png)**
     *
     *
     * **Uploading images with Embeds**
     * <br></br>When uploading an <u>image</u>
     * (using [MessageChannel.sendFile(...)][net.dv8tion.jda.api.entities.MessageChannel.sendFile])
     * you can reference said image using the specified filename as URI `attachment://filename.ext`.
     *
     *
     * <u>Example</u>
     * <pre>`
     * MessageChannel channel; // = reference of a MessageChannel
     * EmbedBuilder embed = new EmbedBuilder();
     * InputStream file = new URL("https://http.cat/500").openStream();
     * embed.setThumbnail("attachment://cat.png") // we specify this in sendFile as "cat.png"
     * .setDescription("This is a cute cat :3");
     * channel.sendFile(file, "cat.png").embed(embed.build()).queue();
    `</pre> *
     *
     * @param  url
     * the url of the thumbnail of the embed
     *
     * @throws java.lang.IllegalArgumentException
     *
     *  * If the length of `url` is longer than [net.dv8tion.jda.api.entities.MessageEmbed.URL_MAX_LENGTH].
     *  * If the provided `url` is not a properly formatted http or https url.
     *
     *
     * @return the builder after the thumbnail has been set
     */
    @JsonIgnore
    fun setThumbnail(url: String?): EmbedEditor {
        if (url == null) {
            thumbnail = null
        } else {
            thumbnail = Thumbnail().apply {
                this.url = url
                this.width = 0
                this.height = 0
            }
        }
        return this
    }

    /**
     * Sets the Image of the embed.
     *
     *
     * **[Example](https://raw.githubusercontent.com/DV8FromTheWorld/JDA/assets/assets/docs/embeds/11-setImage.png)**
     *
     *
     * **Uploading images with Embeds**
     * <br></br>When uploading an <u>image</u>
     * (using [MessageChannel.sendFile(...)][net.dv8tion.jda.api.entities.MessageChannel.sendFile])
     * you can reference said image using the specified filename as URI `attachment://filename.ext`.
     *
     *
     * <u>Example</u>
     * <pre>`
     * MessageChannel channel; // = reference of a MessageChannel
     * EmbedBuilder embed = new EmbedBuilder();
     * InputStream file = new URL("https://http.cat/500").openStream();
     * embed.setImage("attachment://cat.png") // we specify this in sendFile as "cat.png"
     * .setDescription("This is a cute cat :3");
     * channel.sendFile(file, "cat.png").embed(embed.build()).queue();
    `</pre> *
     *
     * @param  url
     * the url of the image of the embed
     *
     * @throws java.lang.IllegalArgumentException
     *
     *  * If the length of `url` is longer than [net.dv8tion.jda.api.entities.MessageEmbed.URL_MAX_LENGTH].
     *  * If the provided `url` is not a properly formatted http or https url.
     *
     *
     * @return the builder after the image has been set
     *
     * @see net.dv8tion.jda.api.entities.MessageChannel.sendFile
     */
    @JsonIgnore
    fun setImage(url: String?): EmbedEditor {
        if (url == null) {
            image = null
        } else {
            image = Image().apply {
                this.url = url
                this.width = 0
                this.height = 0
            }
        }
        return this
    }

    /**
     * Sets the Author of the embed. The author appears in the top left of the embed and can have a small
     * image beside it along with the author's name being made clickable by way of providing a url.
     * This convenience method just sets the name.
     *
     *
     * **[Example](https://raw.githubusercontent.com/DV8FromTheWorld/JDA/assets/assets/docs/embeds/03-setAuthor.png)**
     *
     * @param  name
     * the name of the author of the embed. If this is not set, the author will not appear in the embed
     *
     * @throws java.lang.IllegalArgumentException
     * If the length of `name` is longer than [net.dv8tion.jda.api.entities.MessageEmbed.AUTHOR_MAX_LENGTH].
     *
     * @return the builder after the author has been set
     */
    @JsonIgnore
    fun setAuthor(name: String?): EmbedEditor {
        return setAuthor(name, null, null)
    }

    /**
     * Sets the Author of the embed. The author appears in the top left of the embed and can have a small
     * image beside it along with the author's name being made clickable by way of providing a url.
     * This convenience method just sets the name and the url.
     *
     *
     * **[Example](https://raw.githubusercontent.com/DV8FromTheWorld/JDA/assets/assets/docs/embeds/03-setAuthor.png)**
     *
     * @param  name
     * the name of the author of the embed. If this is not set, the author will not appear in the embed
     * @param  url
     * the url of the author of the embed
     *
     * @throws java.lang.IllegalArgumentException
     *
     *  * If the length of `name` is longer than [net.dv8tion.jda.api.entities.MessageEmbed.AUTHOR_MAX_LENGTH].
     *  * If the length of `url` is longer than [net.dv8tion.jda.api.entities.MessageEmbed.URL_MAX_LENGTH].
     *  * If the provided `url` is not a properly formatted http or https url.
     *
     *
     * @return the builder after the author has been set
     */
    @JsonIgnore
    fun setAuthor(name: String?, url: String?): EmbedEditor {
        return setAuthor(name, url, null)
    }

    /**
     * Sets the Author of the embed. The author appears in the top left of the embed and can have a small
     * image beside it along with the author's name being made clickable by way of providing a url.
     *
     *
     * **[Example](https://raw.githubusercontent.com/DV8FromTheWorld/JDA/assets/assets/docs/embeds/03-setAuthor.png)**
     *
     *
     * **Uploading images with Embeds**
     * <br></br>When uploading an <u>image</u>
     * (using [MessageChannel.sendFile(...)][net.dv8tion.jda.api.entities.MessageChannel.sendFile])
     * you can reference said image using the specified filename as URI `attachment://filename.ext`.
     *
     *
     * <u>Example</u>
     * <pre>`
     * MessageChannel channel; // = reference of a MessageChannel
     * EmbedBuilder embed = new EmbedBuilder();
     * InputStream file = new URL("https://http.cat/500").openStream();
     * embed.setAuthor("Minn", null, "attachment://cat.png") // we specify this in sendFile as "cat.png"
     * .setDescription("This is a cute cat :3");
     * channel.sendFile(file, "cat.png").embed(embed.build()).queue();
    `</pre> *
     *
     * @param  name
     * the name of the author of the embed. If this is not set, the author will not appear in the embed
     * @param  url
     * the url of the author of the embed
     * @param  iconUrl
     * the url of the icon for the author
     *
     * @throws java.lang.IllegalArgumentException
     *
     *  * If the length of `name` is longer than [net.dv8tion.jda.api.entities.MessageEmbed.AUTHOR_MAX_LENGTH].
     *  * If the length of `url` is longer than [net.dv8tion.jda.api.entities.MessageEmbed.URL_MAX_LENGTH].
     *  * If the provided `url` is not a properly formatted http or https url.
     *  * If the length of `iconUrl` is longer than [net.dv8tion.jda.api.entities.MessageEmbed.URL_MAX_LENGTH].
     *  * If the provided `iconUrl` is not a properly formatted http or https url.
     *
     *
     * @return the builder after the author has been set
     */
    @Nonnull
    @JsonIgnore
    fun setAuthor(name: String?, url: String?, iconUrl: String?): EmbedEditor {
        //We only check if the name is null because its presence is what determines if the
        // the author will appear in the embed.
        if (name == null) {
            author = null
        } else {
            author = Author().apply {
                this.name = name
                this.url = url
                this.iconUrl = iconUrl
            }
        }
        return this
    }

    /**
     * Sets the Footer of the embed without icon.
     *
     *
     * **[Example](https://raw.githubusercontent.com/DV8FromTheWorld/JDA/assets/assets/docs/embeds/12-setFooter.png)**
     *
     * @param  text
     * the text of the footer of the embed. If this is not set or set to null, the footer will not appear in the embed.
     *
     * @throws java.lang.IllegalArgumentException
     * If the length of `text` is longer than [net.dv8tion.jda.api.entities.MessageEmbed.TEXT_MAX_LENGTH].
     *
     * @return the builder after the footer has been set
     */
    @JsonIgnore
    fun setFooter(text: String?): EmbedEditor {
        return setFooter(text, null)
    }

    /**
     * Sets the Footer of the embed.
     *
     *
     * **[Example](https://raw.githubusercontent.com/DV8FromTheWorld/JDA/assets/assets/docs/embeds/12-setFooter.png)**
     *
     *
     * **Uploading images with Embeds**
     * <br></br>When uploading an <u>image</u>
     * (using [MessageChannel.sendFile(...)][net.dv8tion.jda.api.entities.MessageChannel.sendFile])
     * you can reference said image using the specified filename as URI `attachment://filename.ext`.
     *
     *
     * <u>Example</u>
     * <pre>`
     * MessageChannel channel; // = reference of a MessageChannel
     * EmbedBuilder embed = new EmbedBuilder();
     * InputStream file = new URL("https://http.cat/500").openStream();
     * embed.setFooter("Cool footer!", "attachment://cat.png") // we specify this in sendFile as "cat.png"
     * .setDescription("This is a cute cat :3");
     * channel.sendFile(file, "cat.png").embed(embed.build()).queue();
    `</pre> *
     *
     * @param  text
     * the text of the footer of the embed. If this is not set, the footer will not appear in the embed.
     * @param  iconUrl
     * the url of the icon for the footer
     *
     * @throws java.lang.IllegalArgumentException
     *
     *  * If the length of `text` is longer than [net.dv8tion.jda.api.entities.MessageEmbed.TEXT_MAX_LENGTH].
     *  * If the length of `iconUrl` is longer than [net.dv8tion.jda.api.entities.MessageEmbed.URL_MAX_LENGTH].
     *  * If the provided `iconUrl` is not a properly formatted http or https url.
     *
     *
     * @return the builder after the footer has been set
     */
    @Nonnull
    @JsonIgnore
    fun setFooter(text: String?, iconUrl: String?): EmbedEditor {
        // We only check if the text is null because its presence is what determines if the
        // footer will appear in the embed.

        if (text == null) footer = null
        else {
            footer = Footer(text)
            footer?.iconUrl = iconUrl
        }

        return this
    }

    /**
     * Copies the provided Field into a new Field for this builder.
     * <br></br>For additional documentation, see [.addField]
     *
     * @param  field
     * the field object to add
     *
     * @return the builder after the field has been added
     */
    @Nonnull
    @JsonIgnore
    fun addField(field: Field): EmbedEditor {
        fields.add(field)
        return this
    }

    /**
     * Adds a Field to the embed.
     *
     *
     * Note: If a blank string is provided to either `name` or `value`, the blank string is replaced
     * with [net.dv8tion.jda.api.EmbedBuilder.ZERO_WIDTH_SPACE].
     *
     *
     * **[Example of Inline](https://raw.githubusercontent.com/DV8FromTheWorld/JDA/assets/assets/docs/embeds/07-addField.png)**
     *
     * **[Example if Non-inline](https://raw.githubusercontent.com/DV8FromTheWorld/JDA/assets/assets/docs/embeds/08-addField.png)**
     *
     * @param  name
     * the name of the Field, displayed in bold above the `value`.
     * @param  value
     * the contents of the field.
     * @param  inline
     * whether or not this field should display inline.
     *
     * @throws java.lang.IllegalArgumentException
     *
     *  * If only `name` or `value` is set. Both must be set.
     *  * If the length of `name` is greater than [net.dv8tion.jda.api.entities.MessageEmbed.TITLE_MAX_LENGTH].
     *  * If the length of `value` is greater than [net.dv8tion.jda.api.entities.MessageEmbed.VALUE_MAX_LENGTH].
     *
     *
     * @return the builder after the field has been added
     */
    @JsonIgnore
    fun addField(name: String, value: String, inline: Boolean): EmbedEditor {
        val field = Field(name, value)
        field.inline = inline
        fields.add(field)
        return this
    }

    /**
     * Clears all fields from the embed, such as those created with the
     * [EmbedBuilder(MessageEmbed)][net.dv8tion.jda.api.EmbedBuilder]
     * constructor or via the
     * [addField][net.dv8tion.jda.api.EmbedBuilder.addField] methods.
     *
     * @return the builder after the field has been added
     */
    @JsonIgnore
    fun clearFields(): EmbedEditor {
        fields.clear()
        return this
    }

    companion object {
        fun urlCheck(field: String, url: String?) {
            if (url != null) {
                if (url.length > MessageEmbed.URL_MAX_LENGTH) {
                    throw TooLongUrlVariableException(field, url, MessageEmbed.URL_MAX_LENGTH)
                }
                if (!EmbedBuilder.URL_PATTERN.matcher(url).matches()) {
                    throw InvalidUrlVariableException(field, url)
                }
            }
        }
    }

    /**
     * Returns a [MessageEmbed][net.dv8tion.jda.api.entities.MessageEmbed]
     * that has been checked as being valid for sending.
     *
     * @throws java.lang.IllegalStateException
     * If the embed is empty. Can be checked with [.isEmpty].
     *
     * @return the built, sendable [net.dv8tion.jda.api.entities.MessageEmbed]
     */
    @JsonIgnore
    fun build(): MessageEmbed {
        check(!isEmpty()) { "Cannot build an empty embed!" }
        val finalDesc = description
        check((finalDesc?.length ?: 0) <= MessageEmbed.TEXT_MAX_LENGTH) {
            String.format(
                "Description is longer than %d! Please limit your input!",
                MessageEmbed.TEXT_MAX_LENGTH
            )
        }
        check(length() <= MessageEmbed.EMBED_MAX_LENGTH_BOT) { "Cannot build an embed with more than " + MessageEmbed.EMBED_MAX_LENGTH_BOT + " characters!" }
        val description = if (finalDesc == null || finalDesc.isEmpty()) null else finalDesc
        val convertedThumb = thumbnail?.let {
            MessageEmbed.Thumbnail(it.url, it.proxyUrl, it.width ?: 0, it.height ?: 0)
        }
        val convertedAuthor = author?.let {
            MessageEmbed.AuthorInfo(it.name, it.url, it.iconUrl, it.proxyIconUrl)
        }
        val convertedFooter = footer?.let {
            MessageEmbed.Footer(it.text, it.iconUrl, it.proxyIconUrl)
        }
        val convertedImage = image?.let {
            MessageEmbed.ImageInfo(it.url, it.proxyUrl, it.width ?: 0, it.height ?: 0)
        }
        val convertedFields = fields.map {
            MessageEmbed.Field(it.name, it.value, it.inline ?: false)
        }
        // BRB
        return EntityBuilder.createMessageEmbed(
            titleUrl,
            title,
            description,
            EmbedType.RICH,
            timestamp,
            color ?: 0x202225,
            convertedThumb,
            null,
            convertedAuthor,
            null,
            convertedFooter,
            convertedImage,
            LinkedList(convertedFields)
        )
    }

    class Footer(
        @JsonProperty("text")
        val text: String
    ) {

        @JsonProperty("icon_url")
        var iconUrl: String? = null

        @JsonProperty("proxy_icon_url")
        var proxyIconUrl: String? = null
    }

    inner class Field(
        @JsonProperty("name")
        val name: String,

        @JsonProperty("value")
        val value: String
    ) {
        @JsonProperty("inline")
        var inline: Boolean? = null
    }

    inner class Image {
        @JsonProperty("url")
        var url: String? = null

        @JsonProperty("proxy_url")
        var proxyUrl: String? = null

        @JsonProperty("height")
        var height: Int? = null

        @JsonProperty("width")
        var width: Int? = null
    }

    inner class Provider {
        @JsonProperty("name")
        var name: String? = null

        @JsonProperty("url")
        var url: String? = null
    }

    inner class Author {
        @JsonProperty("name")
        var name: String? = null

        @JsonProperty("url")
        var url: String? = null

        @JsonProperty("icon_url")
        var iconUrl: String? = null

        @JsonProperty("proxy_icon_url")
        var proxyIconUrl: String? = null
    }

    inner class Thumbnail {
        @JsonProperty("url")
        var url: String? = null

        @JsonProperty("proxy_url")
        var proxyUrl: String? = null

        @JsonProperty("height")
        var height: Int? = null

        @JsonProperty("width")
        var width: Int? = null
    }

    inner class Video {
        @JsonProperty("url")
        var url: String? = null

        @JsonProperty("proxy_url")
        var proxyUrl: String? = null

        @JsonProperty("height")
        var height: Int? = null

        @JsonProperty("width")
        var width: Int? = null
    }
}
