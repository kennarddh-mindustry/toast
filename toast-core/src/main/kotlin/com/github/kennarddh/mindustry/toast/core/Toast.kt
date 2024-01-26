package com.github.kennarddh.mindustry.toast.core

import arc.ApplicationListener
import arc.Core
import com.github.kennarddh.mindustry.toast.common.CoroutineScopes
import com.github.kennarddh.mindustry.toast.common.database.DatabaseSettings
import com.github.kennarddh.mindustry.toast.common.messaging.Messenger
import com.github.kennarddh.mindustry.toast.common.messaging.messages.GameEvent
import com.github.kennarddh.mindustry.toast.common.messaging.messages.ServerStartGameEvent
import com.github.kennarddh.mindustry.toast.common.messaging.messages.ServerStopGameEvent
import com.github.kennarddh.mindustry.toast.core.commons.Logger
import com.github.kennarddh.mindustry.toast.core.commons.ToastVars
import com.github.kennarddh.mindustry.toast.core.handlers.GameEventsHandler
import com.github.kennarddh.mindustry.toast.core.handlers.UserAccountHandler
import com.github.kennarddh.mindustry.toast.core.handlers.UserStatsHandler
import kennarddh.genesis.core.Genesis
import kennarddh.genesis.core.commons.AbstractPlugin
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.transactions.TransactionManager
import java.time.Instant

@Suppress("unused")
class Toast : AbstractPlugin() {
    override fun init() {
        CoroutineScopes.Main.launch {
            DatabaseSettings.init()
            Messenger.init()

            Genesis.addHandler(UserAccountHandler())
            Genesis.addHandler(UserStatsHandler())
            Genesis.addHandler(GameEventsHandler())

            Messenger.publishGameEvent(
                GameEvent(
                    ToastVars.server, Instant.now().toEpochMilli(),
                    ServerStartGameEvent()
                )
            )

            Logger.info("Loaded")
        }

        Core.app.addListener(object : ApplicationListener {
            override fun dispose() {
                Logger.info("Gracefully shutting down")

                Messenger.publishGameEvent(
                    GameEvent(
                        ToastVars.server, Instant.now().toEpochMilli(),
                        ServerStopGameEvent()
                    )
                )

                Messenger.close()

                TransactionManager.closeAndUnregister(DatabaseSettings.database)

                Logger.info("Stopped")
            }
        })
    }
}