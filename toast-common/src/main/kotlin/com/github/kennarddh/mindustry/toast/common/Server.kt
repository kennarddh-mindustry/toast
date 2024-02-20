package com.github.kennarddh.mindustry.toast.common

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class Server(val displayName: String, val gameMode: GameMode, val discordChannelID: Long) {
    @SerialName("Survival")
    Survival("Survival", GameMode.Survival, 1199598512162213958L),

    @SerialName("Attack")
    Attack("Attack", GameMode.Attack, 1209544700189610025L),

    @SerialName("PvP")
    PvP("PvP", GameMode.PvP, 1209544729499275295L),
}