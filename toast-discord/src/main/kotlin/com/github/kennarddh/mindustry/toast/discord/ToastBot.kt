package com.github.kennarddh.mindustry.toast.discord

import com.github.kennarddh.mindustry.toast.common.database.Database
import com.github.kennarddh.mindustry.toast.common.discovery.DiscoveryRedis
import com.github.kennarddh.mindustry.toast.common.messaging.Messenger
import com.github.kennarddh.mindustry.toast.common.verify.discord.VerifyDiscordRedis
import com.github.kennarddh.mindustry.toast.discord.listeners.*
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.requests.GatewayIntent


lateinit var jda: JDA
lateinit var toastMindustryGuild: Guild

lateinit var notificationChannel: TextChannel
lateinit var reportsChannel: TextChannel
lateinit var serverListChannel: TextChannel
lateinit var roleChangesChannel: TextChannel

lateinit var memberRole: Role

suspend fun main() {
    Logger.info("Loaded")

    Database.init(CoroutineScopes.IO.coroutineContext, Logger)
    Messenger.init(CoroutineScopes.IO.coroutineContext, Logger)
    DiscoveryRedis.init()
    VerifyDiscordRedis.init()

    addShutdownHook()

    jda = JDABuilder.createDefault(System.getenv("BOT_TOKEN"))
        .setActivity(Activity.playing("Toast Mindustry Server"))
        .addEventListeners(ReadyListener)
        .addEventListeners(GameEventsListener)
        .addEventListeners(ServerControlListener)
        .addEventListeners(DiscoveryHandler)
        .addEventListeners(VerifyDiscordHandler)
        .addEventListeners(MemberJoinListener)
        .enableIntents(
            GatewayIntent.GUILD_MESSAGES,
            GatewayIntent.GUILD_EMOJIS_AND_STICKERS,
            GatewayIntent.GUILD_MEMBERS,
            GatewayIntent.GUILD_PRESENCES,
            GatewayIntent.GUILD_MESSAGE_REACTIONS
        )
        .build()
}
