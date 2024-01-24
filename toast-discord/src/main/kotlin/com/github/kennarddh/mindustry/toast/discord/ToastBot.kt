package com.github.kennarddh.mindustry.toast.discord

import com.github.kennarddh.mindustry.toast.common.messaging.Messenger
import com.github.kennarddh.mindustry.toast.common.messaging.messages.PlayerJoinGameEvent
import com.github.kennarddh.mindustry.toast.common.messaging.messages.PlayerLeaveGameEvent
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.events.session.ReadyEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.requests.GatewayIntent

lateinit var jda: JDA

const val TOAST_MINDUSTRY_GUILD_ID = 1189553927843237888L

class ReadyListener : ListenerAdapter() {
    private lateinit var toastMindustryGuild: Guild

    override fun onReady(event: ReadyEvent) {
        println("[ToastDiscord] Bot Ready")

        toastMindustryGuild = jda.getGuildById(TOAST_MINDUSTRY_GUILD_ID)!!

        Messenger.listenGameEvent("DiscordBot") {
            val channel = toastMindustryGuild.getTextChannelById(it.server.discordChannelID)!!

            val message = when (it.data) {
                is PlayerJoinGameEvent -> "${(it.data as PlayerJoinGameEvent).playerMindustryName} joined."
                is PlayerLeaveGameEvent -> "${(it.data as PlayerLeaveGameEvent).playerMindustryName} left."
            }

            channel.sendMessage(message).queue()
        }
    }
}

fun main() {
    println("[ToastDiscord] Loaded")

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