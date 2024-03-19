package com.github.kennarddh.mindustry.toast.common

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import mindustry.gen.Player

fun Player.clearRoleEffect() {
    admin = false
}

val publicPermission = setOf(Permission.Join, Permission.Chat)

@Serializable
enum class UserRole(
    val displayName: String,
    val applyRoleEffect: mindustry.gen.Player.() -> Unit,
    private val permissions: Set<Permission> = setOf()
) {
    @SerialName("Player")
    Player("Player", { }, setOf()),

    @SerialName("Mod")
    Mod("Mod", { }, setOf(Permission.ViewUUID, Permission.ViewMindustryNamesHistory)),

    @SerialName("Admin")
    Admin("Admin", {
        admin = true
    }, setOf(Permission.ViewIP)),

    @SerialName("CoOwner")
    CoOwner("CoOwner", {
        admin = true
    }, setOf()),

    @SerialName("Owner")
    Owner("Owner", {
        admin = true
    }, setOf());

    override fun toString(): String = this.displayName

    val fullPermissions: Set<Permission>
        get() {
            val affectingRoles = entries.filter { it <= this }

            return publicPermission + affectingRoles.flatMap { it.permissions }.toSet()
        }
}