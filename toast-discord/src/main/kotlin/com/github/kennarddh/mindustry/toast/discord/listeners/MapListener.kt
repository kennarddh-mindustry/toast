package com.github.kennarddh.mindustry.toast.discord.listeners

import com.github.kennarddh.mindustry.toast.common.GameMode
import com.github.kennarddh.mindustry.toast.common.MapReviewStatus
import com.github.kennarddh.mindustry.toast.common.UserRole
import com.github.kennarddh.mindustry.toast.common.database.Database
import com.github.kennarddh.mindustry.toast.common.database.tables.Map
import com.github.kennarddh.mindustry.toast.common.database.tables.Users
import com.github.kennarddh.mindustry.toast.common.extensions.selectOne
import com.github.kennarddh.mindustry.toast.common.extensions.toDisplayString
import com.github.kennarddh.mindustry.toast.common.messaging.Messenger
import com.github.kennarddh.mindustry.toast.common.messaging.messages.MapUpdateServerControl
import com.github.kennarddh.mindustry.toast.common.messaging.messages.ServerControl
import com.github.kennarddh.mindustry.toast.discord.*
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.utils.FileUpload
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.statements.api.ExposedBlob
import org.jetbrains.exposed.sql.update
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.URL
import javax.imageio.ImageIO


object MapListener : ListenerAdapter() {
    private const val MAP_SUBMIT_ACCEPT_BUTTON_COMPONENT_ID_PREFIX = "map-submit-accept-v1-"
    private const val MAP_SUBMIT_REJECT_BUTTON_COMPONENT_ID_PREFIX = "map-submit-reject-v1-"

    private const val MAX_MAP_FILE_SIZE_IN_BYTES = 1024 * 1024
    private const val MAX_MAP_WIDTH = 1500
    private const val MAX_MAP_HEIGHT = 1500

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (event.name != "map") return

