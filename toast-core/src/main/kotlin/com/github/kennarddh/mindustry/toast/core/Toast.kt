package com.github.kennarddh.mindustry.toast.core

import com.github.kennarddh.mindustry.genesis.core.GenesisAPI
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
import com.github.kennarddh.mindustry.toast.core.handlers.users.UserAccountHandler
import com.github.kennarddh.mindustry.toast.core.handlers.users.UserModerationHandler
import com.github.kennarddh.mindustry.toast.core.handlers.users.UserStatsHandler
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.transactions.TransactionManager
import java.time.Instant

@Suppress("unused")
class Toast : AbstractPlugin() {
    override fun init() {
        CoroutineScopes.Main.launch {
            DatabaseSettings.init()
            Messenger.init()

            GenesisAPI.commandRegistry.registerCommandValidationAnnotation(MinimumRole::class, ::validateMinimumRole)

            GenesisAPI.registerHandler(UserAccountHandler())
            GenesisAPI.registerHandler(UserStatsHandler())
            GenesisAPI.registerHandler(GameEventsHandler())
            GenesisAPI.registerHandler(UserModerationHandler())

            Messenger.publishGameEvent(
                GameEvent(
                    ToastVars.server, Instant.now().toEpochMilli(),
                    ServerStartGameEvent()
                )
            )

            Logger.info("Loaded")
        }
    }

    override suspend fun dispose() {
        Messenger.close()

        TransactionManager.closeAndUnregister(DatabaseSettings.database)
    }
}