import fr.xpdustry.toxopid.dsl.mindustryDependencies
import fr.xpdustry.toxopid.spec.ModMetadata
import fr.xpdustry.toxopid.spec.ModPlatform
import fr.xpdustry.toxopid.task.GithubArtifactDownload

plugins {
    kotlin("jvm") version "1.9.10"
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

project.group = "kennarddh"
project.version = metadata.version

val exposedVersion: String = "0.46.0"

dependencies {
    mindustryDependencies()

    compileOnly("com.xpdustry:kotlin-runtime:3.1.0-k.1.9.10")
    compileOnly("kennarddh:genesis-core:1.1.0")
    compileOnly("kennarddh:genesis-common:1.1.0")

    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")

    implementation("org.mariadb.jdbc:mariadb-java-client:3.3.1")
    implementation("com.zaxxer:HikariCP:5.1.0")
}

kotlin {
    jvmToolchain(17)
}

sourceSets {
    main {
        java.srcDir("src/main/kotlin")
    }
}

configurations.runtimeClasspath {
    exclude(group = "org.jetbrains.kotlin")
    exclude(group = "org.jetbrains.kotlinx")
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
        version.set("v3.1.0-k.1.9.10")
    }

val downloadDistributorCore =
    tasks.register<GithubArtifactDownload>("downloadDistributor") {
        user.set("xpdustry")
        repo.set("distributor")
        name.set("distributor-core.jar")
        version.set("v3.3.0")
    }

val downloadGenesisCore =
    tasks.register<GithubArtifactDownload>("downloadGenesisCore") {
        user.set("kennarddh-mindustry")
        repo.set("genesis")
        name.set("genesis-core-1.1.0.jar")
        version.set("v1.1.0")
    }

val downloadGenesisCommon =
    tasks.register<GithubArtifactDownload>("downloadGenesisCommon") {
        user.set("kennarddh-mindustry")
        repo.set("genesis")
        name.set("genesis-common-1.1.0.jar")
        version.set("v1.1.0")
    }

tasks.runMindustryServer {
    environment("DB_HOST", "jdbc:mariadb://localhost:3307/toast")
    environment("DB_USERNAME", "root")
    environment("DB_PASSWORD", "root")

    mods.setFrom(
        setOf(
            tasks.jar,
            downloadKotlinRuntime,
            downloadDistributorCore,
            downloadGenesisCore,
            downloadGenesisCommon
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