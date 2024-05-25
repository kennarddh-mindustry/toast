package com.github.kennarddh.mindustry.toast.discord.listeners

import com.github.kennarddh.mindustry.toast.common.database.Database
import com.github.kennarddh.mindustry.toast.common.database.tables.MindustryUser
import com.github.kennarddh.mindustry.toast.common.database.tables.UserPunishments
import com.github.kennarddh.mindustry.toast.common.database.tables.UserReports
import com.github.kennarddh.mindustry.toast.common.database.tables.Users
import com.github.kennarddh.mindustry.toast.common.extensions.selectOne
import com.github.kennarddh.mindustry.toast.common.extensions.toDisplayString
import com.github.kennarddh.mindustry.toast.common.messaging.Messenger
import com.github.kennarddh.mindustry.toast.common.messaging.messages.*
import com.github.kennarddh.mindustry.toast.discord.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.session.ReadyEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.text.TextInput
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle
import net.dv8tion.jda.api.interactions.modals.Modal
import net.dv8tion.jda.api.utils.MarkdownSanitizer
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.alias
import org.jetbrains.exposed.sql.update


object GameEventsListener : ListenerAdapter() {
    private const val PARDON_BUTTON_COMPONENT_ID_PREFIX = "pardon-"
    private const val PARDON_MODAL_ID_PREFIX = "pardonModal-"

