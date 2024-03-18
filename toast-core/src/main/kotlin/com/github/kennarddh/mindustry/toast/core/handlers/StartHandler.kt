package com.github.kennarddh.mindustry.toast.core.handlers

import arc.Core
import arc.util.Reflect
import arc.util.Timer
import com.github.kennarddh.mindustry.genesis.core.commons.CoroutineScopes
import com.github.kennarddh.mindustry.genesis.core.commons.runOnMindustryThread
import com.github.kennarddh.mindustry.genesis.core.events.annotations.EventHandler
import com.github.kennarddh.mindustry.genesis.core.handlers.Handler
import com.github.kennarddh.mindustry.toast.common.messaging.Messenger
import com.github.kennarddh.mindustry.toast.common.messaging.messages.GameEvent
import com.github.kennarddh.mindustry.toast.common.messaging.messages.ServerStartGameEvent
import com.github.kennarddh.mindustry.toast.core.commons.Logger
import com.github.kennarddh.mindustry.toast.core.commons.ToastVars
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import mindustry.Vars
import mindustry.game.EventType
import mindustry.io.SaveIO
import mindustry.net.Administration.Config
import mindustry.server.ServerControl
import kotlin.time.Duration.Companion.seconds

class StartHandler : Handler {
    @EventHandler
    suspend fun onLoad(event: EventType.ServerLoadEvent) {
        Config.port.set(ToastVars.port)

        Logger.info("Port set to ${ToastVars.port}")

        Logger.info("Server load... Will host in 1 second.")

        runBlocking {
            delay(1.seconds)

            Logger.info("Applying configs.")

            ToastVars.applyConfigs()
            ToastVars.server.gameMode.applyConfigs()
            ToastVars.server.applyConfigs()

            Logger.info("Configs applied.")

            Logger.info("Hosting")

            host()

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

    private fun host() {
        runOnMindustryThread {
            // TODO: When v147 released replace this with ServerControl.instance.cancelPlayTask()
            Reflect.get<Timer.Task>(ServerControl.instance, "lastTask")?.cancel()

            Vars.logic.reset()

            if (AutoSaveHandler.file.exists()) {
                Logger.info("Found auto save. Using it.")

                SaveIO.load(AutoSaveHandler.file)
            } else {
                Logger.info("No auto save found. Using random maps.")
                
                val map = Vars.maps.shuffleMode.next(ToastVars.server.gameMode.mindustryGameMode, Vars.state.map)

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

            Logger.info("Hosted")
        }
    }
}