package com.github.kennarddh.mindustry.toast.core.handlers

import com.github.kennarddh.mindustry.genesis.core.GenesisAPI
import com.github.kennarddh.mindustry.genesis.core.commons.CoroutineScopes
import com.github.kennarddh.mindustry.genesis.core.handlers.Handler
import com.github.kennarddh.mindustry.toast.common.messaging.Messenger
import com.github.kennarddh.mindustry.toast.common.messaging.messages.ChatServerControl
import com.github.kennarddh.mindustry.toast.common.messaging.messages.GameEvent
import com.github.kennarddh.mindustry.toast.common.messaging.messages.PlayerChatGameEvent
import com.github.kennarddh.mindustry.toast.common.messaging.messages.ServerCommandServerControl
import com.github.kennarddh.mindustry.toast.core.commons.Logger
import com.github.kennarddh.mindustry.toast.core.commons.ToastVars
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import mindustry.gen.Call

class ServerControlHandler : Handler() {
    override suspend fun onInit() {
        Messenger.listenServerControl("${ToastVars.server.name}ServerServerControl", "${ToastVars.server.name}.#") {
            val data = it.data

            if (data is ServerCommandServerControl) {
                try {
                    GenesisAPI.commandRegistry.invokeServerCommand(data.command)
                } catch (error: Exception) {
                    Logger.error("Unknown invokeServerCommand error occurred", error)
                }
            } else if (data is ChatServerControl) {
                Call.sendMessage("[gold]<Discord> [accent][${data.sender}][white]: ${data.message}")

                CoroutineScopes.Main.launch {
                    Messenger.publishGameEvent(
                        "${ToastVars.server.name}.discord.chat",
                        GameEvent(
                            ToastVars.server, Clock.System.now(),
                            PlayerChatGameEvent(
                                "<Discord> ${data.sender}",
                                null,
                                data.message
                            )
                        )
                    )
                }
            }
        }
    }
}