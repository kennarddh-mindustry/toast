package com.github.kennarddh.mindustry.toast.core

import com.github.kennarddh.mindustry.genesis.core.Genesis
import com.github.kennarddh.mindustry.genesis.core.commons.AbstractPlugin
import com.github.kennarddh.mindustry.genesis.core.commons.CoroutineScopes
import com.github.kennarddh.mindustry.toast.common.database.Database
import com.github.kennarddh.mindustry.toast.common.discovery.DiscoveryRedis
import com.github.kennarddh.mindustry.toast.common.messaging.Messenger
import com.github.kennarddh.mindustry.toast.common.messaging.messages.GameEvent
import com.github.kennarddh.mindustry.toast.common.messaging.messages.ServerStopGameEvent
import com.github.kennarddh.mindustry.toast.common.verify.discord.VerifyDiscordRedis
import com.github.kennarddh.mindustry.toast.core.commands.paramaters.types.TeamParameter
import com.github.kennarddh.mindustry.toast.core.commands.paramaters.types.ToastPlayerParameter
import com.github.kennarddh.mindustry.toast.core.commands.paramaters.types.UnitTypeParameter
import com.github.kennarddh.mindustry.toast.core.commands.validations.*
import com.github.kennarddh.mindustry.toast.core.commons.Logger
import com.github.kennarddh.mindustry.toast.core.commons.ToastVars
import com.github.kennarddh.mindustry.toast.core.handlers.*
import com.github.kennarddh.mindustry.toast.core.handlers.users.*
import com.github.kennarddh.mindustry.toast.core.handlers.vote.kick.VoteKickCommandHandler
import com.github.kennarddh.mindustry.toast.core.handlers.vote.rtv.RTVCommandHandler
import com.github.kennarddh.mindustry.toast.core.handlers.vote.skip_wave.SkipWaveCommandHandler
import kotlinx.datetime.Clock
import mindustry.game.Team
import mindustry.gen.Player
import mindustry.type.UnitType
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

        Genesis.commandRegistry.registerCommandValidationAnnotation(MinimumRole::class, ::validateMinimumRole)
        Genesis.commandRegistry.registerCommandValidationAnnotation(LoggedIn::class, ::validateLoggedIn)
        Genesis.commandRegistry.registerCommandValidationAnnotation(MinimumRank::class, ::validateMinimumRank)
        Genesis.commandRegistry.replaceParameterType(Player::class, ToastPlayerParameter())
        Genesis.commandRegistry.registerParameterType(UnitType::class, UnitTypeParameter())
        Genesis.commandRegistry.registerParameterType(Team::class, TeamParameter())

        Logger.info("Registering handlers")

        Genesis.registerHandler(UserAccountHandler())
        Genesis.registerHandler(UserJoinsHandler())
        Genesis.registerHandler(UserLeavesHandler())
        Genesis.registerHandler(UserStatsHandler())
        Genesis.registerHandler(UserModerationHandler())
        Genesis.registerHandler(UserModerationSyncHandler())
        Genesis.registerHandler(UserRoleSyncHandler())
        Genesis.registerHandler(UserReportHandler())
        Genesis.registerHandler(UserDiscordVerify())

        Genesis.registerHandler(GameEventsHandler())
        Genesis.registerHandler(DiscoveryHandler())

        Genesis.registerHandler(SettingsHandler())
        Genesis.registerHandler(DiscordHandler())
        Genesis.registerHandler(StartHandler())

        Genesis.registerHandler(VoteKickCommandHandler())
        Genesis.registerHandler(RTVCommandHandler())
        Genesis.registerHandler(SkipWaveCommandHandler())

        Genesis.registerHandler(ServerPresenceHandler())
        Genesis.registerHandler(WelcomeHandler())

        Genesis.registerHandler(ServerControlHandler())

        Genesis.registerHandler(MessageHandler())

        Genesis.registerHandler(GameStatsCommands())

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