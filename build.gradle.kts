import com.apollographql.apollo.gradle.api.ApolloExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("application")
    id("com.apollographql.apollo") version "2.5.9"
    id("com.github.johnrengelman.shadow") version "7.1.0"
    kotlin("jvm") version "1.5.31"
}

application.mainClass.set("me.melijn.melijnbot.MelijnBotKt")
group = "me.melijn.melijnbot"
version = "2.1.0"

configure<JavaPluginExtension> {
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
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://m2.dv8tion.net/releases")
        name = "m2-dv8tion"
    }
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
    maven("https://nexus.melijn.com/repository/maven-public/")
    maven("https://nexus.melijn.com/repository/jcenter-mirror/")
    mavenLocal()
    maven("https://duncte123.jfrog.io/artifactory/maven")
    maven("https://nexus.melijn.com/repository/jitpack/") // pooppack mirror
}

val jackson = "2.13.0" // https://mvnrepository.com/artifact/com.fasterxml.jackson.core/jackson-core
val ktor = "1.6.4"   // https://mvnrepository.com/artifact/io.ktor/ktor-client-cio
val apollo = "2.5.9" // https://mvnrepository.com/artifact/com.apollographql.apollo/apollo-runtime
val kotlinX = "1.5.2-native-mt" // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-coroutines-core
val kotlin = "1.5.31"
val scrimage = "4.0.22"

dependencies {
    // https://ci.dv8tion.net/job/JDA/
    implementation("net.dv8tion:JDA:4.3.0_339") {
        exclude("opus-java")
    }

    implementation("io.sentry:sentry:5.2.2")

    // https://mvnrepository.com/artifact/club.minnced/discord-webhooks
    implementation("club.minnced:discord-webhooks:0.7.2")
    // https://github.com/freya022/JEmojis
    implementation("com.github.ToxicMushroom:JEmojis:a8c82848f166893f67251c741579c74c80fbb2dd")

    // https://github.com/sedmelluq/jda-nas
    implementation("com.sedmelluq:jda-nas:1.1.0")

    // https://github.com/sedmelluq/lavaplayer
    implementation("com.sedmelluq:lavaplayer:1.3.78")

    // https://nexus.melijn.com/#browse/browse:maven-public:me%2Fmelijn%2Fllklient%2FLavalink-Klient
    implementation("me.melijn.llklient:Lavalink-Klient:2.2.3")

    api("org.jetbrains.kotlin:kotlin-script-util:$kotlin")
    api("org.jetbrains.kotlin:kotlin-compiler:$kotlin")
    api("org.jetbrains.kotlin:kotlin-scripting-compiler:$kotlin")

    // https://mvnrepository.com/artifact/org.jetbrains.kotlin/kotlin-stdlib-jdk8
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlin")

    // https://nexus.melijn.com/#browse/browse:maven-public:me%2Fmelijn%2Fjagtag
    implementation("me.melijn.jagtag:JagTag-Kotlin:2.2.1")

    // https://search.maven.org/artifact/com.zaxxer/HikariCP
    implementation("com.zaxxer:HikariCP:5.0.0")

    // https://mvnrepository.com/artifact/org.postgresql/postgresql
    implementation("org.postgresql:postgresql:42.2.24")

    // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-coroutines-core
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinX")

    // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-coroutines-jdk8
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$kotlinX")

    // https://duncte123.jfrog.io/ui/packages/gav:%2F%2Fme.duncte123:weebJava
    implementation("me.duncte123:weebJava:3.0.1_4")

    // https://mvnrepository.com/artifact/se.michaelthelin.spotify/spotify-web-api-java
    implementation("se.michaelthelin.spotify:spotify-web-api-java:6.5.4")

    // https://mvnrepository.com/artifact/ch.qos.logback/logback-classic
    implementation("ch.qos.logback:logback-classic:1.2.6")

    // https://mvnrepository.com/artifact/com.fasterxml.jackson.core/jackson-core
    implementation("com.fasterxml.jackson.core:jackson-core:$jackson")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jackson")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:$jackson")

    // https://mvnrepository.com/artifact/io.ktor/ktor-client-cio
    implementation("io.ktor:ktor:$ktor")
    implementation("io.ktor:ktor-client-okhttp:$ktor")
    implementation("io.ktor:ktor-server-netty:$ktor")
    implementation("io.ktor:ktor-jackson:$ktor")
    implementation("io.ktor:ktor-client-jackson:$ktor")

    // https://nexus.melijn.com/#browse/browse:maven-public:me%2Fmelijn%2Fgifdecoder
    implementation("com.github.zh79325:open-gif:1.0.4")
    implementation("com.sksamuel.scrimage:scrimage-core:$scrimage")
    implementation("com.sksamuel.scrimage:scrimage-filters:$scrimage")
    implementation("com.sksamuel.scrimage:scrimage-webp:$scrimage")
    implementation("com.sksamuel.scrimage:scrimage-formats-extra:$scrimage")

    // https://nexus.melijn.com/#browse/browse:maven-public:me%2Fmelijn%2Fjikankt
    implementation("me.melijn.jikankt:JikanKt:1.3.2")

    // https://mvnrepository.com/artifact/org.mariuszgromada.math/MathParser.org-mXparser
    implementation("org.mariuszgromada.math:MathParser.org-mXparser:4.4.2")

    // https://mvnrepository.com/artifact/com.apollographql.apollo/apollo-runtime
    implementation("com.apollographql.apollo:apollo-runtime:$apollo")
    implementation("com.apollographql.apollo:apollo-coroutines-support:$apollo")

    // https://mvnrepository.com/artifact/io.lettuce/lettuce-core
    implementation("io.lettuce:lettuce-core:6.1.5.RELEASE")

    // https://github.com/cdimascio/dotenv-kotlin
    implementation("io.github.cdimascio:dotenv-kotlin:6.2.2")
}

tasks {
    withType(JavaCompile::class) {
        options.encoding = "UTF-8"
    }
    withType(KotlinCompile::class) {
        kotlinOptions {
            jvmTarget = "15"
        }
    }

    shadowJar {
        mergeServiceFiles()
        archiveFileName.set("melijn.jar")
    }
}
