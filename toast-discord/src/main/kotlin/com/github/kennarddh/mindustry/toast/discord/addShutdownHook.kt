package com.github.kennarddh.mindustry.toast.discord

import com.github.kennarddh.mindustry.toast.common.database.DatabaseSettings
import com.github.kennarddh.mindustry.toast.common.messaging.Messenger
import org.jetbrains.exposed.sql.transactions.TransactionManager

fun addShutdownHook() {
    Runtime.getRuntime().addShutdownHook(Thread {
        println("Gracefully shutting down")

        Messenger.close()

        jda.shutdown()

        TransactionManager.closeAndUnregister(DatabaseSettings.database)

        println("Stopped")
    })
}