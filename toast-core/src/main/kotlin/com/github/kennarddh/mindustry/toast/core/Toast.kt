package com.github.kennarddh.mindustry.toast.core

import com.github.kennarddh.mindustry.genesis.core.GenesisAPI
import com.github.kennarddh.mindustry.genesis.core.commons.AbstractPlugin
import com.github.kennarddh.mindustry.genesis.core.commons.CoroutineScopes
import com.github.kennarddh.mindustry.toast.common.database.DatabaseSettings
import com.github.kennarddh.mindustry.toast.common.messaging.Messenger
import com.github.kennarddh.mindustry.toast.core.commands.paramaters.types.ToastPlayerParameter
import com.github.kennarddh.mindustry.toast.core.commands.validations.MinimumRole
import com.github.kennarddh.mindustry.toast.core.commands.validations.validateMinimumRole
import com.github.kennarddh.mindustry.toast.core.commons.Logger
import com.github.kennarddh.mindustry.toast.core.handlers.*
import com.github.kennarddh.mindustry.toast.core.handlers.users.*
import mindustry.gen.Player
import org.jetbrains.exposed.sql.transactions.TransactionManager

@Suppress("unused")
class Toast : AbstractPlugin() {
    override suspend fun onInit() {
        DatabaseSettings.init(CoroutineScopes.IO.coroutineContext)
        Messenger.init()

        GenesisAPI.commandRegistry.registerCommandValidationAnnotation(MinimumRole::class, ::validateMinimumRole)
        GenesisAPI.commandRegistry.replaceParameterType(Player::class, ToastPlayerParameter())

        GenesisAPI.registerHandler(UserAccountHandler())
        GenesisAPI.registerHandler(UserJoinsHandler())
        GenesisAPI.registerHandler(UserStatsHandler())
        GenesisAPI.registerHandler(UserModerationHandler())
        GenesisAPI.registerHandler(UserModerationSyncHandler())
        GenesisAPI.registerHandler(UserReportHandler())

        GenesisAPI.registerHandler(GameEventsHandler())

        GenesisAPI.registerHandler(SettingsHandler())
        GenesisAPI.registerHandler(DiscordHandler())
        GenesisAPI.registerHandler(StartHandler())

        GenesisAPI.registerHandler(VoteKickCommandHandler())

        GenesisAPI.registerHandler(ServerPresenceHandler())
        GenesisAPI.registerHandler(WelcomeHandler())

        GenesisAPI.registerHandler(ServerControlHandler())

        Logger.info("Loaded")
    }

    override suspend fun onDispose() {
        Messenger.close()

        TransactionManager.closeAndUnregister(DatabaseSettings.database)
    }
}