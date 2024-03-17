package com.github.kennarddh.mindustry.toast.core.commons.entities

import mindustry.gen.Player
import java.util.*


object Entities {
    val players = Collections.synchronizedMap(mutableMapOf<Player, PlayerData>())
}