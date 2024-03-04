package com.github.kennarddh.mindustry.toast.discord.listeners

import com.github.kennarddh.mindustry.toast.common.database.Database
import com.github.kennarddh.mindustry.toast.common.database.tables.MindustryUser
import com.github.kennarddh.mindustry.toast.common.database.tables.UserPunishments
import com.github.kennarddh.mindustry.toast.common.database.tables.Users
import com.github.kennarddh.mindustry.toast.common.messaging.Messenger
import com.github.kennarddh.mindustry.toast.common.messaging.messages.*
import com.github.kennarddh.mindustry.toast.common.selectOne
import com.github.kennarddh.mindustry.toast.common.toDisplayString
import com.github.kennarddh.mindustry.toast.discord.*
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.session.ReadyEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.alias


object GameEventsListener : ListenerAdapter() {
    override fun onReady(event: ReadyEvent) {
        Messenger.listenGameEvent("DiscordBotGameEvents", "#") {
            val channel = toastMindustryGuild.getTextChannelById(it.server.discordChannelID)!!

            val message = when (it.data) {
                is PlayerJoinGameEvent -> "${(it.data as PlayerJoinGameEvent).playerMindustryName} joined."
                is PlayerLeaveGameEvent -> "${(it.data as PlayerLeaveGameEvent).playerMindustryName} left."
                is PlayerChatGameEvent -> "[${(it.data as PlayerChatGameEvent).playerMindustryName}]: ${(it.data as PlayerChatGameEvent).message}"
                is ServerStartGameEvent -> "Server start."
                is ServerStopGameEvent -> "Server stop."
                is ServerRestartGameEvent -> "Server restart."
                is PlayerReportedGameEvent -> {
                    val data = it.data as PlayerReportedGameEvent

                    val embed = EmbedBuilder().run {
                        setTitle("Player Reported")

                        setColor(DiscordConstant.REPORTED_EMBED_COLOR)

                        addField(
                            MessageEmbed.Field(
                                "Reporter",
                                if (data.playerUserID != null)
                                    "`${data.playerMindustryName}`/`${data.playerUserID}`"
                                else
                                    data.playerMindustryName,
                                true
                            )
                        )

                        addField(
                            MessageEmbed.Field(
                                "Target",
                                if (data.targetUserID != null)
                                    "`${data.targetMindustryName}`/`${data.targetUserID}`"
                                else
                                    data.targetMindustryName, true
                            )
                        )

                        addField(MessageEmbed.Field("Server", it.server.displayName, false))

                        addField(MessageEmbed.Field("Reason", data.reason, false))

                        build()
                    }

                    reportsChannel.sendMessageEmbeds(embed).queue()

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
                                }!!
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

                        notificationChannel.sendMessageEmbeds(embed).queue()
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
}
