package com.github.kennarddh.mindustry.toast.core.handlers

import com.github.kennarddh.mindustry.genesis.core.handlers.Handler
import com.github.kennarddh.mindustry.toast.core.commons.ToastVars
import mindustry.net.Administration.Config

class WelcomeHandler : Handler {
    override suspend fun onInit() {
        Config.motd.set("Welcome to [red] | Toast |[orange]| Survival |[yellow]Owner:Bread|please join our discord by using /discord we are looking for new members and staff ${ToastVars.server.displayName} server.")
    }
}
