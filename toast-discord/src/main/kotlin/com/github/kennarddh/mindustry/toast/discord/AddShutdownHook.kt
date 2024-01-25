package com.github.kennarddh.mindustry.toast.discord

import com.github.kennarddh.mindustry.toast.common.messaging.Messenger
import kotlinx.coroutines.runBlocking

fun AddShutdownHook() {
    Runtime.getRuntime().addShutdownHook(object : Thread() {
        override fun run() = runBlocking {
            println("Gracefully shutting down")

            Messenger.close()

            println("Stopped")
        }
    })
}