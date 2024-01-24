package com.github.kennarddh.mindustry.toast.discord

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.events.session.ReadyEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

lateinit var jda: JDA

class ReadyListener : ListenerAdapter() {
    override fun onReady(event: ReadyEvent) {
        println("[ToastDiscord] Bot Ready")
    }
}

fun main() {
    println("[ToastDiscord] Loaded")

    jda = JDABuilder.createDefault(System.getenv("BOT_TOKEN"))
        .addEventListeners(ReadyListener())
        .build()
}