package com.github.kennarddh.mindustry.toast.core.handlers

import com.github.kennarddh.mindustry.genesis.core.handlers.Handler
import com.github.kennarddh.mindustry.toast.core.commons.Logger
import com.github.kennarddh.mindustry.toast.core.commons.ToastVars
import mindustry.net.Administration.Config

class ServerPresenceHandler : Handler {
    override suspend fun onInit() {
        Config.serverName.set("[red]Toast |[white] ${ToastVars.server.displayName}")
        Config.desc.set(ToastVars.server.description)

        Logger.info("Server presence done")
    }
}
