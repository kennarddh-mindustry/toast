package com.github.kennarddh.mindustry.toast.discord

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