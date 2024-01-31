import com.apollographql.apollo.gradle.api.ApolloExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("application")
    id("com.apollographql.apollo") version "2.5.11"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    kotlin("jvm") version "1.9.22"
}

application.mainClass.set("me.melijn.melijnbot.MelijnBotKt")
group = "me.melijn.melijnbot"
version = "2.1.0"

configure<JavaPluginExtension> {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
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
    mavenLocal()
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
    maven("https://reposilite.melijn.com/releases/")
    maven("https://reposilite.melijn.com/snapshots/")
    maven("https://m2.duncte123.dev/releases")
    maven("https://reposilite.melijn.com/shitpack/") // pooppack mirror
}

val jackson = "2.13.2" // https://mvnrepository.com/artifact/com.fasterxml.jackson.core/jackson-core
val ktor = "1.6.8"   // https://mvnrepository.com/artifact/io.ktor/ktor-client-cio
val apollo = "2.5.11" // https://mvnrepository.com/artifact/com.apollographql.apollo/apollo-runtime
val kotlinX = "1.6.1" // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-coroutines-core
val kotlin = "1.8.20"
val scrimage = "4.0.31"

dependencies {
    // https://ci.dv8tion.net/job/JDA/
    implementation("net.dv8tion:JDA:4.4.1_353") {
        exclude("opus-java")
        exclude("nv-websocket-client")
    }

    implementation("com.neovisionaries:nv-websocket-client:3.0.0-SNAPSHOT")

    implementation("io.sentry:sentry:6.4.2")

    // https://mvnrepository.com/artifact/club.minnced/discord-webhooks
    implementation("club.minnced:discord-webhooks:0.8.4")

    // https://github.com/freya022/JEmojis
    implementation("com.github.ToxicMushroom:JEmojis:a8c82848f166893f67251c741579c74c80fbb2dd")

    // required for scriptDef.kt
    implementation(kotlin("scripting-common"))
    implementation(kotlin("scripting-jvm"))
    implementation(kotlin("scripting-dependencies"))
    implementation(kotlin("scripting-dependencies-maven"))

    // required for host.kt
    implementation(kotlin("scripting-jvm-host"))

    // https://mvnrepository.com/artifact/org.jetbrains.kotlin/kotlin-stdlib-jdk8
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlin")

    // https://reposilite.melijn.com/#/releases/me/melijn/jagtag/JagTag-Kotlin
    implementation("me.melijn.jagtag:JagTag-Kotlin:2.2.2")

    // https://search.maven.org/artifact/com.zaxxer/HikariCP
    implementation("com.zaxxer:HikariCP:5.1.0")

    // https://mvnrepository.com/artifact/org.postgresql/postgresql
    implementation("org.postgresql:postgresql:42.7.1")

    // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-coroutines-core
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinX")

    // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-coroutines-jdk8
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$kotlinX")

    // https://duncte123.jfrog.io/ui/packages/gav:%2F%2Fme.duncte123:weebJava
    implementation("me.duncte123:weebJava:3.0.1_7")

    // https://mvnrepository.com/artifact/ch.qos.logback/logback-classic
    implementation("ch.qos.logback:logback-classic:1.4.14")

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

    // https://mvnrepository.com/artifact/org.mariuszgromada.math/MathParser.org-mXparser
    implementation("org.mariuszgromada.math:MathParser.org-mXparser:5.2.1")

    // https://mvnrepository.com/artifact/com.apollographql.apollo/apollo-runtime
    implementation("com.apollographql.apollo:apollo-runtime:$apollo")
    implementation("com.apollographql.apollo:apollo-coroutines-support:$apollo")

    // https://mvnrepository.com/artifact/io.lettuce/lettuce-core
    implementation("io.lettuce:lettuce-core:6.3.1.RELEASE")

    // https://github.com/cdimascio/dotenv-kotlin
    implementation("io.github.cdimascio:dotenv-kotlin:6.3.1")
}

tasks {
    withType(JavaCompile::class) {
        options.encoding = "UTF-8"
        options.compilerArgs = listOf("--add-opens java.base/java.lang=ALL-UNNAMED")
    }
    withType(KotlinCompile::class) {
        kotlinOptions {
            jvmTarget = "21"
        }
    }

    shadowJar {
        mergeServiceFiles()
        archiveFileName.set("melijn.jar")
    }
}
