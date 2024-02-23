package com.github.kennarddh.mindustry.toast.common

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import mindustry.gen.Player

fun Player.clearRoleEffect() {
    admin = false
}

@Serializable
enum class UserRole(
    val applyRoleEffect: mindustry.gen.Player.() -> Unit,
) {
    @SerialName("Player")
    Player({ }),

    @SerialName("Mod")
    Mod({ }),

    @SerialName("Admin")
    Admin({
        admin = true
    }),

    @SerialName("CoOwner")
    CoOwner({
        admin = true
    }),

    @SerialName("Owner")
    Owner({
        admin = true
    }),
}