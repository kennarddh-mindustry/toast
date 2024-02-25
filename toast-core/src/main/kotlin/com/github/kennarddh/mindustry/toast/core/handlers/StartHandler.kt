package com.github.kennarddh.mindustry.toast.core.handlers

import arc.Core
import arc.util.Reflect
import arc.util.Timer
import com.github.kennarddh.mindustry.genesis.core.commons.runOnMindustryThreadSuspended
import com.github.kennarddh.mindustry.genesis.core.events.annotations.EventHandler
import com.github.kennarddh.mindustry.genesis.core.handlers.Handler
import com.github.kennarddh.mindustry.toast.common.messaging.Messenger
import com.github.kennarddh.mindustry.toast.common.messaging.messages.GameEvent
import com.github.kennarddh.mindustry.toast.common.messaging.messages.ServerStartGameEvent
import com.github.kennarddh.mindustry.toast.core.commons.Logger
import com.github.kennarddh.mindustry.toast.core.commons.ToastVars
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import mindustry.Vars
import mindustry.game.EventType
import mindustry.net.Administration.Config
import mindustry.server.ServerControl
import kotlin.time.Duration.Companion.seconds

class StartHandler : Handler() {
    @EventHandler
    suspend fun onLoad(event: EventType.ServerLoadEvent) {
        Logger.info("ServerLoad... Will host in 1 second.")

        Config.port.set(ToastVars.port)

        delay(1.seconds)

        Logger.info("Hosting")

        host()

        Logger.info("Hosted")

        Messenger.publishGameEvent(
            "${ToastVars.server.name}.start",
            GameEvent(
                ToastVars.server, Clock.System.now(),
                ServerStartGameEvent()
            )
        )

        Logger.info("ServerStartGameEvent published")
    }

    private suspend fun host() {
        runOnMindustryThreadSuspended {
            // TODO: When v147 released replace this with ServerControl.instance.cancelPlayTask()
            Reflect.get<Timer.Task>(ServerControl.instance, "lastTask")?.cancel()

            val map = Vars.maps.shuffleMode.next(ToastVars.server.gameMode.mindustryGameMode, Vars.state.map)

            Vars.logic.reset()

            ServerControl.instance.lastMode = ToastVars.server.gameMode.mindustryGameMode

            Core.settings.put("lastServerMode", ServerControl.instance.lastMode.name)
            Vars.world.loadMap(map, map.applyRules(ServerControl.instance.lastMode))
            Vars.state.rules = map.applyRules(ToastVars.server.gameMode.mindustryGameMode)
            Vars.logic.play()

            Vars.netServer.openServer()
        }
    }
}