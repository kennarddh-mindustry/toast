package com.github.kennarddh.mindustry.toast.common

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class Server(val displayName: String, val gameMode: GameMode, val discordChannelID: Long) {
    @SerialName("Survival")
    Survival("Survival", GameMode.Survival, 1199598512162213958L)
}