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
import com.github.kennarddh.mindustry.toast.core.commons.ToastVars
import kotlinx.datetime.Clock
import mindustry.Vars
import mindustry.game.EventType
import mindustry.server.ServerControl

class StartHandler : Handler() {
    @EventHandler
    suspend fun onLoad(event: EventType.ServerLoadEvent) {
        host()

        Messenger.publishGameEvent(
            GameEvent(
                ToastVars.server, Clock.System.now().toEpochMilliseconds(),
                ServerStartGameEvent()
            )
        )
    }

    private suspend fun host() {
        runOnMindustryThreadSuspended {
            // TODO: When v147 released replace this with ServerControl.instance.cancelPlayTask()
            Reflect.get<Timer.Task>(ServerControl.instance, "lastTask")?.cancel()
        }

        val map = Vars.maps.shuffleMode.next(ToastVars.server.gameMode.mindustryGameMode, Vars.state.map)

        runOnMindustryThreadSuspended {
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