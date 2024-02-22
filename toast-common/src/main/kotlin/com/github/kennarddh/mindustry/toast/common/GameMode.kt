package com.github.kennarddh.mindustry.toast.common

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import mindustry.game.Gamemode
import mindustry.game.Rules

@Serializable
enum class GameMode(val mindustryGameMode: Gamemode, val applyRules: Rules.() -> Unit) {
    @SerialName("Survival")
    Survival(Gamemode.survival, {
        buildSpeedMultiplier = 2f
    }),

    @SerialName("Attack")
    Attack(Gamemode.attack, {
        buildCostMultiplier = 0.5f
    }),

    @SerialName("PvP")
    PvP(Gamemode.pvp, {
        buildSpeedMultiplier = 2f
    }),
}