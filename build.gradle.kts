import com.apollographql.apollo.gradle.api.ApolloExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    repositories {
        jcenter()
    }
    dependencies {
        classpath("com.apollographql.apollo:apollo-gradle-plugin:2.4.1")
    }
}

plugins {
    id("application")
    id("com.github.johnrengelman.shadow") version "6.1.0"
    id("org.jetbrains.kotlin.jvm") version "1.4.20"
    id("com.apollographql.apollo") version "2.4.1"
}

application {
    mainClassName = "me.melijn.melijnbot.MelijnBotKt"
}
group = "me.melijn.melijnbot"
version = "2.0.8"

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_15
    targetCompatibility = JavaVersion.VERSION_15
}

configure<ApolloExtension> {
    generateKotlinModels.set(true) // or false for Java models

    service("anilist") {
        rootPackageName.set("me.melijn.melijnbot.anilist")
        sourceFolder.set("me/melijn/melijnbot/anilist")
    }

    customTypeMapping.map {
        ("StartDate" to "me.melijn.melijnbot.internals.models.AnilistDateKt")
        ("MediaFragment.StartDate" to "me.melijn.melijnbot.internals.models.AnilistDateKt")
    }
//    service("kitsu") {
//        rootPackageName = "me.melijn.melijnbot.kitsu"
//        sourceFolder = "me/melijn/melijnbot/kitsu"
//    }
}


repositories {
    jcenter()
    maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots/") }
    maven { url = uri("https://jitpack.io") }
    mavenCentral()
    mavenLocal()
}


dependencies {
    // https://bintray.com/dv8fromtheworld/maven/JDA/
    implementation("net.dv8tion:JDA:4.2.0_222") {
        exclude("opus-java")
    }


    // https://bintray.com/sedmelluq/com.sedmelluq/jda-nas
    implementation("com.sedmelluq:jda-nas:1.1.0")

    // https://bintray.com/sedmelluq/com.sedmelluq/lavaplayer
    implementation("com.github.ToxicMushroom:lavaplayer-test:a2cd883a06")
    // implementation("com.github.Melijn:lavaplayer:18000a1479")

    // https://jitpack.io/#ToxicMushroom/Lavalink-Klient
    implementation("com.github.ToxicMushroom:Lavalink-Klient:2.0.0-b1")

    api(kotlin("script-util"))
    api(kotlin("compiler"))
    api(kotlin("scripting-compiler"))

    // https://mvnrepository.com/artifact/org.jetbrains.kotlin/kotlin-stdlib-jdk8
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.4.20")

    // https://github.com/ToxicMushroom/JagTag-Kotlin
    implementation("com.github.ToxicMushroom:JagTag-Kotlin:0.6.4")

    // https://search.maven.org/artifact/com.zaxxer/HikariCP
    implementation("com.zaxxer:HikariCP:3.4.5")

    // https://mvnrepository.com/artifact/org.postgresql/postgresql
    implementation("org.postgresql:postgresql:42.2.18")

    // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-coroutines-core
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")

    // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-coroutines-jdk8
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.4.2")

    // https://bintray.com/duncte123/weeb.java/weeb.java
    implementation("me.duncte123:weebJava:2.2.0_29")

    // https://mvnrepository.com/artifact/se.michaelthelin.spotify/spotify-web-api-java
    implementation("se.michaelthelin.spotify:spotify-web-api-java:6.5.0")

    // https://mvnrepository.com/artifact/ch.qos.logback/logback-classic
    implementation("ch.qos.logback:logback-classic:1.2.3")


    // https://mvnrepository.com/artifact/com.fasterxml.jackson.core/jackson-core
    implementation("com.fasterxml.jackson.core:jackson-core:2.12.0")

    // https://github.com/FasterXML/jackson-module-kotlin
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.12.0")

    // https://search.maven.org/artifact/com.fasterxml.jackson.dataformat/jackson-dataformat-xml
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:2.12.0")


    // https://mvnrepository.com/artifact/io.ktor/ktor-client-cio
    implementation("io.ktor:ktor:1.4.3")
    implementation("io.ktor:ktor-client-okhttp:1.4.3")
    implementation("io.ktor:ktor-server-netty:1.4.3")


    implementation("com.github.ToxicMushroom:gifencoder:23b3128881")
    implementation("com.github.ToxicMushroom:animated-gif-lib-for-java:03e397e311")

    // https://github.com/GSculerlor/JikanKt/releases
    // implementation("com.github.GSculerlor:JikanKT:1.3.1")
    implementation("com.github.Melijn:JikanKt:dd5884b643")

    implementation("org.mariuszgromada.math:MathParser.org-mXparser:4.4.2")


    implementation("com.apollographql.apollo:apollo-runtime:2.4.1")
    implementation("com.apollographql.apollo:apollo-coroutines-support:2.4.1")

    // implementation("com.github.husnjak:IGDB-API-JVM:0.7")
    implementation("io.lettuce:lettuce-core:5.3.4.RELEASE")

    // https://github.com/cdimascio/dotenv-kotlin
    implementation("io.github.cdimascio:dotenv-kotlin:6.2.2")
}

tasks {
    withType(JavaCompile::class) {
        options.encoding = "UTF-8"
    }
    withType(KotlinCompile::class) {
        kotlinOptions {
            // 15 not supported at this time 25/10/2020 | latest kotlin: 1.4.10
            jvmTarget = "14"
        }
    }

    shadowJar {
        archiveFileName.set("melijn.jar")
    }
}