    override fun onReady(event: ReadyEvent) {
        Messenger.listenGameEvent("DiscordBotGameEvents", "#") {
            val channel = toastMindustryGuild.getTextChannelById(it.server.discordChannelID)!!

            val message = when (it.data) {
                is PlayerJoinGameEvent -> "${(it.data as PlayerJoinGameEvent).playerMindustryName} joined."
                is PlayerLeaveGameEvent -> "${(it.data as PlayerLeaveGameEvent).playerMindustryName} left."
                is PlayerChatGameEvent -> "[${(it.data as PlayerChatGameEvent).playerMindustryName}]: ${
                    MarkdownSanitizer.sanitize(
                        (it.data as PlayerChatGameEvent).message,
                        MarkdownSanitizer.SanitizationStrategy.ESCAPE
                    )
                }"

                is ServerStartGameEvent -> "Server start."
                is ServerStopGameEvent -> "Server stop."
                is ServerRestartGameEvent -> "Server restart."
                is PlayerReportedGameEvent -> {
                    val data = it.data as PlayerReportedGameEvent

                    CoroutineScopes.Main.launch {
                        val userReport = Database.newTransaction {
                            val targetUserAlias = Users.alias("targetUser")
                            val targetMindustryUserAlias = MindustryUser.alias("targetMindustryUser")

                            UserReports
                                .join(
                                    Users,
                                    JoinType.LEFT,
                                    onColumn = UserReports.userID,
                                    otherColumn = Users.id
                                )
                                .join(
                                    targetUserAlias,
                                    JoinType.LEFT,
                                    onColumn = UserReports.targetUserID,
                                    otherColumn = targetUserAlias[Users.id]
                                )
                                .join(
                                    MindustryUser,
                                    JoinType.LEFT,
                                    onColumn = UserReports.mindustryUserID,
                                    otherColumn = MindustryUser.id
                                )
                                .join(
                                    targetMindustryUserAlias,
                                    JoinType.LEFT,
                                    onColumn = UserReports.targetMindustryUserID,
                                    otherColumn = targetMindustryUserAlias[MindustryUser.id]
                                )
                                .selectOne {
                                    UserReports.id eq data.userReportID
                                }
                        }

                        if (userReport == null) {
                            Logger.error("Missing UserReports entry with the id ${data.userReportID}.")

                            return@launch
                        }

                        val embed = EmbedBuilder().run {
                            setTitle("Player Reported")

                            setColor(DiscordConstant.REPORTED_EMBED_COLOR)

                            addField(
                                MessageEmbed.Field(
                                    "Reporter",
                                    "`${data.name}`/`${userReport[UserReports.mindustryUserID]}`",
                                    true
                                )
                            )

                            addField(
                                MessageEmbed.Field(
                                    "Target",
                                    "`${data.targetPlayerMindustryName}`/`${userReport[UserReports.targetMindustryUserID]}`",
                                    true
                                )
                            )

                            addField(MessageEmbed.Field("Server", it.server.displayName, false))

                            addField(MessageEmbed.Field("Reason", userReport[UserReports.reason], false))

                            build()
                        }

                        reportsChannel.sendMessageEmbeds(embed).queue()
                    }

                    null
                }

                is PlayerPunishedGameEvent -> {
                    val data = it.data as PlayerPunishedGameEvent

                    CoroutineScopes.Main.launch {
                        val userPunishment = Database.newTransaction {
                            val targetUserAlias = Users.alias("targetUser")
                            val targetMindustryUserAlias = MindustryUser.alias("targetMindustryUser")

                            UserPunishments
                                .join(
                                    Users,
                                    JoinType.LEFT,
                                    onColumn = UserPunishments.userID,
                                    otherColumn = Users.id
                                )
                                .join(
                                    targetUserAlias,
                                    JoinType.LEFT,
                                    onColumn = UserPunishments.targetUserID,
                                    otherColumn = targetUserAlias[Users.id]
                                )
                                .join(
                                    MindustryUser,
                                    JoinType.LEFT,
                                    onColumn = UserPunishments.mindustryUserID,
                                    otherColumn = MindustryUser.id
                                )
                                .join(
                                    targetMindustryUserAlias,
                                    JoinType.LEFT,
                                    onColumn = UserPunishments.targetMindustryUserID,
                                    otherColumn = targetMindustryUserAlias[MindustryUser.id]
                                )
                                .selectOne {
                                    UserPunishments.id eq data.userPunishmentID
                                }
                        }

                        if (userPunishment == null) {
                            Logger.error("Missing UserPunishments entry with the id ${data.userPunishmentID}.")

                            return@launch
                        }

                        val embed = EmbedBuilder().run {
                            setTitle("Punishment")

                            setColor(DiscordConstant.PUNISHMENT_EMBED_COLOR)

                            addField(
                                MessageEmbed.Field(
                                    "ID",
                                    userPunishment[UserPunishments.id].value.toString(),
                                    false
                                )
                            )

                            addField(
                                MessageEmbed.Field(
                                    "Name",
                                    if (userPunishment[UserPunishments.userID] != null)
                                        "`${data.name}`/`${userPunishment[UserPunishments.userID]}`"
                                    else
                                        data.name,
                                    true
                                )
                            )

                            addField(
                                MessageEmbed.Field(
                                    "Target",
                                    if (userPunishment[UserPunishments.targetUserID] != null)
                                        "`${data.name}`/`${userPunishment[UserPunishments.targetUserID]}`"
                                    else
                                        data.targetPlayerMindustryName, true
                                )
                            )

                            addField(
                                MessageEmbed.Field(
                                    "Type",
                                    userPunishment[UserPunishments.type].displayName,
                                    false
                                )
                            )

                            addField(
                                MessageEmbed.Field(
                                    "Duration",
                                    if (userPunishment[UserPunishments.endAt] == null) "Null" else
                                        userPunishment[UserPunishments.endAt]!!
                                            .toInstant(TimeZone.UTC)
                                            .minus(
                                                userPunishment[UserPunishments.punishedAt].toInstant(TimeZone.UTC)
                                            )
                                            .toDisplayString(),
                                    true
                                )
                            )

                            addField(
                                MessageEmbed.Field(
                                    "Server",
                                    userPunishment[UserPunishments.server].displayName,
                                    true
                                )
                            )

                            addField(MessageEmbed.Field("Reason", userPunishment[UserPunishments.reason], false))

                            build()
                        }

                        notificationChannel.sendMessageEmbeds(embed).addActionRow(
                            Button.primary("$PARDON_BUTTON_COMPONENT_ID_PREFIX${data.userPunishmentID}", "Pardon")
                        ).queue()
                    }

                    null
                }

                is PlayerRoleChangedGameEvent -> {
                    val data = it.data as PlayerRoleChangedGameEvent

                    CoroutineScopes.Main.launch {
                        val user = Database.newTransaction {
                            Users.selectOne { Users.id eq data.userID }!!
                        }

                        val embed = EmbedBuilder().run {
                            setTitle("User's Role Changed")

                            setColor(DiscordConstant.ROLE_CHANGES_EMBED_COLOR)

                            addField(
                                MessageEmbed.Field(
                                    "User",
                                    user[Users.username],
                                    true
                                )
                            )

                            addField(
                                MessageEmbed.Field(
                                    "Role",
                                    user[Users.role].displayName,
                                    true
                                )
                            )

                            build()
                        }

                        roleChangesChannel.sendMessageEmbeds(embed).queue()
                    }

                    null
                }
            }

            if (message != null)
                channel.sendMessage(message).queue()
        }
    }


    override fun onButtonInteraction(event: ButtonInteractionEvent) {
        if (event.componentId.startsWith(PARDON_BUTTON_COMPONENT_ID_PREFIX)) {
            CoroutineScopes.IO.launch {
                val user = Database.newTransaction {
                    Users.selectOne { Users.discordID eq event.user.id }
                }

                if (user == null) {
                    return@launch event.reply("Your must verify your discord account before using this.")
                        .setEphemeral(true)
                        .queue()
                }

                val userPunishmentID = event.componentId
                    .drop(PARDON_BUTTON_COMPONENT_ID_PREFIX.length)

                val pardonReason: TextInput = TextInput.create("pardonReason", "Pardon reason", TextInputStyle.SHORT)
                    .setPlaceholder("Pardon reason.")
                    .setMinLength(0)
                    .build()


                val modal = Modal.create("$PARDON_MODAL_ID_PREFIX$userPunishmentID", "Pardon")
                    .addComponents(ActionRow.of(pardonReason))
                    .build()

                event.replyModal(modal).queue()
            }
        }
    }

    override fun onModalInteraction(event: ModalInteractionEvent) {
        if (event.modalId.startsWith(PARDON_MODAL_ID_PREFIX)) {
            event.deferReply(true).queue()

            val userPunishmentID = event.modalId
                .drop(PARDON_MODAL_ID_PREFIX.length)
                .toInt()

            val pardonReason = event.getValue("pardonReason")!!.asString

            CoroutineScopes.IO.launch {
                Database.newTransaction {
                    UserPunishments.update({ UserPunishments.id eq userPunishmentID }) {
                        it[this.pardonReason] = pardonReason
                        it[this.pardonedAt] = Clock.System.now().toLocalDateTime(TimeZone.UTC)
                    }

                    val userPunishment = UserPunishments.selectOne { UserPunishments.id eq userPunishmentID }

                    if (userPunishment == null) {
                        Logger.error("Missing UserPunishments entry with the id $userPunishmentID.")

                        return@newTransaction
                    }

                    val embed = EmbedBuilder().run {
                        setTitle("Pardon")

                        setColor(DiscordConstant.PARDON_EMBED_COLOR)

                        addField(
                            MessageEmbed.Field(
                                "ID",
                                userPunishment[UserPunishments.id].value.toString(),
                                true
                            )
                        )

                        addField(
                            MessageEmbed.Field(
                                "Type",
                                userPunishment[UserPunishments.type].displayName,
                                false
                            )
                        )

                        addField(
                            MessageEmbed.Field(
                                "Target User ID",
                                (userPunishment[UserPunishments.targetUserID]?.value ?: "Null").toString(),
                                false
                            )
                        )

                        addField(
                            MessageEmbed.Field(
                                "Target Mindustry User ID",
                                (userPunishment[UserPunishments.mindustryUserID]?.value ?: "Null").toString(),
                                false
                            )
                        )

                        addField(MessageEmbed.Field("Reason", userPunishment[UserPunishments.reason], false))
                        addField(
                            MessageEmbed.Field(
                                "Pardon Reason",
                                userPunishment[UserPunishments.pardonReason],
                                false
                            )
                        )

                        build()
                    }

                    notificationChannel.sendMessageEmbeds(embed).queue()

                    event.hook.sendMessage("Pardoned").queue()
                }
            }
        }
    }
}
