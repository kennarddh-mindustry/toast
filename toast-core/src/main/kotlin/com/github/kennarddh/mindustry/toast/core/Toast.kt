package com.github.kennarddh.mindustry.toast.core

import com.github.kennarddh.mindustry.genesis.core.GenesisAPI
import com.github.kennarddh.mindustry.genesis.core.commons.AbstractPlugin
import com.github.kennarddh.mindustry.genesis.core.commons.CoroutineScopes
import com.github.kennarddh.mindustry.toast.common.database.Database
import com.github.kennarddh.mindustry.toast.common.discovery.DiscoveryRedis
import com.github.kennarddh.mindustry.toast.common.messaging.Messenger
import com.github.kennarddh.mindustry.toast.common.messaging.messages.GameEvent
import com.github.kennarddh.mindustry.toast.common.messaging.messages.ServerStopGameEvent
import com.github.kennarddh.mindustry.toast.common.verify.discord.VerifyDiscordRedis
import com.github.kennarddh.mindustry.toast.core.commands.paramaters.types.ToastPlayerParameter
import com.github.kennarddh.mindustry.toast.core.commands.validations.*
import com.github.kennarddh.mindustry.toast.core.commons.Logger
import com.github.kennarddh.mindustry.toast.core.commons.ToastVars
import com.github.kennarddh.mindustry.toast.core.handlers.*
import com.github.kennarddh.mindustry.toast.core.handlers.users.*
import com.github.kennarddh.mindustry.toast.core.handlers.vote.kick.VoteKickCommandHandler
import kotlinx.datetime.Clock
import mindustry.gen.Player
import org.jetbrains.exposed.sql.transactions.TransactionManager

@Suppress("unused")
class Toast : AbstractPlugin() {
    override suspend fun onInit() {
        Logger.info("Connecting to external services")

        Database.init(CoroutineScopes.IO.coroutineContext, Logger)
        Messenger.init(CoroutineScopes.IO.coroutineContext, Logger)
        DiscoveryRedis.init()
        VerifyDiscordRedis.init()

        Logger.info("Registering command and parameter")

        GenesisAPI.commandRegistry.registerCommandValidationAnnotation(MinimumRole::class, ::validateMinimumRole)
        GenesisAPI.commandRegistry.registerCommandValidationAnnotation(LoggedIn::class, ::validateLoggedIn)
        GenesisAPI.commandRegistry.registerCommandValidationAnnotation(MinimumRank::class, ::validateMinimumRank)
        GenesisAPI.commandRegistry.replaceParameterType(Player::class, ToastPlayerParameter())

        Logger.info("Registering handlers")

        GenesisAPI.registerHandler(UserAccountHandler())
        GenesisAPI.registerHandler(UserJoinsHandler())
        GenesisAPI.registerHandler(UserStatsHandler())
        GenesisAPI.registerHandler(UserModerationHandler())
        GenesisAPI.registerHandler(UserModerationSyncHandler())
        GenesisAPI.registerHandler(UserReportHandler())
        GenesisAPI.registerHandler(UserDiscordVerify())

        GenesisAPI.registerHandler(GameEventsHandler())
        GenesisAPI.registerHandler(DiscoveryHandler())

        GenesisAPI.registerHandler(SettingsHandler())
        GenesisAPI.registerHandler(DiscordHandler())
        GenesisAPI.registerHandler(StartHandler())

        GenesisAPI.registerHandler(VoteKickCommandHandler())

        GenesisAPI.registerHandler(ServerPresenceHandler())
        GenesisAPI.registerHandler(WelcomeHandler())

        GenesisAPI.registerHandler(ServerControlHandler())

        GenesisAPI.registerHandler(MessageHandler())

        Logger.info("Loaded")
    }

    override suspend fun onDispose() {
        Messenger.publishGameEvent(
            "${ToastVars.server.name}.stop",
            GameEvent(
                ToastVars.server, Clock.System.now(),
                ServerStopGameEvent()
            )
        )

        DiscoveryRedis.close()
        VerifyDiscordRedis.close()

        Messenger.close()

        TransactionManager.closeAndUnregister(Database.database)
    }
}