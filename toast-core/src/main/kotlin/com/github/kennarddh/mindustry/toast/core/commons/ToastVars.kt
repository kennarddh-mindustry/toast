package com.github.kennarddh.mindustry.toast.core.commons

import com.github.kennarddh.mindustry.toast.common.Server

object ToastVars {
    val server: Server
        get() = when (System.getenv("SERVER")) {
            "Survival" -> Server.Survival
            else -> throw Exception("${System.getenv("SERVER")} is not a valid server")
        }
}