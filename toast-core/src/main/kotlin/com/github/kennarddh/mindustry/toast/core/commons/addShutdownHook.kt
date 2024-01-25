package com.github.kennarddh.mindustry.toast.core.commons

import arc.util.Log
import com.github.kennarddh.mindustry.toast.common.database.DatabaseSettings
import com.github.kennarddh.mindustry.toast.common.messaging.Messenger
import com.github.kennarddh.mindustry.toast.common.messaging.messages.GameEvent
import com.github.kennarddh.mindustry.toast.common.messaging.messages.ServerStopGameEvent
import org.jetbrains.exposed.sql.transactions.TransactionManager
import java.time.Instant

fun addShutdownHook() {
    Runtime.getRuntime().addShutdownHook(Thread {
        Log.info("Gracefully shutting down")

        Messenger.publishGameEvent(
            GameEvent(
                ToastVars.server, Instant.now().toEpochMilli(),
                ServerStopGameEvent()
            )
        )

        Messenger.close()

        TransactionManager.closeAndUnregister(DatabaseSettings.database)

        Log.info("Stopped")
    })
}