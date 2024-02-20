package com.github.kennarddh.mindustry.toast.common

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class GameMode {
    @SerialName("Survival")
    Survival,

    @SerialName("Attack")
    Attack,

    @SerialName("PvP")
    PvP,
}