        if (event.subcommandName == "list") {
            CoroutineScopes.IO.launch {
                val gameModeString = event.getOption("game-mode")!!.asString

                val gameMode = try {
                    GameMode.valueOf(gameModeString)
                } catch (error: IllegalArgumentException) {
                    return@launch event.hook.editOriginal("$gameModeString is not a valid game mode").queue()
                }

                Database.newTransaction {
                    val maps =
                        Map.selectAll().where { Map.gameMode eq gameMode }

                    val response = buildString {
                        appendLine("## Maps for ${gameMode.displayName}")

                        maps.forEachIndexed() { index, map ->
                            appendLine("${index + 1}. ${map[Map.name]}")
                            appendLine("\tAuthor: ${map[Map.author]}")
                            appendLine("\tSize: ${map[Map.width]}x${map[Map.height]}")
                            appendLine("\tStatus: ${map[Map.reviewStatus].displayName}")

                            if (map[Map.reviewStatus] == MapReviewStatus.Accepted) {
                                appendLine("\tActive: ${map[Map.active].toDisplayString()}")
                            }
                        }
                    }

                    event.reply(response).queue()
                }
            }
        } else if (event.subcommandName == "enable") {
            CoroutineScopes.IO.launch {
                val user = Database.newTransaction {
                    Users.selectOne { Users.discordID eq event.user.id }
                } ?: return@launch event.reply("Your must verify your discord account before using this.")
                    .setEphemeral(true)
                    .queue()

                if (user[Users.role] < UserRole.Admin) {
                    return@launch event.reply("Your role must be greater than or equal to Admin.")
                        .setEphemeral(true)
                        .queue()
                }

                val mapID = event.getOption("map-id")!!.asInt

                Database.newTransaction {
                    val map = Map.selectOne { Map.id eq mapID }
                        ?: return@newTransaction event.reply("Map not found.")
                            .setEphemeral(true)
                            .queue()

                    if (map[Map.reviewStatus] == MapReviewStatus.Pending) {
                        event.reply("Map has not been reviewed.")
                            .setEphemeral(true)
                            .queue()
                    }

                    Map.update({ Map.id eq mapID }) {
                        it[active] = true
                    }

                    event.reply("Map `$mapID` enabled.")
                        .setEphemeral(true)
                        .queue()

                    Messenger.publishServerControl(
                        "map-update.${map[Map.gameMode].name}",
                        ServerControl(
                            Clock.System.now().toEpochMilliseconds(),
                            MapUpdateServerControl
                        )
                    )
                }
            }
        } else if (event.subcommandName == "disable") {
            CoroutineScopes.IO.launch {
                val user = Database.newTransaction {
                    Users.selectOne { Users.discordID eq event.user.id }
                } ?: return@launch event.reply("Your must verify your discord account before using this.")
                    .setEphemeral(true)
                    .queue()

                if (user[Users.role] < UserRole.Admin) {
                    return@launch event.reply("Your role must be greater than or equal to Admin.")
                        .setEphemeral(true)
                        .queue()
                }

                val mapID = event.getOption("map-id")!!.asInt

                Database.newTransaction {
                    val map = Map.selectOne { Map.id eq mapID }
                        ?: return@newTransaction event.reply("Map not found.")
                            .setEphemeral(true)
                            .queue()

                    if (map[Map.reviewStatus] == MapReviewStatus.Pending) {
                        event.reply("Map has not been reviewed.")
                            .setEphemeral(true)
                            .queue()
                    }

                    Map.update({ Map.id eq mapID }) {
                        it[active] = false
                    }

                    event.reply("Map `$mapID` disabled.")
                        .setEphemeral(true)
                        .queue()

                    Messenger.publishServerControl(
                        "map-update.${map[Map.gameMode].name}",
                        ServerControl(
                            Clock.System.now().toEpochMilliseconds(),
                            MapUpdateServerControl
                        )
                    )
                }
            }
        } else if (event.subcommandName == "info") {
            CoroutineScopes.IO.launch {
                event.deferReply().queue()

                val mapID = event.getOption("map-id")!!.asInt

                Database.newTransaction {
                    val map = Map.selectOne { Map.id eq mapID }
                        ?: return@newTransaction event.hook.editOriginal("Map not found.")
                            .queue()

                    val inputStream = map[Map.file].inputStream

                    val byteArrayOutputStream = ByteArrayOutputStream()

                    inputStream.transferTo(byteArrayOutputStream)

                    val inputStreamClone1 = ByteArrayInputStream(byteArrayOutputStream.toByteArray())
                    val inputStreamClone2 = ByteArrayInputStream(byteArrayOutputStream.toByteArray())

                    val preview = mindustryContentHandler
                        .getMapMetadataWithPreview(inputStreamClone1)
                        .getOrElse {
                            event.hook
                                .editOriginal("Error ${it.message ?: "Unknown"}")
                                .queue()

                            null
                        }?.second ?: return@newTransaction

                    val message =
                        MessageEdit {
                            files += FileUpload.fromStreamSupplier("${map[Map.id]}.msav") { inputStreamClone2 }
                            files += FileUpload.fromStreamSupplier("map.png") {
                                val previewOutputStream = ByteArrayOutputStream()

                                ImageIO.write(preview, "png", previewOutputStream)

                                ByteArrayInputStream(previewOutputStream.toByteArray())
                            }
                            embeds += Embed {
                                color = DiscordConstant.MAP_INFO_EMBED_COLOR
                                title = "Map Info"
                                author(
                                    event.user.name,
                                    event.user.effectiveAvatarUrl,
                                    event.user.effectiveAvatarUrl
                                )
                                field("Name", map[Map.name], false)
                                field("Game Mode", map[Map.gameMode].displayName, false)
                                field("Author", map[Map.author], false)
                                field(
                                    "Description",
                                    map[Map.description],
                                    false
                                )
                                field("Size", "${map[Map.width]}x${map[Map.height]}", false)
                                field("Active", map[Map.active].toDisplayString(), false)
                                image = "attachment://map.png"
                            }
                        }

                    event.hook.editOriginal(message).queue()
                }
            }
        } else if (event.subcommandName == "submit") {
            CoroutineScopes.IO.launch {
                event.reply("Processing").setEphemeral(true).queue()

                val mapAttachment = event.getOption("msav-file")!!.asAttachment
                val gameModeString = event.getOption("game-mode")!!.asString

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

                val mapID = Database.newTransaction {
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
                        it[this.gameMode] = gameMode
                    } get Map.id
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
                                embeds += Embed {
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
                                    image = "attachment://map.png"
                                }
                                components +=
                                    ActionRow.of(
                                        Button.primary(
                                            "$MAP_SUBMIT_ACCEPT_BUTTON_COMPONENT_ID_PREFIX$mapID",
                                            "Accept"
                                        ),
                                        Button.danger(
                                            "$MAP_SUBMIT_REJECT_BUTTON_COMPONENT_ID_PREFIX$mapID",
                                            "Reject"
                                        )
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

    override fun onButtonInteraction(event: ButtonInteractionEvent) {
        if (event.componentId.startsWith(MAP_SUBMIT_ACCEPT_BUTTON_COMPONENT_ID_PREFIX)) {
            CoroutineScopes.IO.launch {
                val user = Database.newTransaction {
                    Users.selectOne { Users.discordID eq event.user.id }
                } ?: return@launch event.reply("Your must verify your discord account before using this.")
                    .setEphemeral(true)
                    .queue()

                if (user[Users.role] < UserRole.Admin) {
                    return@launch event.reply("Your role must be greater than or equal to Admin.")
                        .setEphemeral(true)
                        .queue()
                }

                val mapID = event.componentId
                    .drop(MAP_SUBMIT_ACCEPT_BUTTON_COMPONENT_ID_PREFIX.length)
                    .toInt()

                Database.newTransaction {
                    val map = Map.selectOne { Map.id eq mapID }
                        ?: return@newTransaction event.reply("Map not found.")
                            .setEphemeral(true)
                            .queue()

                    if (map[Map.reviewStatus] != MapReviewStatus.Pending) {
                        event.reply("Map has already been reviewed.")
                            .setEphemeral(true)
                            .queue()
                    }

                    Map.update({ Map.id eq mapID }) {
                        it[reviewStatus] = MapReviewStatus.Accepted
                        it[reviewedByUserID] = user[Users.id]
                        it[reviewedAt] = Clock.System.now().toLocalDateTime(TimeZone.UTC)
                        it[active] = true
                    }

                    event.reply("Map `$mapID` accepted.")
                        .setEphemeral(true)
                        .queue()

                    Messenger.publishServerControl(
                        "map-update.${map[Map.gameMode].name}",
                        ServerControl(
                            Clock.System.now().toEpochMilliseconds(),
                            MapUpdateServerControl
                        )
                    )

                    event.message.editMessageEmbeds(
                        Embed(event.message.embeds.first()) {
                            color = DiscordConstant.MAP_SUBMISSION_ACCEPTED_EMBED_COLOR
                            field("Reviewer", event.member!!.asMention, false)
                            image = "attachment://map2.png"
                        }
                    )
                        .setComponents()
                        .setFiles(
                            FileUpload.fromStreamSupplier(event.message.attachments.first().fileName) {
                                event.message.attachments.first().proxy.download().join()
                            },
                            FileUpload.fromStreamSupplier("map2.png") {
                                URL(event.message.embeds.first().image!!.url).openStream()
                            }
                        )
                        .queue()
                }
            }
        } else if (event.componentId.startsWith(MAP_SUBMIT_REJECT_BUTTON_COMPONENT_ID_PREFIX)) {
            CoroutineScopes.IO.launch {
                val user = Database.newTransaction {
                    Users.selectOne { Users.discordID eq event.user.id }
                } ?: return@launch event.reply("Your must verify your discord account before using this.")
                    .setEphemeral(true)
                    .queue()

                if (user[Users.role] < UserRole.Admin) {
                    return@launch event.reply("Your role must be greater than or equal to Admin.")
                        .setEphemeral(true)
                        .queue()
                }

                val mapID = event.componentId
                    .drop(MAP_SUBMIT_REJECT_BUTTON_COMPONENT_ID_PREFIX.length)
                    .toInt()

                Database.newTransaction {
                    val map = Map.selectOne { Map.id eq mapID }
                        ?: return@newTransaction event.reply("Map not found.")
                            .setEphemeral(true)
                            .queue()

                    if (map[Map.reviewStatus] != MapReviewStatus.Pending) {
                        event.reply("Map has already been reviewed.")
                            .setEphemeral(true)
                            .queue()
                    }

                    Map.update({ Map.id eq mapID }) {
                        it[reviewStatus] = MapReviewStatus.Rejected
                        it[reviewedByUserID] = user[Users.id]
                        it[reviewedAt] = Clock.System.now().toLocalDateTime(TimeZone.UTC)
                        it[active] = true
                    }

                    event.reply("Map `$mapID` rejected.")
                        .setEphemeral(true)
                        .queue()

                    event.message.editMessageEmbeds(
                        Embed(event.message.embeds.first()) {
                            color = DiscordConstant.MAP_SUBMISSION_REJECTED_EMBED_COLOR
                            field("Reviewer", event.member!!.asMention, false)
                            image = "attachment://map2.png"
                        }
                    )
                        .setComponents()
                        .setFiles(
                            FileUpload.fromStreamSupplier(event.message.attachments.first().fileName) {
                                event.message.attachments.first().proxy.download().join()
                            },
                            FileUpload.fromStreamSupplier("map2.png") {
                                URL(event.message.embeds.first().image!!.url).openStream()
                            }
                        )
                        .queue()
                }
            }
        }
    }
}
