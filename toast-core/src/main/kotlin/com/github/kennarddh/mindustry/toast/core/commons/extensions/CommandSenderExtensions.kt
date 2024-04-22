package com.github.kennarddh.mindustry.toast.core.commons.extensions

import com.github.kennarddh.mindustry.genesis.core.commands.senders.CommandSender
import com.github.kennarddh.mindustry.genesis.core.commands.senders.PlayerCommandSender
import com.github.kennarddh.mindustry.genesis.core.commands.senders.ServerCommandSender
import com.github.kennarddh.mindustry.genesis.standard.extensions.stripColors
import com.github.kennarddh.mindustry.toast.common.Permission
import com.github.kennarddh.mindustry.toast.core.commons.Logger
import com.github.kennarddh.mindustry.toast.core.commons.safeGetPlayerData

fun CommandSender.getPermissions(): Set<Permission>? = when (this) {
    is ServerCommandSender -> Permission.all
    is PlayerCommandSender -> player.safeGetPlayerData()?.fullPermissions
    else -> {
        Logger.error("Unknown CommandSender")

        sendError("Server error occurred, please report.")

        null
    }
}

fun CommandSender.getName() = when (this) {
    is ServerCommandSender -> "Server"
    is PlayerCommandSender -> player.name
    else -> {
        Logger.error("Unknown CommandSender")

        sendError("Server error occurred, please report.")

        null
    }
}

fun CommandSender.getStrippedName() = getName()?.stripColors()