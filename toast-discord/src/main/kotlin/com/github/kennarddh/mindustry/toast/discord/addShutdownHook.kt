package com.github.kennarddh.mindustry.toast.discord

import com.github.kennarddh.mindustry.toast.common.database.Database
import com.github.kennarddh.mindustry.toast.common.discovery.DiscoveryRedis
import com.github.kennarddh.mindustry.toast.common.messaging.Messenger
import com.github.kennarddh.mindustry.toast.common.verify.discord.VerifyDiscordRedis
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.transactions.TransactionManager

fun addShutdownHook() {
    Runtime.getRuntime().addShutdownHook(Thread {
        runBlocking {
            println("Gracefully shutting down")

            jda.shutdown()

            DiscoveryRedis.close()
            VerifyDiscordRedis.close()

            Messenger.close()

            TransactionManager.closeAndUnregister(Database.database)

            println("Stopped")
        }
    })
}