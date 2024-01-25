package com.github.kennarddh.mindustry.toast.discord

import com.github.kennarddh.mindustry.toast.common.messaging.Messenger

fun addShutdownHook() {
    Runtime.getRuntime().addShutdownHook(Thread {
        println("Gracefully shutting down")

        Messenger.close()

        jda.shutdown()

        println("Stopped")
    })
}