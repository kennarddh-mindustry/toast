package com.github.kennarddh.mindustry.toast.core.commons

import com.github.kennarddh.mindustry.toast.common.Server

object ToastVars {
    val server: Server
        get() {
            try {
                return Server.valueOf(System.getenv("SERVER"))
            } catch (error: IllegalArgumentException) {
                throw IllegalArgumentException("${System.getenv("SERVER")} is not a valid server")
            }
        }
}