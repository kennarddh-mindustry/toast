// https://github.com/xpdustry/imperium/blob/d0e1a84be6e93dc7a546de7a47a764a45cd0a627/imperium-discord/src/main/kotlin/com/xpdustry/imperium/discord/content/MindustryContentHandler.kt
package com.github.kennarddh.mindustry.toast.discord.content

import mindustry.game.Schematic
import java.awt.image.BufferedImage
import java.io.InputStream
import java.io.OutputStream

interface MindustryContentHandler {
    suspend fun getSchematic(stream: InputStream): Result<Schematic>

    suspend fun getSchematic(string: String): Result<Schematic>

    suspend fun getSchematicPreview(schematic: Schematic): Result<BufferedImage>

    suspend fun writeSchematic(schematic: Schematic, output: OutputStream): Result<Unit>

    suspend fun getMapMetadata(stream: InputStream): Result<MapMetadata>

    suspend fun getMapMetadataWithPreview(
        stream: InputStream
    ): Result<Pair<MapMetadata, BufferedImage>>
}

data class MapMetadata(
    val name: String,
    val description: String?,
    val author: String?,
    val width: Int,
    val height: Int,
    val tags: Map<String, String>,
)