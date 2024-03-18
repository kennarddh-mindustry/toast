package com.github.kennarddh.mindustry.toast.core.commons.entities

import mindustry.gen.Player
import java.util.concurrent.ConcurrentHashMap


object Entities {
    val players = ConcurrentHashMap<Player, PlayerData>()
}