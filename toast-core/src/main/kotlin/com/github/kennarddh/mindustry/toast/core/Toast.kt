package com.github.kennarddh.mindustry.toast.core

import com.github.kennarddh.mindustry.genesis.core.GenesisAPI
import com.github.kennarddh.mindustry.genesis.core.commons.AbstractPlugin
import com.github.kennarddh.mindustry.toast.common.database.DatabaseSettings
import com.github.kennarddh.mindustry.toast.common.messaging.Messenger
import com.github.kennarddh.mindustry.toast.common.messaging.messages.GameEvent
import com.github.kennarddh.mindustry.toast.common.messaging.messages.ServerStartGameEvent
import com.github.kennarddh.mindustry.toast.core.commands.paramaters.types.ToastPlayerParameter
import com.github.kennarddh.mindustry.toast.core.commands.validations.MinimumRole
import com.github.kennarddh.mindustry.toast.core.commands.validations.validateMinimumRole
import com.github.kennarddh.mindustry.toast.core.commons.Logger
import com.github.kennarddh.mindustry.toast.core.commons.ToastVars
import com.github.kennarddh.mindustry.toast.core.handlers.GameEventsHandler
import com.github.kennarddh.mindustry.toast.core.handlers.users.UserAccountHandler
import com.github.kennarddh.mindustry.toast.core.handlers.users.UserModerationHandler
import com.github.kennarddh.mindustry.toast.core.handlers.users.UserStatsHandler
import kotlinx.datetime.Clock
import mindustry.gen.Player
import org.jetbrains.exposed.sql.transactions.TransactionManager

@Suppress("unused")
class Toast : AbstractPlugin() {
    override suspend fun onInit() {
        DatabaseSettings.init()
        Messenger.init()

        GenesisAPI.commandRegistry.registerCommandValidationAnnotation(MinimumRole::class, ::validateMinimumRole)
        GenesisAPI.commandRegistry.replaceParameterType(Player::class, ToastPlayerParameter())

        GenesisAPI.registerHandler(UserAccountHandler())
        GenesisAPI.registerHandler(UserStatsHandler())
        GenesisAPI.registerHandler(GameEventsHandler())
        GenesisAPI.registerHandler(UserModerationHandler())

        Messenger.publishGameEvent(
            GameEvent(
                ToastVars.server, Clock.System.now().toEpochMilliseconds(),
                ServerStartGameEvent()
            )
        )

        Logger.info("Loaded")
    }

    override suspend fun onDispose() {
        Messenger.close()

        TransactionManager.closeAndUnregister(DatabaseSettings.database)
    }
}