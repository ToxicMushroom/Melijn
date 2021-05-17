import com.apollographql.apollo.gradle.api.ApolloExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("application")
    id("com.apollographql.apollo") version "2.5.6"
    id("com.github.johnrengelman.shadow") version "7.0.0"
    kotlin("jvm") version "1.5.0"
}

application.mainClass.set("me.melijn.melijnbot.MelijnBotKt")
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
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://m2.dv8tion.net/releases")
        name = "m2-dv8tion"
    }
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
    maven("https://duncte123.jfrog.io/artifactory/maven")
    maven("https://nexus.melijn.com/repository/maven-public/")
    maven("https://nexus.melijn.com/repository/jcenter-mirror/")
    mavenLocal()
}

val jackson = "2.12.3" // https://mvnrepository.com/artifact/com.fasterxml.jackson.core/jackson-core
val ktor = "1.5.4"   // https://mvnrepository.com/artifact/io.ktor/ktor-client-cio
val apollo = "2.5.6" // https://mvnrepository.com/artifact/com.apollographql.apollo/apollo-runtime
val kotlinX = "1.5.0" // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-coroutines-core
val kotlin = "1.5.0"

dependencies {
    // https://ci.dv8tion.net/job/JDA/
    implementation("net.dv8tion:JDA:4.2.1_264") {
        exclude("opus-java")
    }

    // https://github.com/sedmelluq/jda-nas
    implementation("com.sedmelluq:jda-nas:1.1.0")

    // https://github.com/sedmelluq/lavaplayer
    implementation("com.sedmelluq:lavaplayer:1.3.76")

    // https://nexus.melijn.com/#browse/browse:maven-public:me%2Fmelijn%2Fllklient%2FLavalink-Klient
    implementation("me.melijn.llklient:Lavalink-Klient:2.1.8")

    api(kotlin("script-util"))
    api(kotlin("compiler"))
    api(kotlin("scripting-compiler"))

    // https://mvnrepository.com/artifact/org.jetbrains.kotlin/kotlin-stdlib-jdk8
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlin")

    // https://nexus.melijn.com/#browse/browse:maven-public:me%2Fmelijn%2Fjagtag
    implementation("me.melijn.jagtag:JagTag-Kotlin:2.1.5")

    // https://search.maven.org/artifact/com.zaxxer/HikariCP
    implementation("com.zaxxer:HikariCP:4.0.3")

    // https://mvnrepository.com/artifact/org.postgresql/postgresql
    implementation("org.postgresql:postgresql:42.2.20")

    // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-coroutines-core
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinX")

    // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-coroutines-jdk8
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$kotlinX")

    // https://duncte123.jfrog.io/ui/packages/gav:%2F%2Fme.duncte123:weebJava
    implementation("me.duncte123:weebJava:3.0.1_4")

    // https://mvnrepository.com/artifact/se.michaelthelin.spotify/spotify-web-api-java
    implementation("se.michaelthelin.spotify:spotify-web-api-java:6.5.4")

    // https://mvnrepository.com/artifact/ch.qos.logback/logback-classic
    implementation("ch.qos.logback:logback-classic:1.2.3")


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

    // https://nexus.melijn.com/#browse/browse:maven-public:me%2Fmelijn%2Fgifencoder
    implementation("me.melijn.gifencoder:gifencoder:1.0.0")

    // https://nexus.melijn.com/#browse/browse:maven-public:me%2Fmelijn%2Fgifdecoder
    implementation("me.melijn.gifdecoder:animated-gif-lib-for-java:1.0.1")

    // https://nexus.melijn.com/#browse/browse:maven-public:me%2Fmelijn%2Fjikankt
    implementation("me.melijn.jikankt:JikanKt:1.3.2")

    // https://mvnrepository.com/artifact/org.mariuszgromada.math/MathParser.org-mXparser
    implementation("org.mariuszgromada.math:MathParser.org-mXparser:4.4.2")

    // https://mvnrepository.com/artifact/com.apollographql.apollo/apollo-runtime
    implementation("com.apollographql.apollo:apollo-runtime:$apollo")
    implementation("com.apollographql.apollo:apollo-coroutines-support:$apollo")

    // https://mvnrepository.com/artifact/io.lettuce/lettuce-core
    implementation("io.lettuce:lettuce-core:6.1.1.RELEASE")

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
        archiveFileName.set("melijn.jar")
    }
}
