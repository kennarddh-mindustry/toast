package com.github.kennarddh.mindustry.toast.core.commons.entities

import com.github.kennarddh.mindustry.toast.common.UserRole
import mindustry.gen.Player
import kotlin.time.Duration

data class PlayerData(
    var userID: Int?,
    val mindustryUserID: Int,
    val player: Player,
    val originalName: String,
    var role: UserRole?,
    var xp: Int = 0,
    var playTime: Duration = Duration.ZERO,
)
