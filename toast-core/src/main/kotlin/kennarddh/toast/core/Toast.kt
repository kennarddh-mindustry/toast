package kennarddh.toast.core

import arc.util.Log
import kennarddh.genesis.core.commons.AbstractPlugin

@SuppressWarnings("unused")
class Toast : AbstractPlugin() {
    override fun init() {
        Log.info("[Toast] Loaded")
    }
}