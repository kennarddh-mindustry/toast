package com.github.kennarddh.mindustry.toast.common

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import mindustry.gen.Player

fun Player.clearRoleEffect() {
    admin = false
}

@Serializable
enum class UserRole(
    val displayName: String,
    val applyRoleEffect: mindustry.gen.Player.() -> Unit,
) {
    @SerialName("Player")
    Player("Player", { }),

    @SerialName("Mod")
    Mod("Mod", { }),

    @SerialName("Admin")
    Admin("Admin", {
        admin = true
    }),

    @SerialName("CoOwner")
    CoOwner("CoOwner", {
        admin = true
    }),

    @SerialName("Owner")
    Owner("Owner", {
        admin = true
    });

    override fun toString(): String = this.displayName
}