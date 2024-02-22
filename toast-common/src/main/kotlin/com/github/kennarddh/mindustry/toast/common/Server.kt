package com.github.kennarddh.mindustry.toast.common

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import mindustry.game.Rules

@Serializable
enum class Server(
    val displayName: String,
    val description: String,
    val gameMode: GameMode,
    val discordChannelID: Long,
    val applyRules: Rules.() -> Unit
) {
    @SerialName("Survival")
    Survival("Survival", "Toast Survival", GameMode.Survival, 1199598512162213958L, { }),

    @SerialName("Attack")
    Attack("Attack", "Toast Attack", GameMode.Attack, 1209544700189610025L, { }),

    @SerialName("PvP")
    PvP("PvP", "Toast Pvp", GameMode.PvP, 1209544729499275295L, { }),
}