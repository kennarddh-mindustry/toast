package com.github.kennarddh.mindustry.toast.common

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class UserRole {
    @SerialName("Owner")
    Owner,

    @SerialName("CoOwner")
    CoOwner,

    @SerialName("Admin")
    Admin,

    @SerialName("Mod")
    Mod,

    @SerialName("Player")
    Player,
}