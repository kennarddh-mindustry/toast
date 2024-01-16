package com.github.kennarddh.mindustry.toast.core

import arc.util.Log
import com.github.kennarddh.mindustry.toast.core.commons.database.DatabaseHandler
import com.github.kennarddh.mindustry.toast.core.commons.database.DatabaseSettings
import com.github.kennarddh.mindustry.toast.core.handlers.UserAccountHandler
import com.github.kennarddh.mindustry.toast.core.handlers.UserStatsHandler
import kennarddh.genesis.core.Genesis
import kennarddh.genesis.core.commons.AbstractPlugin

@Suppress("unused")
class Toast : AbstractPlugin() {
    override fun init() {
        DatabaseSettings.init()
        Genesis.addHandler(DatabaseHandler())

        Genesis.addHandler(UserAccountHandler())
        Genesis.addHandler(UserStatsHandler())

        Log.info("[ToastCore] Loaded")
    }
}