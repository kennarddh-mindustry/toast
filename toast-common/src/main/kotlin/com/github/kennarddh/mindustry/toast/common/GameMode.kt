package com.github.kennarddh.mindustry.toast.common

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import mindustry.game.Rules

@Serializable
enum class GameMode(val applyRules: Rules.() -> Unit) {
    @SerialName("Survival")
    Survival({
        buildSpeedMultiplier = 2f
    }),

    @SerialName("Attack")
    Attack({
        buildCostMultiplier = 0.5f
    }),

    @SerialName("PvP")
    PvP({
        buildSpeedMultiplier = 2f
    }),
}