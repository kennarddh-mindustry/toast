package kennarddh.toast.core

import arc.util.Log
import kennarddh.genesis.core.Genesis
import kennarddh.genesis.core.commons.AbstractPlugin
import kennarddh.toast.core.handlers.UserXPHandler

@SuppressWarnings("unused")
class Toast : AbstractPlugin() {
    override fun init() {
        Genesis.addHandler(UserXPHandler())

        Log.info("[ToastCore] Loaded")
    }
}