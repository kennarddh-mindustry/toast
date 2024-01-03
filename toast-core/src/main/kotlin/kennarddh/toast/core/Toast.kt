package kennarddh.toast.core

import arc.util.Log
import kennarddh.genesis.core.Genesis
import kennarddh.genesis.core.commons.AbstractPlugin
import kennarddh.toast.core.handlers.UserXPHandler
import mindustry.gen.Call

@SuppressWarnings("unused")
class Toast : AbstractPlugin() {
    override fun init() {
        Log.info("[Toast] Loaded")

        Genesis.addHandler(UserXPHandler())
    }
}