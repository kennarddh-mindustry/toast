package com.github.kennarddh.mindustry.toast.core

import arc.util.Log
import com.github.kennarddh.mindustry.toast.common.database.DatabaseSettings
import com.github.kennarddh.mindustry.toast.core.handlers.GameEventsHandler
import com.github.kennarddh.mindustry.toast.core.handlers.UserAccountHandler
import com.github.kennarddh.mindustry.toast.core.handlers.UserStatsHandler
import kennarddh.genesis.core.Genesis
import kennarddh.genesis.core.commons.AbstractPlugin

@Suppress("unused")
class Toast : AbstractPlugin() {
    override fun init() {
        DatabaseSettings.init()

        Genesis.addHandler(UserAccountHandler())
        Genesis.addHandler(UserStatsHandler())
        Genesis.addHandler(GameEventsHandler())

        Log.info("[ToastCore] Loaded")
    }
}