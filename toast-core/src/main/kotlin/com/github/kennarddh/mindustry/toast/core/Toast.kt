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
import com.github.kennarddh.mindustry.toast.core.commands.paramaters.types.ToastPlayerParameter
import com.github.kennarddh.mindustry.toast.core.commands.validations.*
import com.github.kennarddh.mindustry.toast.core.commons.Logger
import com.github.kennarddh.mindustry.toast.core.commons.ToastState
import com.github.kennarddh.mindustry.toast.core.commons.ToastVars
import com.github.kennarddh.mindustry.toast.core.handlers.*
import com.github.kennarddh.mindustry.toast.core.handlers.users.*
import com.github.kennarddh.mindustry.toast.core.handlers.vote.kick.VoteKickCommandHandler
import com.github.kennarddh.mindustry.toast.core.handlers.vote.rtv.RTVCommandHandler
import com.github.kennarddh.mindustry.toast.core.handlers.vote.skip_wave.SkipWaveCommandHandler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.withLock
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

        Genesis.commandRegistry.registerCommandValidationAnnotation(MinimumRole::class, ::validateMinimumRole)
        Genesis.commandRegistry.registerCommandValidationAnnotation(LoggedIn::class, ::validateLoggedIn)
        Genesis.commandRegistry.registerCommandValidationAnnotation(MinimumRank::class, ::validateMinimumRank)
        Genesis.commandRegistry.replaceParameterType(Player::class, ToastPlayerParameter())

        Logger.info("Registering handlers")

        Genesis.registerHandler(UserAccountHandler())
        Genesis.registerHandler(UserJoinsHandler())
        Genesis.registerHandler(UserLeavesHandler())
        Genesis.registerHandler(UserStatsHandler())
        Genesis.registerHandler(UserModerationHandler())
        Genesis.registerHandler(UserModerationSyncHandler())
        Genesis.registerHandler(UserRoleSyncHandler())
        Genesis.registerHandler(UserReportHandler())
        Genesis.registerHandler(UserDiscord())

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
        Genesis.registerHandler(MapUpdateHandler())
        Genesis.registerHandler(MessageHandler())
        Genesis.registerHandler(GameStatsCommands())
        Genesis.registerHandler(JavascriptHandler())
        Genesis.registerHandler(ShutdownHandler())
        Genesis.registerHandler(AutoSaveHandler())

        Logger.info("Registering shutdown hook")

        Runtime.getRuntime().addShutdownHook(object : Thread() {
            override fun run(): Unit = runBlocking {
                ToastVars.stateLock.withLock {
                    if (ToastVars.state == ToastState.Disposing) return@runBlocking
                }

                Logger.info("Gracefully shutting down via shutdown hook.")

                try {
                    Genesis.getHandler<ShutdownHandler>()?.shutdown()

                    onDispose()
                } catch (_: CancellationException) {
                    Logger.warn("Shutdown hook cancelled.")
                }
            }
        })

        Logger.info("Loaded")
    }

    override suspend fun onDispose() {
        Logger.info("Trying to dispose")

        ToastVars.stateLock.withLock {
            if (ToastVars.state == ToastState.Disposing) return

            ToastVars.state = ToastState.Disposing
        }

        Logger.info("Disposing")

        Messenger.publishGameEvent(
            "${ToastVars.server.name}.stop",
            GameEvent(
                ToastVars.server, Clock.System.now(),
                ServerStopGameEvent()
            )
        )

        DiscoveryRedis.delete(ToastVars.server)

        DiscoveryRedis.close()
        VerifyDiscordRedis.close()

        Messenger.close()

        TransactionManager.closeAndUnregister(Database.database)

        Logger.info("Disposed.")
    }
}