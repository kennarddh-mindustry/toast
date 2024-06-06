package com.github.kennarddh.mindustry.toast.discord.listeners

import com.github.kennarddh.mindustry.toast.common.GameMode
import com.github.kennarddh.mindustry.toast.common.MapReviewStatus
import com.github.kennarddh.mindustry.toast.common.database.Database
import com.github.kennarddh.mindustry.toast.common.database.tables.Map
import com.github.kennarddh.mindustry.toast.common.database.tables.Users
import com.github.kennarddh.mindustry.toast.common.extensions.selectOne
import com.github.kennarddh.mindustry.toast.discord.*
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.utils.FileUpload
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.statements.api.ExposedBlob
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import javax.imageio.ImageIO


object MapListener : ListenerAdapter() {
    const val MAX_MAP_FILE_SIZE_IN_BYTES = 1024 * 1024
    const val MAX_MAP_WIDTH = 1500
    const val MAX_MAP_HEIGHT = 1500

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (event.name == "map") {
            if (event.subcommandName == "submit") {
                CoroutineScopes.IO.launch {
                    event.reply("Processing").setEphemeral(true).queue()

                    val mapAttachment = event.getOption("msav-file")!!.asAttachment
                    val gameModeString = event.getOption("game-mode")!!.asString
                    val obsoletedMapID = event.getOption("obsoleted-map-id")

                    val gameMode = try {
                        GameMode.valueOf(gameModeString)
                    } catch (error: IllegalArgumentException) {
                        return@launch event.hook.editOriginal("$gameModeString is not a valid game mode").queue()
                    }

                    if (mapAttachment.size > MAX_MAP_FILE_SIZE_IN_BYTES)
                        return@launch event.hook.editOriginal("Map size must not be bigger than 1mb.").queue()

                    val user = Database.newTransaction {
                        Users.selectOne { Users.discordID eq event.user.id }
                    }

                    if (user == null) {
                        return@launch event.hook.editOriginal("Your must verify your discord account before using this.")
                            .queue()
                    }

                    if (event.guild == null) {
                        event.hook.editOriginal("Cannot use outside guild").queue()

                        return@launch
                    }

                    val bytes = mapAttachment.proxy.download().await().use(InputStream::readBytes)

                    val (meta, preview) = mindustryContentHandler
                        .getMapMetadataWithPreview(bytes.inputStream())
                        .getOrElse {
                            event.hook
                                .editOriginal("Error ${it.message ?: "Unknown"}")
                                .queue()

                            null
                        } ?: return@launch


                    if (meta.width > MAX_MAP_WIDTH || meta.height > MAX_MAP_HEIGHT) {
                        return@launch event.hook.editOriginal(
                            "The map is bigger than ${MAX_MAP_WIDTH}x${MAX_MAP_HEIGHT}."
                        ).queue()
                    }

                    Database.newTransaction {
                        Map.insert {
                            it[name] = meta.name
                            it[description] = meta.description ?: "-"
                            it[author] = meta.author ?: "Unknown"
                            it[width] = meta.width
                            it[height] = meta.height
                            it[file] = ExposedBlob(bytes)
                            it[submittedByUserID] = user[Users.id]
                            it[active] = false
                            it[reviewStatus] = MapReviewStatus.Pending
                        }
                    }

                    val message =
                        event.channel
                            .sendMessage(
                                MessageCreate {
                                    files += FileUpload.fromStreamSupplier(mapAttachment.fileName, bytes::inputStream)
                                    files += FileUpload.fromStreamSupplier("map.png") {
                                        val previewOutputStream = ByteArrayOutputStream()

                                        ImageIO.write(preview, "png", previewOutputStream)

                                        ByteArrayInputStream(previewOutputStream.toByteArray())
                                    }
                                    embeds += Embed() {
                                        color = DiscordConstant.MAP_SUBMISSION_PENDING_EMBED_COLOR
                                        title = "Map Submission"
                                        author(
                                            event.user.name,
                                            event.user.effectiveAvatarUrl,
                                            event.user.effectiveAvatarUrl
                                        )
                                        field("Name", meta.name, false)
                                        field("Game Mode", gameMode.displayName, false)
                                        field("Author", meta.author ?: "Unknown", false)
                                        field(
                                            "Description",
                                            meta.description ?: "-",
                                            false
                                        )
                                        field("Size", "${meta.width}x${meta.height}", false)
                                        if (obsoletedMapID != null) {
                                            field("Obsoleted Map ID", "`${obsoletedMapID.asInt}`", false)
                                        }
                                        image = "attachment://map.png"
                                    }
                                    components +=
                                        ActionRow.of(
                                            Button.primary("map-submit-accept", "Accept"),
                                            Button.danger("map-submit-reject", "Reject")
                                        )
                                })
                            .await()

                    message
                        .createThreadChannel(meta.name)
                        .setAutoArchiveDuration(ThreadChannel.AutoArchiveDuration.TIME_3_DAYS)
                        .await()

                    event.hook.editOriginal(
                        "Submitted ${message.jumpUrl}."
                    ).queue()
                }
            }
        }
    }
}
