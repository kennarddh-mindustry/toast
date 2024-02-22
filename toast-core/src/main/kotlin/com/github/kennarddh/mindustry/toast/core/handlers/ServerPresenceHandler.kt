package com.github.kennarddh.mindustry.toast.core.handlers

import com.github.kennarddh.mindustry.genesis.core.handlers.Handler
import com.github.kennarddh.mindustry.toast.core.commons.ToastVars
import mindustry.net.Administration.Config

class ServerPresenceHandler : Handler() {
    override suspend fun onInit() {
        Config.serverName.set("Toast | ${ToastVars.server.displayName}")
        Config.desc.set(ToastVars.server.description)
    }
}