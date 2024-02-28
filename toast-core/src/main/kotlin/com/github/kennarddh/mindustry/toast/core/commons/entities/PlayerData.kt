package com.github.kennarddh.mindustry.toast.core.commons.entities

import com.github.kennarddh.mindustry.toast.common.UserRole
import mindustry.gen.Player

data class PlayerData(
    var userID: Int?,
    val mindustryUserID: Int,
    val player: Player,
    val originalName: String,
    var role: UserRole?
)
