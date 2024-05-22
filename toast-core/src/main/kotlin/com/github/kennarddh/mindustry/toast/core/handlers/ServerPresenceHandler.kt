package com.github.kennarddh.mindustry.toast.core.handlers

import com.github.kennarddh.mindustry.genesis.core.commons.runOnMindustryThread
import com.github.kennarddh.mindustry.genesis.core.handlers.Handler
import com.github.kennarddh.mindustry.toast.core.commons.Logger
import com.github.kennarddh.mindustry.toast.core.commons.ToastVars
import mindustry.net.Administration.Config

class ServerPresenceHandler : Handler {
    override suspend fun onInit() {
        runOnMindustryThread {
            Config.serverName.set("[red]Toast |[blue] ${ToastVars.server.displayName}[red]US")
            Config.desc.set(ToastVars.server.description)

            Logger.info("Server presence done")
        }
    }
}
