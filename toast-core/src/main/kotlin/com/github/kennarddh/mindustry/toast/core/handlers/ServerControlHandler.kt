package com.github.kennarddh.mindustry.toast.core.handlers

import com.github.kennarddh.mindustry.genesis.core.GenesisAPI
import com.github.kennarddh.mindustry.genesis.core.handlers.Handler
import com.github.kennarddh.mindustry.toast.common.messaging.Messenger
import com.github.kennarddh.mindustry.toast.common.messaging.messages.ServerCommandServerControl
import com.github.kennarddh.mindustry.toast.core.commons.ToastVars

class ServerControlHandler : Handler() {
    override suspend fun onInit() {
        Messenger.listenServerControl("${ToastVars.server.name}Server", "${ToastVars.server.name}.#") {
            val data = it.data

            if (data is ServerCommandServerControl) {
                GenesisAPI.commandRegistry.invokeServerCommand(data.command)
            }
        }
    }
}