package com.github.kennarddh.mindustry.toast.discord

import com.github.kennarddh.mindustry.toast.common.database.DatabaseSettings
import com.github.kennarddh.mindustry.toast.common.database.tables.MindustryUser
import com.github.kennarddh.mindustry.toast.common.database.tables.UserPunishments
import com.github.kennarddh.mindustry.toast.common.database.tables.Users
import com.github.kennarddh.mindustry.toast.common.messaging.Messenger
import com.github.kennarddh.mindustry.toast.common.messaging.messages.*
import com.github.kennarddh.mindustry.toast.common.selectOne
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.events.session.ReadyEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.requests.GatewayIntent
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.alias
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction


lateinit var jda: JDA

class ReadyListener : ListenerAdapter() {
    private lateinit var toastMindustryGuild: Guild
    private lateinit var notificationChannel: TextChannel
    private lateinit var reportsChannel: TextChannel
    private lateinit var serverListChannel: TextChannel

    override fun onReady(event: ReadyEvent) {
        Logger.info("Bot Ready")

        toastMindustryGuild = jda.getGuildById(DiscordConstant.TOAST_MINDUSTRY_GUILD_ID)!!
        notificationChannel = toastMindustryGuild.getTextChannelById(DiscordConstant.NOTIFICATIONS_CHANNEL_ID)!!
        reportsChannel = toastMindustryGuild.getTextChannelById(DiscordConstant.REPORTS_CHANNEL_ID)!!
        serverListChannel = toastMindustryGuild.getTextChannelById(DiscordConstant.SERVER_LIST_CHANNEL_ID)!!

        Messenger.listenGameEvent("DiscordBot") {
            val channel = toastMindustryGuild.getTextChannelById(it.server.discordChannelID)!!

            val message = when (it.data) {
                is PlayerJoinGameEvent -> "${(it.data as PlayerJoinGameEvent).playerMindustryName} joined."
                is PlayerLeaveGameEvent -> "${(it.data as PlayerLeaveGameEvent).playerMindustryName} left."
                is PlayerChatGameEvent -> "[${(it.data as PlayerChatGameEvent).playerMindustryName}]: ${(it.data as PlayerChatGameEvent).message}"
                is ServerStartGameEvent -> "Server start."
                is ServerStopGameEvent -> "Server stop."
                is ServerRestartGameEvent -> "Server restart."
                is PlayerPunishedGameEvent -> {
                    val data = it.data as PlayerPunishedGameEvent

                    CoroutineScopes.Main.launch {
                        val userPunishment = newSuspendedTransaction(CoroutineScopes.IO.coroutineContext) {
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

                            addField(MessageEmbed.Field("Name", "${data.name}/", false))
                            addField(MessageEmbed.Field("Target", data.targetPlayerMindustryName, false))

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
                                            .toString(),
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
            }

            if (message != null)
                channel.sendMessage(message).queue()
        }
    }
}

suspend fun main() {
    Logger.info("Loaded")

    Messenger.init()

    DatabaseSettings.init(CoroutineScopes.IO.coroutineContext)

    addShutdownHook()

    jda = JDABuilder.createDefault(System.getenv("BOT_TOKEN"))
        .setActivity(Activity.playing("Toast Mindustry Server"))
        .addEventListeners(ReadyListener())
        .enableIntents(
            GatewayIntent.GUILD_MESSAGES,
            GatewayIntent.GUILD_EMOJIS_AND_STICKERS,
            GatewayIntent.GUILD_MEMBERS,
            GatewayIntent.GUILD_PRESENCES,
            GatewayIntent.GUILD_MESSAGE_REACTIONS
        )
        .build()
}
