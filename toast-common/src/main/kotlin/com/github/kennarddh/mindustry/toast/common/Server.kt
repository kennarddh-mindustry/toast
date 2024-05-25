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
    val applyRules: Rules.() -> Unit,
    val applyConfigs: () -> Unit
) {
    @SerialName("Survival")
    Survival("Survival", "[yellow]Toast Survival", GameMode.Survival, 1199598512162213958L, { }, { }),

    @SerialName("Attack")
    Attack("Attack", "[red]Toast Attack", GameMode.Attack, 1209544700189610025L, { attackMode = true }, { }),

    @SerialName("PvP")
    PvP("PvP", "[blue]Toast Pvp", GameMode.PvP, 1209544729499275295L, { pvp = true }, { });

    override fun toString(): String = this.displayName
}
