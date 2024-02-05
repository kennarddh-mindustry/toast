package com.github.kennarddh.mindustry.toast.core

import com.github.kennarddh.mindustry.genesis.core.Genesis
import com.github.kennarddh.mindustry.genesis.core.commons.AbstractPlugin
import com.github.kennarddh.mindustry.toast.common.CoroutineScopes
import com.github.kennarddh.mindustry.toast.common.database.DatabaseSettings
import com.github.kennarddh.mindustry.toast.common.messaging.Messenger
import com.github.kennarddh.mindustry.toast.common.messaging.messages.GameEvent
import com.github.kennarddh.mindustry.toast.common.messaging.messages.ServerStartGameEvent
import com.github.kennarddh.mindustry.toast.core.commands.validations.MinimumRole
import com.github.kennarddh.mindustry.toast.core.commands.validations.validateMinimumRole
import com.github.kennarddh.mindustry.toast.core.commons.Logger
import com.github.kennarddh.mindustry.toast.core.commons.ToastVars
import com.github.kennarddh.mindustry.toast.core.handlers.GameEventsHandler
import com.github.kennarddh.mindustry.toast.core.handlers.UserAccountHandler
import com.github.kennarddh.mindustry.toast.core.handlers.UserModerationHandler
import com.github.kennarddh.mindustry.toast.core.handlers.UserStatsHandler
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.transactions.TransactionManager
import java.time.Instant

@Suppress("unused")
class Toast : AbstractPlugin() {
    override fun init() {
        CoroutineScopes.Main.launch {
            DatabaseSettings.init()
            Messenger.init()

            Genesis.commandRegistry.registerCommandValidationAnnotation(MinimumRole::class, ::validateMinimumRole)

            Genesis.registerHandler(UserAccountHandler())
            Genesis.registerHandler(UserStatsHandler())
            Genesis.registerHandler(GameEventsHandler())
            Genesis.registerHandler(UserModerationHandler())

            Messenger.publishGameEvent(
                GameEvent(
                    ToastVars.server, Instant.now().toEpochMilli(),
                    ServerStartGameEvent()
                )
            )

            Logger.info("Loaded")
        }
    }

    override fun dispose() {
        Messenger.close()

        TransactionManager.closeAndUnregister(DatabaseSettings.database)
    }
}