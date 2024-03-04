package com.github.kennarddh.mindustry.toast.common

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import mindustry.gen.Player

fun Player.clearRoleEffect() {
    admin = false
}

@Serializable
enum class UserRole(
    val displayName: String,
    val applyRoleEffect: mindustry.gen.Player.() -> Unit,
    val permissions: Set<Permission> = setOf()
) {
    @SerialName("Player")
    Player("Player", { }, setOf(Permission.Join, Permission.Chat)),

    @SerialName("Mod")
    Mod("Mod", { }, setOf(Permission.ViewUUID)),

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
            val allAffectingRoles = entries.filter { it <= this }

            val computedPermissions: Set<Permission> = setOf()

            allAffectingRoles.forEach {
                computedPermissions + it.permissions
            }

            return computedPermissions
        }
}