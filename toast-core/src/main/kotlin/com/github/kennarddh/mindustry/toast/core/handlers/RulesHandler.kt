package com.github.kennarddh.mindustry.toast.core.handlers

import com.github.kennarddh.mindustry.genesis.core.events.annotations.EventHandler
import com.github.kennarddh.mindustry.genesis.core.handlers.Handler
import com.github.kennarddh.mindustry.toast.core.commons.ToastVars
import mindustry.Vars
import mindustry.game.EventType

class RulesHandler : Handler() {
    @EventHandler
    fun onPlay(event: EventType.PlayEvent) {
        ToastVars.server.gameMode.applyRules(Vars.state.rules)
        ToastVars.server.applyRules(Vars.state.rules)
    }
}