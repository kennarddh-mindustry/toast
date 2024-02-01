package com.github.kennarddh.mindustry.toast.discord

import com.github.kennarddh.mindustry.toast.common.CoroutineScopes
import com.github.kennarddh.mindustry.toast.common.database.DatabaseSettings
import com.github.kennarddh.mindustry.toast.common.database.tables.UserBan
import com.github.kennarddh.mindustry.toast.common.database.tables.Users
import com.github.kennarddh.mindustry.toast.common.messaging.Messenger
import com.github.kennarddh.mindustry.toast.common.messaging.messages.*
import com.github.kennarddh.mindustry.toast.common.selectOne
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.events.session.ReadyEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.requests.GatewayIntent
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.alias
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

lateinit var jda: JDA

const val TOAST_MINDUSTRY_GUILD_ID = 1189553927843237888L

class ReadyListener : ListenerAdapter() {
    private lateinit var toastMindustryGuild: Guild

    override fun onReady(event: ReadyEvent) {
        Logger.info("Bot Ready")

        toastMindustryGuild = jda.getGuildById(TOAST_MINDUSTRY_GUILD_ID)!!

        Messenger.listenGameEvent("DiscordBot") {
            val channel = toastMindustryGuild.getTextChannelById(it.server.discordChannelID)!!

            val message = when (it.data) {
                is PlayerJoinGameEvent -> "${(it.data as PlayerJoinGameEvent).playerMindustryName} joined."
                is PlayerLeaveGameEvent -> "${(it.data as PlayerLeaveGameEvent).playerMindustryName} left."
                is PlayerChatGameEvent -> "[${(it.data as PlayerChatGameEvent).playerMindustryName}]: ${(it.data as PlayerChatGameEvent).message}"
                is ServerStartGameEvent -> "Server start."
                is ServerStopGameEvent -> "Server stop."
                is ServerRestartGameEvent -> "Server restart."
                is PlayerBannedGameEvent -> {
                    // TODO: Make reports channel for ban/kick/voteKick stuff
                    val data = it.data as PlayerBannedGameEvent

                    CoroutineScopes.IO.launch {
                        newSuspendedTransaction {
                            val targetUserAlias = Users.alias("target")

                            val userBan = UserBan
                                .join(Users, JoinType.INNER, onColumn = UserBan.userID, otherColumn = Users.id)
                                .join(
                                    targetUserAlias,
                                    JoinType.LEFT,
                                    onColumn = UserBan.targetUserID,
                                    otherColumn = targetUserAlias[Users.id]
                                )
                                .selectOne { UserBan.id eq data.userBanID }!!

                            val message = if (userBan.hasValue(targetUserAlias[Users.id]))
                                "`${data.targetPlayerMindustryName}` was banned by `${userBan[Users.username]}/${userBan[Users.id]}`"
                            else
                                "`${data.targetPlayerMindustryName}/${userBan[targetUserAlias[Users.username]]}/${userBan[targetUserAlias[Users.id]]}` was banned by `${userBan[Users.username]}/${userBan[Users.id]}`"

                            channel.sendMessage(message).queue()
                        }
                    }
                    null
                }

                is PlayerKickedGameEvent -> TODO()
                is PlayerVoteKickCancelGameEvent -> TODO()
                is PlayerVoteKickStartGameEvent -> TODO()
                is PlayerVoteKickVoteGameEvent -> TODO()
            }

            if (message != null)
                channel.sendMessage(message).queue()
        }
    }
}

suspend fun main() {
    Logger.info("Loaded")

    Messenger.init()

    DatabaseSettings.init()

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
