package com.github.kennarddh.mindustry.toast.core.commons.entities

import mindustry.gen.Player
import java.util.*


object Entities {
    val players: MutableMap<Player, PlayerData> = Collections.synchronizedMap(mutableMapOf())
}