package kennarddh.toast.core

import arc.util.Log
import kennarddh.genesis.core.Genesis
import kennarddh.genesis.core.commons.AbstractPlugin
import kennarddh.toast.core.database.DatabaseSettings
import kennarddh.toast.core.handlers.UserXPHandler

@Suppress("unused")
class Toast : AbstractPlugin() {
    override fun init() {
        DatabaseSettings.init()

        Genesis.addHandler(UserXPHandler())

        Log.info("[ToastCore] Loaded")
    }
}