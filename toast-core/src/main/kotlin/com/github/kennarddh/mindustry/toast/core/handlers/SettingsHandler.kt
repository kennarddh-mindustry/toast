package com.github.kennarddh.mindustry.toast.core.handlers

import com.github.kennarddh.mindustry.genesis.core.events.annotations.EventHandler
import com.github.kennarddh.mindustry.genesis.core.handlers.Handler
import com.github.kennarddh.mindustry.toast.core.commons.Logger
import com.github.kennarddh.mindustry.toast.core.commons.ToastVars
import mindustry.Vars
import mindustry.game.EventType

class SettingsHandler : Handler() {
    override suspend fun onInit() {
        Logger.info("Applying configs.")
        ToastVars.server.gameMode.applyConfigs()
        ToastVars.server.applyConfigs()
        Logger.info("Configs applied.")
    }

    @EventHandler
    fun onPlay(event: EventType.PlayEvent) {
        Logger.info("New map. Applying rules.")

        ToastVars.server.gameMode.applyRules(Vars.state.rules)
        ToastVars.server.applyRules(Vars.state.rules)

        Logger.info("New map. Rules applied.")
    }
}