package com.github.kennarddh.mindustry.toast.common

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class PunishmentType {
    @SerialName("Kick")
    Kick,

    @SerialName("VoteKick")
    VoteKick,

    @SerialName("Ban")
    Ban,

    @SerialName("Mute")
    Mute
}