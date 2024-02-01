package com.github.kennarddh.mindustry.toast.common

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class KickType {
    @SerialName("Kick")
    Kick,

    @SerialName("VoteKick")
    VoteKick,
}