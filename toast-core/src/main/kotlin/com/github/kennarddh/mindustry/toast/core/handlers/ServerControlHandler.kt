package com.github.kennarddh.mindustry.toast.core.handlers

import com.github.kennarddh.mindustry.genesis.core.GenesisAPI
import com.github.kennarddh.mindustry.genesis.core.handlers.Handler
import com.github.kennarddh.mindustry.toast.common.messaging.Messenger
import com.github.kennarddh.mindustry.toast.common.messaging.messages.ServerCommandServerControl
import com.github.kennarddh.mindustry.toast.core.commons.Logger
import com.github.kennarddh.mindustry.toast.core.commons.ToastVars

class ServerControlHandler : Handler() {
    override suspend fun onInit() {
        Messenger.listenServerControl("${ToastVars.server.name}ServerServerControl", "${ToastVars.server.name}.#") {
            Logger.info("Received server control $it")
            val data = it.data

            if (data is ServerCommandServerControl) {
                Logger.info("Received server command server control ${data.command}")

                try {
                    GenesisAPI.commandRegistry.invokeServerCommand(data.command)
                } catch (_: IllegalArgumentException) {
                    Logger.warn("Command from ServerCommandServerControl should not be empty")
                }
            }
        }
    }
}