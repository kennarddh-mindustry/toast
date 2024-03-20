package com.github.kennarddh.mindustry.toast.core.handlers

import com.github.kennarddh.mindustry.genesis.core.commons.runOnMindustryThreadSuspended
import com.github.kennarddh.mindustry.genesis.core.events.annotations.EventHandler
import com.github.kennarddh.mindustry.genesis.core.handlers.Handler
import com.github.kennarddh.mindustry.toast.core.commons.Logger
import com.github.kennarddh.mindustry.toast.core.commons.ToastVars
import mindustry.Vars
import mindustry.game.EventType
import mindustry.gen.Call

class SettingsHandler : Handler {
    @EventHandler
    suspend fun onPlay(event: EventType.PlayEvent) {
        runOnMindustryThreadSuspended {
            Logger.info("New map. Applying rules.")

            ToastVars.applyRules(Vars.state.rules)
            ToastVars.server.gameMode.applyRules(Vars.state.rules)
            ToastVars.server.applyRules(Vars.state.rules)

            Logger.info("New map. Rules applied. Sending rules.")

            Call.setRules(Vars.state.rules)

            Logger.info("New map. Rules applied. Rules sent.")
        }
    }
}