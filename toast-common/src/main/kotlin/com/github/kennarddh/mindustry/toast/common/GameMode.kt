package com.github.kennarddh.mindustry.toast.common

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import mindustry.game.Gamemode
import mindustry.game.Rules
import mindustry.net.Administration.Config

@Serializable
enum class GameMode(
    val displayName: String,
    val mindustryGameMode: Gamemode,
    val applyRules: Rules.() -> Unit,
    val applyConfigs: () -> Unit
) {
    @SerialName("Survival")
    Survival("Survival", Gamemode.survival, {
        buildSpeedMultiplier = 2f
    }, {}),

    @SerialName("Attack")
    Attack("Attack", Gamemode.attack, {
        buildCostMultiplier = 0.5f
    }, {}),

    @SerialName("PvP")
    PvP("PvP", Gamemode.pvp, {
        buildSpeedMultiplier = 2f
    }, {
        Config.autoPause.set(true)
    });

    override fun toString(): String = this.displayName
}