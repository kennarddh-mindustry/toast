package com.github.kennarddh.mindustry.toast.core.handlers

import arc.Core
import arc.Events
import arc.util.Reflect
import arc.util.Timer
import com.github.kennarddh.mindustry.genesis.core.Genesis
import com.github.kennarddh.mindustry.genesis.core.commands.annotations.Command
import com.github.kennarddh.mindustry.genesis.core.commands.annotations.Description
import com.github.kennarddh.mindustry.genesis.core.commands.senders.ServerCommandSender
import com.github.kennarddh.mindustry.genesis.core.commons.CoroutineScopes
import com.github.kennarddh.mindustry.genesis.core.handlers.Handler
import com.github.kennarddh.mindustry.toast.common.messaging.Messenger
import com.github.kennarddh.mindustry.toast.common.messaging.messages.GameEvent
import com.github.kennarddh.mindustry.toast.common.messaging.messages.ServerStartGameEvent
import com.github.kennarddh.mindustry.toast.core.commons.Logger
import com.github.kennarddh.mindustry.toast.core.commons.ToastState
import com.github.kennarddh.mindustry.toast.core.commons.ToastVars
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlinx.datetime.Clock
import mindustry.Vars
import mindustry.core.GameState
import mindustry.game.EventType
import mindustry.io.SaveIO
import mindustry.net.Administration.Config
import mindustry.server.ServerControl
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class StartHandler : Handler {
    override suspend fun onInit() {
        Genesis.commandRegistry.removeCommand("host")
        Genesis.commandRegistry.removeCommand("load")

        // @EventHandler Annotation doesn't always work when the EventType is ServerLoadEvent
        Events.on(EventType.ServerLoadEvent::class.java) { _ ->
            Config.port.set(ToastVars.port)

            Logger.info("Port set to ${ToastVars.port}")

            runBlocking {
                Logger.info("Applying configs.")

                ToastVars.applyConfigs()
                ToastVars.server.gameMode.applyConfigs()
                ToastVars.server.applyConfigs()

                Logger.info("Configs applied.")

                Logger.info("Hosting")

                val successHost = tryHost()

                if (!successHost) {
                    Logger.error("Failed to host")

                    return@runBlocking
                }

                CoroutineScopes.Main.launch {
                    Logger.info("ServerStartGameEvent publishing")

                    Messenger.publishGameEvent(
                        "${ToastVars.server.name}.start",
                        GameEvent(
                            ToastVars.server, Clock.System.now(),
                            ServerStartGameEvent()
                        )
                    )

                    Logger.info("ServerStartGameEvent published")
                }
            }
        }
    }

    private suspend fun tryLoadAutoSave(timeout: Duration = 20.seconds): Boolean {
        try {
            return withTimeout(timeout) {
                try {
                    if (!AutoSaveHandler.file.exists()) return@withTimeout false

                    Logger.info("Found auto save. Using it.")

                    if (!SaveIO.isSaveValid(AutoSaveHandler.file)) {
                        Logger.warn("Invalid auto save file. Deleting.")

                        AutoSaveHandler.file.delete()

                        return@withTimeout false
                    }

                    SaveIO.load(AutoSaveHandler.file)

                    Vars.state.rules.sector = null

                    Vars.state.set(GameState.State.playing)

                    Logger.info("Auto save loaded.")

                    return@withTimeout true
                } catch (error: Exception) {
                    Logger.error("Failed to load auto save msav file.", error)

                    return@withTimeout false
                }
            }
        } catch (error: TimeoutCancellationException) {
            Logger.info("Timeout on loading auto save. Ignoring auto save.")

            return false
        }
    }

    private suspend fun tryHost(): Boolean {
        try {
            return withTimeout(30.seconds) {
                // TODO: When v147 released replace this with ServerControl.instance.cancelPlayTask()
                Reflect.get<Timer.Task>(ServerControl.instance, "lastTask")?.cancel()

                Vars.logic.reset()

                val successLoadAutoSave = tryLoadAutoSave()

                if (!successLoadAutoSave) {
                    // Just to be safe I guess
                    // TODO: When v147 released replace this with ServerControl.instance.cancelPlayTask()
                    Reflect.get<Timer.Task>(ServerControl.instance, "lastTask")?.cancel()

                    Vars.logic.reset()

                    Logger.info("No auto save found. Using random maps.")

                    val map =
                        Vars.maps.shuffleMode.next(ToastVars.server.gameMode.mindustryGameMode, Vars.state.map)

                    ServerControl.instance.lastMode = ToastVars.server.gameMode.mindustryGameMode

                    Core.settings.put("lastServerMode", ServerControl.instance.lastMode.name)
                    Vars.world.loadMap(map, map.applyRules(ServerControl.instance.lastMode))

                    Vars.state.rules = map.applyRules(ToastVars.server.gameMode.mindustryGameMode)
                }

                ToastVars.applyRules(Vars.state.rules)
                ToastVars.server.gameMode.applyRules(Vars.state.rules)
                ToastVars.server.applyRules(Vars.state.rules)

                Vars.logic.play()

                Vars.netServer.openServer()

                ToastVars.stateLock.withLock {
                    ToastVars.state = ToastState.Hosting
                }

                Logger.info("Hosted")

                true
            }
        } catch (error: TimeoutCancellationException) {
            Logger.error("Timeout on starting. Exiting.")

            Vars.net.dispose()
            Core.app.exit()

            return false
        }
    }

    @Command(["host"])
    @Description("Start hosting.")
    private suspend fun hostCommand(sender: ServerCommandSender) {
        if (Vars.state.isGame)
            return sender.sendError("Already hosting.")

        tryHost()
    }
}