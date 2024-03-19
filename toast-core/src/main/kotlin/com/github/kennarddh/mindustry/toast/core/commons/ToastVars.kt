package com.github.kennarddh.mindustry.toast.core.commons

import com.github.kennarddh.mindustry.toast.common.Server
import mindustry.game.Rules
import mindustry.net.Administration.Config

enum class ToastState {
    Idle,
    Hosting,
    ShuttingDown,
    Disposed
}

object ToastVars {
    val server: Server
        get() {
            try {
                return Server.valueOf(System.getenv("SERVER"))
            } catch (error: IllegalArgumentException) {
                throw IllegalArgumentException("${System.getenv("SERVER")} is not a valid server")
            }
        }

    val port: Int
        get() = System.getenv("PORT")?.toInt() ?: 6567

    val applyRules: Rules.() -> Unit = {}

    val applyConfigs: () -> Unit = {
        Config.messageRateLimit.set(1)
    }

    var state: ToastState = ToastState.Idle
}