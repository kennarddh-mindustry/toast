package com.github.kennarddh.mindustry.toast.common

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class UserRole {
    @SerialName("Player")
    Player,

    @SerialName("Mod")
    Mod,

    @SerialName("Admin")
    Admin,

    @SerialName("CoOwner")
    CoOwner,

    @SerialName("Owner")
    Owner,
}