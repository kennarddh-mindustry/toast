package com.github.kennarddh.mindustry.toast.core.commons

import arc.util.Log
import com.github.kennarddh.mindustry.toast.common.messaging.Messenger
import com.github.kennarddh.mindustry.toast.common.messaging.messages.GameEvent
import com.github.kennarddh.mindustry.toast.common.messaging.messages.ServerStopGameEvent
import kotlinx.coroutines.runBlocking
import java.time.Instant

fun addShutdownHook() {
    Runtime.getRuntime().addShutdownHook(object : Thread() {
        override fun run() = runBlocking {
            Log.info("Gracefully shutting down")

            Messenger.publishGameEvent(
                GameEvent(
                    ToastVars.server, Instant.now().toEpochMilli(),
                    ServerStopGameEvent()
                )
            )

            Messenger.close()

            Log.info("Stopped")
        }
    })
}