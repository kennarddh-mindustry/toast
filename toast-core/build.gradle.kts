import fr.xpdustry.toxopid.dsl.mindustryDependencies
import fr.xpdustry.toxopid.spec.ModMetadata
import fr.xpdustry.toxopid.spec.ModPlatform
import fr.xpdustry.toxopid.task.GithubArtifactDownload

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization") version "1.9.22"
    java
    `maven-publish`
    id("fr.xpdustry.toxopid") version "3.2.0"
}

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://maven.xpdustry.com/releases")
    maven("https://maven.xpdustry.com/mindustry")

    maven {
        url = uri("http://23.95.107.12:9999/releases")
        isAllowInsecureProtocol = true
    }
}

toxopid {
    // The version with which your mod/plugin is compiled.
    // If not set, will compile with v143 by default.
    compileVersion.set("v146")
    // The version with which your mod/plugin is tested.
    // If not set, defaults to the value of compileVersion.
    runtimeVersion.set("v146")

    // The platforms you target, you can choose DESKTOP, HEADLESS or/and ANDROID.
    // If not set, will target DESKTOP by default.
    platforms.add(ModPlatform.HEADLESS)
}

val metadata = ModMetadata.fromJson(project.file("plugin.json"))

project.group = "com.github.kennarddh.mindustry"
project.version = metadata.version

val genesisVersion = "3.0.0-beta.1"

dependencies {
    mindustryDependencies()

    implementation(project(":toast-common"))

    compileOnly("com.xpdustry:kotlin-runtime:3.1.1-k.1.9.22")
    compileOnly("com.github.kennarddh.mindustry:genesis-core:$genesisVersion")
    compileOnly("com.github.kennarddh.mindustry:genesis-standard:$genesisVersion")

    implementation(platform("org.jetbrains.exposed:exposed-bom:0.46.0"))
    implementation("org.jetbrains.exposed:exposed-java-time")
    implementation("org.jetbrains.exposed:exposed-core")
    implementation("org.jetbrains.exposed:exposed-jdbc")

    implementation("org.mariadb.jdbc:mariadb-java-client:3.3.2")
    implementation("com.zaxxer:HikariCP:5.1.0")

    implementation("com.password4j:password4j:1.7.3")

    implementation("com.rabbitmq:amqp-client:5.20.0")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

    implementation("org.slf4j:slf4j-api:2.0.11")
}

kotlin {
    jvmToolchain(17)
}

java {
    withSourcesJar()
    withJavadocJar()
}

sourceSets {
    main {
        java.srcDir("src/main/kotlin")
    }
}

configurations.runtimeClasspath {
    exclude("org.jetbrains.kotlin", "kotlin-stdlib")
    exclude("org.jetbrains.kotlin", "kotlin-stdlib-common")
    exclude("org.jetbrains.kotlin", "kotlin-stdlib-jdk8")
    exclude("org.jetbrains.kotlin", "kotlin-reflect")
    exclude("org.jetbrains.kotlinx", "kotlinx-coroutines-core")
    exclude("org.jetbrains.kotlinx", "kotlinx-coroutines-jdk8")
    exclude("org.slf4j")
}

tasks {
    jar {
        doFirst {
            val metadataFile = temporaryDir.resolve("plugin.json")

            metadataFile.writeText(metadata.toJson(true))

            from(metadataFile)
        }

        manifest {
            attributes(mapOf("Multi-Release" to "true"))
        }

        duplicatesStrategy = DuplicatesStrategy.EXCLUDE

        from(sourceSets.main.get().output)
        from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    }
}

val downloadKotlinRuntime =
    tasks.register<GithubArtifactDownload>("downloadKotlinRuntime") {
        user.set("xpdustry")
        repo.set("kotlin-runtime")
        name.set("kotlin-runtime.jar")
        version.set("v3.1.1-k.1.9.22")
    }

val downloadGenesisCore =
    tasks.register<GithubArtifactDownload>("downloadGenesisCore") {
        user.set("kennarddh-mindustry")
        repo.set("genesis")
        name.set("genesis-core-$genesisVersion.jar")
        version.set("v$genesisVersion")
    }

val downloadGenesisStandard =
    tasks.register<GithubArtifactDownload>("downloadGenesisStandard") {
        user.set("kennarddh-mindustry")
        repo.set("genesis")
        name.set("genesis-standard-$genesisVersion.jar")
        version.set("v$genesisVersion")
    }

tasks.runMindustryServer {
    environment("DB_HOST", "jdbc:mariadb://localhost:3307/toast")
    environment("DB_USERNAME", "root")
    environment("DB_PASSWORD", "root")
    environment("RABBITMQ_URI", "amqp://root:root@localhost:5672")
    environment("SERVER", "Survival")

    mods.setFrom(
        setOf(
            tasks.jar,
            downloadKotlinRuntime,
            downloadGenesisCore,
            downloadGenesisStandard
        )
    )
}

publishing {
    repositories {
        maven {
            name = "reposilite"
            url = uri("http://23.95.107.12:9999/releases")
            credentials(PasswordCredentials::class)
            authentication {
                create<BasicAuthentication>("basic")
            }
            isAllowInsecureProtocol = true
        }
    }
    publications {
        create<MavenPublication>("maven") {
            groupId = groupId
            artifactId = metadata.name
            version = version
            from(components["java"])
        }
    }
}

tasks.register("getArtifactPath") {
    doLast { println(tasks.jar.get().archiveFile.get().toString()) }
}