package com.github.kennarddh.mindustry.toast.common

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class PunishmentType(val displayName: String) {
    @SerialName("Kick")
    Kick("Kick"),

    @SerialName("VoteKick")
    VoteKick("VoteKIck"),

    @SerialName("Ban")
    Ban("Ban"),

    @SerialName("Mute")
    Mute("Mute");

    override fun toString(): String = this.displayName
}